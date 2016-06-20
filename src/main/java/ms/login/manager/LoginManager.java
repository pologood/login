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
  private String tokenKey;

  @Autowired
  public LoginManager(Environment env) {
    smsRegisterTemplate = env.getRequiredProperty("sms.register.template");
    tokenKey = env.getProperty("security.token");
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
        c.expire(key, 60);
      } else if (cnt >= 3) {
        return Errno.IDENTIFY_CODE_REQUIRED;
      }
    }
    return Errno.OK;
  }
      
  public byte[] getIdentifyingCode(String id) {
    return patchcaService.getPatchca(id);
  }

  public ApiResult getRegisterCode(String account) {
    int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
    if (isEmail(account)) {
      throw new RuntimeException("not implemented");
    } else {
      smsService.send(account, String.valueOf(rand), String.format(smsRegisterTemplate, rand));
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

    accountMapper.updatePasswordByPhone(accountName, encodePassword(password));
    return ApiResult.ok();
  }

  public ApiResult resetPassword(User user, String password, String oldPassword,
                                 HttpServletResponse response) {
    Account account = accountMapper.find(user.getUid());
    if (account == null) return ApiResult.internalError("uid not found");

    if (checkPassword(password, account.getPassword())) {
      accountMapper.updatePasswordById(user.getUid(), encodePassword(password));
      String token = rememberMeService.login(response, user);
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

  ApiResult getOpenAccount(String openId) {
    LoginService service = loginServiceProvider.get(openId);
    if (service == null) return ApiResult.unAuthorized();
    LoginService.User user = service.info(openId);
    if (user == null) return ApiResult.unAuthorized();
    else return new ApiResult<LoginService.User>(user);
  }

  public ApiResult getAccount(User user) {
    if (user.isOpen()) {
      return getOpenAccount(user.getId());
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

  public ApiResult getAccountPermCheck(Account account, User user, Optional<String> token) {
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

  public List<Long> getPermIds(Account account) {
    List<Long> permIds = null;

    if (account.getPerm() == Account.PERM_EXIST) {
      permIds = accountPermMapper.get(account.getId());
    } else if (account.getPerm() != Long.MAX_VALUE) {
      permIds = Arrays.asList(account.getPerm());
    }

    return permIds;
  }

  public ApiResult login(String accountName, String password, Optional<String> id,
                         Optional<String> idcode, String openId,
                         HttpServletResponse response) {
    int errno = checkPatchca(accountName, id.orElse(accountName), idcode);
    if (errno != Errno.OK) return new ApiResult(errno);
    
    Account account;
    if (isEmail(accountName)) {
      account = null;
    } else {
      account = accountMapper.findByPhone(accountName);
    }
    
    if (account == null || !checkPassword(password, account.getPassword())) {
      errno = logLoginError(accountName);
      if (errno != Errno.OK && !idcode.isPresent()) return new ApiResult(errno);
      return new ApiResult(account == null ? Errno.USER_NOT_FOUND : Errno.USER_PASSWORD_ERROR);
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
      
      token = rememberMeService.login(
        response, new User(openId, user.getName(), account.getIncId(), getPermIds(account)));
    } else {
      token = rememberMeService.login(
        response, new User(account.getId(), account.getName(),
                           account.getIncId(), getPermIds(account)));
    }
    
    return new ApiResult<String>(token);
  }

  public ApiResult login(String tmpToken, LoginServiceProvider.Name provider,
                         HttpServletResponse response) {
    LoginService service = loginServiceProvider.get(provider);
    LoginService.User user = service.login(tmpToken);
    if (user == null) return ApiResult.unAuthorized();

    String token = null;

    OpenAccount openAccount = openAccountMapper.findByOpenId(user.getOpenId());
    if (openAccount != null) {
      Account account = accountMapper.find(openAccount.getUid());
      if (account != null) {
        token = rememberMeService.login(
          response, new User(user.getOpenId(), user.getName(),
                             account.getIncId(), getPermIds(account)));
      }
    }

    if (token == null) {
      token = rememberMeService.login(response, new User(user.getOpenId(), user.getName()));
    }

    return new ApiResult<String>(token);
  }

  public ApiResult logout(String id, HttpServletResponse response) {
    rememberMeService.logout(id, response);
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
}
