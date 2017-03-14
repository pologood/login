package ms.login.manager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletResponse;
import redis.clients.jedis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import commons.utils.*;
import commons.saas.*;
import commons.spring.RedisRememberMeService;
import static commons.spring.RedisRememberMeService.User;
import static commons.spring.RedisRememberMeService.UserPerm;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.mapper.*;

@Component
public class LoginManager {
  @Autowired PatchcaService         patchcaService;
  @Autowired SmsService             smsService;
  @Autowired AccountMapper          accountMapper;
  @Autowired AccountPermMapper      accountPermMapper;
  @Autowired OpenAccountMapper      openAccountMapper;
  @Autowired JedisPool              jedisPool;
  @Autowired RedisRememberMeService rememberMeService;
  @Autowired LoginServiceProvider   loginServiceProvider;
  @Autowired RestNameService        restNameService;
  @Autowired RestTemplate           restTemplate;

  private static final String LOGIN_ERROR_PREFIX = "LoginManagerLoginError_";
  private String smsRegisterTemplate;
  private String smsLoginTemplate;
  private String tokenKey;
  private boolean xiaopUseUno;
  private boolean permissionOn;

  public enum Action {
    REGISTER, LOGIN;
  }

  @Autowired
  public LoginManager(Environment env) {
    smsRegisterTemplate = env.getRequiredProperty("sms.register.template");
    smsLoginTemplate = env.getProperty("sms.login.template");

    tokenKey = env.getProperty("security.token");
    xiaopUseUno = env.getProperty("login.xiaop.uno", Boolean.class, false);
    permissionOn = env.getProperty("permissionOn", Boolean.class, false);
  }

  private boolean isEmail(String account) {
    return account.indexOf('@') != -1;
  }

  private String encodePassword(String password) {
    String random = String.valueOf(new Random().nextInt(100000));
    return random + ":" + DigestHelper.sha1((password + random).getBytes());
  }

  private boolean checkPassword(String password, String encodedPassword) {
    String[] parts = encodedPassword.split(":", 2);
    if (parts.length != 2) return false;
    return DigestHelper.sha1((password + parts[0]).getBytes()).equals(parts[1]);
  }

  private int checkPatchca(String accountName, String id, Optional<String> idcode) {
    String key = LOGIN_ERROR_PREFIX + accountName;
    String errCnt = JedisHelper.get(jedisPool, key);

    if (errCnt != null && Integer.valueOf(errCnt) >= 3) {
      if (idcode.isPresent()) {
        if (!patchcaService.checkText(id, idcode.get())) {
          return Errno.IDENTIFY_CODE_ERROR;
        }
        JedisHelper.del(jedisPool, key);
      } else {
        return Errno.IDENTIFY_CODE_REQUIRED;
      }
    }
    return Errno.OK;
  }

  private int logLoginError(String id) {
    String key = LOGIN_ERROR_PREFIX + id;
    try (Jedis c = jedisPool.getResource()) {
      long cnt = c.incr(key);
      if (cnt == 1) {
        c.expire(key, 300);
      } else if (cnt >= 3) {
        return Errno.IDENTIFY_CODE_REQUIRED;
      }
    }
    return Errno.OK;
  }

  public byte[] getIdentifyingCode(String id) {
    return patchcaService.getPatchca(id);
  }

  public ApiResult getRegisterCode(String account, Action action) {
    int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
    if (isEmail(account)) {
      throw new RuntimeException("not implemented");
    } else {
      String template = null;
      if (action == Action.REGISTER) template = smsRegisterTemplate;
      else if (action == Action.LOGIN) template = smsLoginTemplate;

      if (template == null) return ApiResult.badRequest("not implemented");

      smsService.send(account, String.valueOf(rand), String.format(template, rand));
    }
    return ApiResult.ok();
  }

  public ApiResult addAccount(String accountName, String password, String regcode,
                              Optional<String> name, HttpServletResponse response) {
    Account account = new Account();
    String code;
    if (isEmail(accountName)) {
      code = null;
      account.setEmail(accountName);
    } else {
      code = smsService.get(accountName, regcode);
      account.setPhone(accountName);
    }

    if (!regcode.equals(code)) {
      return new ApiResult(Errno.SMS_CODE_ERROR);
    }

    if (name.isPresent()) account.setName(name.get());
    account.setStatus(Account.Status.OK);

    try {
      account.setPassword(encodePassword(password));
      accountMapper.add(account);
    } catch (DuplicateKeyException e) {
      return new ApiResult(Errno.USER_EXISTS);
    }

    String token = rememberMeService.login(
      response, new User(account.getId(), account.getName()));
    return new ApiResult<String>(token);
  }

  public ApiResult resetPassword(String accountName, String password, String regcode,
                                 Optional<String> id, Optional<String> idcode) {
    int errno = checkPatchca(accountName, id.orElse(accountName), idcode);
    if (errno != Errno.OK) return new ApiResult(errno);

    String code;
    boolean isEmail = this.isEmail(accountName);
    if (isEmail) {
      code = null;
    } else {
      code = smsService.get(accountName, regcode);
    }

    if (!regcode.equals(code)) {
      errno = logLoginError(accountName);
      if (errno != Errno.OK && !idcode.isPresent()) return new ApiResult(errno);
      else return new ApiResult(Errno.SMS_CODE_ERROR);
    }

    int r = accountMapper.updatePasswordByPhone(accountName, encodePassword(password));
    if (r > 0) {
      Account account = accountMapper.findByPhone(accountName);
      if (account != null) rememberMeService.logout(String.valueOf(account.getId()), null, true);
    }

    return ApiResult.ok();
  }

  public ApiResult resetPassword(User user, String password, String oldPassword,
                                 HttpServletResponse response) {
    Account account = accountMapper.find(user.getUid());
    if (account == null) return ApiResult.internalError("uid not found");

    if (checkPassword(oldPassword, account.getPassword())) {
      accountMapper.updatePasswordById(user.getUid(), encodePassword(password));
      String token = rememberMeService.login(response, user, true);
      return new ApiResult<String>(token);
    } else {
      return new ApiResult(Errno.USER_PASSWORD_ERROR);
    }
  }

  public ApiResult updateAccount(RedisRememberMeService.User user, Account account) {
    account.setStatus(null);
    account.setPerm(Long.MAX_VALUE);
    accountMapper.update(account);

    if (account.getName() != null) {
      user.setName(account.getName());
      rememberMeService.update(user);
    }

    return ApiResult.ok();
  }

  public ApiResult deleteAccount(long uid, HttpServletResponse response) {
    Account account = accountMapper.find(uid);
    if (account == null) return ApiResult.ok();

    String phone = account.getPhone();
    String email = account.getEmail();

    for (int i = 0; i < 100; ++i) {
      String rand = StringHelper.random(3);
      if (phone != null) {
        account.setPhone(phone + "#" + rand);
      }
      if (email != null) {
        account.setEmail(email + "#" + rand);
      }
      try {
        accountMapper.delete(account);
        rememberMeService.logout(String.valueOf(uid), response, true);
        return ApiResult.ok();
      } catch (DuplicateKeyException e) {
      }
    }
    return ApiResult.badRequest("delete too much");
  }

  Account getLocalAccount(String accountName) {
    Account account = isEmail(accountName) ? accountMapper.findByEmail(accountName) :
      accountMapper.findByPhone(accountName);
    if (account != null) account.setGrantPerms(getPermIds(account));
    return account;
  }

  Account getLocalAccount(long uid) {
    Account account = accountMapper.find(uid);
    if (account != null) account.setGrantPerms(getPermIds(account));
    return account;
  }

  LoginService.User getOpenAccountImpl(String openId) {
    LoginService service = loginServiceProvider.get(openId);
    if (service != null) {
      return service.info(openId);
    }
    return null;
  }

  ApiResult getOpenAccount(String openId, boolean pcf) {
    LoginService service = loginServiceProvider.get(openId);
    if (service == null) return ApiResult.unAuthorized();
    LoginService.User user = service.info(openId);
    if (user == null) return ApiResult.unAuthorized();

    if (pcf) {
      OpenAccount openAccount = openAccountMapper.findByOpenId(user.getOpenId());
      if (openAccount != null && openAccount.getStatus() == OpenAccount.Status.AGREE) {
        user.setUid(openAccount.getUid());
      }
    }

    return new ApiResult<LoginService.User>(user);
  }

  public ApiResult getAccount(User user, boolean pcf) {
    if (user.isOpen() || user.getOpenId() != null) {
      return getOpenAccount(user.getOpenId(), pcf);
    } else {
      Account account = getLocalAccount(user.getUid());
      if (account == null) return ApiResult.notFound();
      else return new ApiResult<Account>(account);
    }
  }

  public ApiResult getAccount(User user, String accountName, Optional<String> token) {
    Account account = getLocalAccount(accountName);
    return getAccountPermCheck(account, user, token);
  }

  public ApiResult getAccount(User user, long uid, Optional<String> token) {
    Account account = getLocalAccount(uid);
    return getAccountPermCheck(account, user, token);
  }

  public ApiResult getAccount(long uid, LoginServiceProvider.Name provider) {
    List<OpenAccount> accounts = openAccountMapper.findByUid(uid);

    OpenAccount account = null;
    for (OpenAccount a : accounts) {
      if (LoginServiceProvider.getProvider(a.getOpenId()) == provider) {
        account = a;
        break;
      }
    }

    if (account != null) {
      LoginService loginService = loginServiceProvider.get(account.getOpenId());
      String token = loginService.getAccessToken();
      if (token == null) return new ApiResult(Errno.OPEN_ACCESS_TOKEN_ERROR);
      return new ApiResult<Map>(MapHelper.make("openId", account.getRawOpenId(), "token", token));
    } else {
      return ApiResult.notFound();
    }
  }

  ApiResult getAccountPermCheck(Account account, User user, Optional<String> token) {
    if (account == null) return ApiResult.notFound();

    if (token.isPresent()) {
      boolean ok = false;
      if (user.getIncId() > 0) {
        String text = user.getIncIdString() + ":" + String.valueOf(account.getId());
        ok = token.get().equals(DigestHelper.hmacSHA1(tokenKey, text.getBytes()));
      }
      if (!ok) {
        String text = user.getId() + ":" + String.valueOf(account.getId());
        ok = token.get().equals(DigestHelper.hmacSHA1(tokenKey, text.getBytes()));
      }
      if (ok) return new ApiResult<Account>(account);
    } else {
      if (account.getIncId() != Integer.MIN_VALUE && account.getIncId() == user.getIncId()) {
        return new ApiResult<Account>(account);
      } else if (user.isInternal()) {
        return new ApiResult<Account>(account);
      }
    }
    return ApiResult.notFound();
  }

  public List<UserPerm> getPermIds(Account account) {
    List<UserPerm> perms = null;

    if (account.getPerm() == Account.PERM_EXIST) {
      perms = accountPermMapper.get(account.getId());
    } else if (account.getPerm() != Long.MAX_VALUE) {
      if (permissionOn) {
        perms = new ArrayList<>();
        perms.add(new UserPerm(account.getPerm()));
        perms.addAll(accountPermMapper.get(account.getId()));
      } else {
        perms = Arrays.asList(new UserPerm(account.getPerm()));
      }
    } else if (permissionOn) {
      perms = accountPermMapper.get(account.getId());
    }

    return perms;
  }

  public ApiResult login(String accountName, String password, String smsCode, Optional<String> id,
                         Optional<String> idcode, String openId, boolean pcf,
                         HttpServletResponse response) {
    int errno = checkPatchca(accountName, id.orElse(accountName), idcode);
    if (errno != Errno.OK) return new ApiResult(errno);

    Account account;
    if (isEmail(accountName)) {
      account = accountMapper.findByEmail(accountName);
    } else {
      account = accountMapper.findByPhone(accountName);
    }

    if (account == null ||
        (password != null && !checkPassword(password, account.getPassword())) ||
        (smsCode != null && !smsCode.equals(smsService.get(accountName, smsCode)))) {

      errno = logLoginError(accountName);
      if (errno != Errno.OK && !idcode.isPresent()) return new ApiResult(errno);

      if (account == null) errno = Errno.USER_NOT_FOUND;
      else if (smsCode != null) errno = Errno.SMS_CODE_ERROR;
      else errno = Errno.USER_PASSWORD_ERROR;

      return new ApiResult(errno);
    }

    String token;
    if (openId != null) {
      LoginService.User user = getOpenAccountImpl(openId);
      if (user == null) {
        return ApiResult.badRequest("invalid openId");
      }

      OpenAccount openAccount = new OpenAccount(user);
      openAccount.setUid(account.getId());
      openAccount.setStatus(OpenAccount.Status.AGREE);
      openAccountMapper.bind(openAccount);

      User u;
      if (pcf) {
        long uid = account.getId();
        u = new User(uid, openId, user.getName(), account.getIncId(), getPermIds(account));
      } else {
        u = new User(openId, user.getName(), account.getIncId(), getPermIds(account));
      }

      token = rememberMeService.login(response, u);
    } else {
      token = rememberMeService.login(
        response, new User(account.getId(), account.getName(),
                           account.getIncId(), getPermIds(account)));
    }

    return new ApiResult<String>(token);
  }

  public ApiResult login(String tmpToken, boolean pcf, LoginServiceProvider.Name provider,
                         HttpServletResponse response) {
    LoginService service = loginServiceProvider.get(provider);
    LoginService.User user = service.login(tmpToken);
    if (user == null) return ApiResult.unAuthorized();

    User u = null;

    OpenAccount openAccount = openAccountMapper.findByOpenId(user.getOpenId());
    if (openAccount != null && openAccount.getStatus() == OpenAccount.Status.AGREE) {
      Account account = accountMapper.find(openAccount.getUid());
      if (account != null) {
        if (pcf) {
          u = new User(account.getId(), user.getName(), account.getIncId(), getPermIds(account));
        } else {
          u = new User(user.getOpenId(), user.getName(), account.getIncId(), getPermIds(account));
        }
      }
    }

    if (xiaopUseUno && user.getId() > 0) {
      Account account = accountMapper.find(user.getId());
      if (account != null) {
        u = new User(user.getId(), user.getName(), account.getIncId(), getPermIds(account));
      } else {
        u = new User(user.getId(), user.getName());
        String email = LoginServiceProvider.openIdToEmail(user.getOpenId());
        accountMapper.addOpenUser(user.getId(), email, user.getName(), user.getHeadImg());
      }
    }

    if (u == null) {
      u = new User(user.getOpenId(), user.getName());
    }

    return new ApiResult<String>(rememberMeService.login(response, u));
  }

  public ApiResult logout(User user, HttpServletResponse response, boolean unbind) {
    rememberMeService.logout(user.getId(), response, false);
    if (unbind && !user.isOpen()) {
      String openId = user.getOpenId();
      if (openId != null) {
        openAccountMapper.delete(openId, Long.parseLong(user.getId()));
      } else {
        openAccountMapper.deleteAll(Long.parseLong(user.getId()));
      }
    }
    return ApiResult.ok();
  }

  public ApiResult listBindOpenAccount(long uid) {
    List<OpenAccount> accounts = openAccountMapper.findByUid(uid);
    return new ApiResult<List>(accounts);
  }

  public ApiResult applyBindOpenId(String openId, String code) {
    LoginService.User user = getOpenAccountImpl(openId);
    if (user == null) {
      return ApiResult.badRequest("invalid openId");
    }

    String id = JedisHelper.get(jedisPool, code);
    if (id == null) {
      return ApiResult.badRequest("invalid code");
    }

    long uid = Long.parseLong(id);
    OpenAccount openAccount = new OpenAccount(user);
    openAccount.setUid(uid);
    openAccount.setStatus(OpenAccount.Status.WAIT_AGREE);
    openAccountMapper.bind(openAccount);

    // ignore result
    restTemplate.postForObject(
      restNameService.lookup("NOTICE"),
      MapHelper.makeMulti("title", "apply", "channel", code, "uids", id),
      ApiResult.class);

    return ApiResult.ok();
  }

  public ApiResult acceptBindOpenId(long uid, String openId) {
    int r = openAccountMapper.accept(openId, uid, OpenAccount.Status.AGREE);
    if (r == 0) return ApiResult.badRequest("apply first");

    LoginService.User user = getOpenAccountImpl(openId);
    if (user == null) {
      return ApiResult.badRequest("invalid openId");
    }

    Account account = accountMapper.find(uid);
    rememberMeService.update(
      new User(openId, user.getName(), account.getIncId(), getPermIds(account)));

    return ApiResult.ok();
  }

  public ApiResult unbindOpenId(long uid, String openId) {
    int r = openAccountMapper.delete(openId, uid);
    if (r == 0) return ApiResult.ok();

    LoginService.User user = getOpenAccountImpl(openId);
    if (user == null) {
      return ApiResult.badRequest("invalid openId");
    }

    rememberMeService.update(new User(openId, user.getName()));
    return ApiResult.ok();
  }
}
