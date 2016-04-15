package ms.login.manager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletResponse;
import redis.clients.jedis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import commons.utils.*;
import commons.saas.*;
import commons.spring.*;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.mapper.*;

@Component
public class LoginManager {
  @Autowired PatchcaService patchcaService;
  @Autowired SmsService     smsService;
  @Autowired AccountMapper  accountMapper;
  @Autowired JedisPool      jedisPool;
  @Autowired RedisRememberMeService rememberMeService;

  private final static String LOGIN_ERROR_PREFIX = "LoginManagerLoginError_";

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

  private int checkPatchca(String id, Optional<String> idcode) {
    String errCnt = null;
    try (Jedis c = jedisPool.getResource()) {
      errCnt = c.get(LOGIN_ERROR_PREFIX + id);
    }
    if (errCnt != null && Integer.valueOf(errCnt) >= 3) {
      if (idcode.isPresent()) {
        String text = patchcaService.getText(id);
        if (!idcode.get().equals(text)) {
          return Errno.IDENTIFY_CODE_ERROR;
        }
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
    String rand = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
    if (isEmail(account)) {
      throw new RuntimeException("not implemented");
    } else {
      smsService.send(account, rand, "【搜狗科技】你的验证码是 " + rand);
    }
    return ApiResult.ok();
  }

  public ApiResult addAccount(String accountName, String password, String regcode) {
    Account account = new Account();
    String code;
    if (isEmail(accountName)) {
      code = null;
      account.setEmail(accountName);
    } else {
      code = smsService.get(accountName);
      account.setPhone(accountName);
    }

    if (!regcode.equals(code)) {
      return new ApiResult(Errno.IDENTIFY_CODE_ERROR);
    }

    try {
      account.setPassword(encodePassword(password));
      accountMapper.add(account);
    } catch (DuplicateKeyException e) {
      return new ApiResult(Errno.USER_EXISTS);
    }

    return new ApiResult<Account>(account);
  }

  public ApiResult resetPassword(String accountName, String password,
                                 String regcode, Optional<String> idcode) {
    int errno = checkPatchca(accountName, idcode);
    if (errno != Errno.OK) return new ApiResult(errno);

    String code;
    boolean isEmail = this.isEmail(accountName);
    if (isEmail) {
      code = null;
    } else {
      code = smsService.get(accountName);
    }

    if (!regcode.equals(code)) {
      errno = logLoginError(accountName);
      if (errno != Errno.OK) return new ApiResult(errno);
      else return new ApiResult(Errno.IDENTIFY_CODE_ERROR);
    }

    accountMapper.updatePasswordByPhone(accountName, encodePassword(password));
    return ApiResult.ok();
  }

  public ApiResult resetPassword(long uid, String password, String oldPassword,
                                 HttpServletResponse response) {
    Account account = accountMapper.find(uid);
    if (account == null) return ApiResult.internalError("uid not found");

    if (checkPassword(password, account.getPassword())) {
      accountMapper.updatePasswordById(uid, encodePassword(password));
      String token = rememberMeService.login(
        response, new RedisRememberMeService.User(uid, account.getPerm()));
      return new ApiResult<String>(token);
    } else {
      return new ApiResult(Errno.USER_PASSWORD_ERROR);
    }
  }

  public ApiResult updateAccount(RedisRememberMeService.User user, Account account) {
    if (account.getId() == user.getUid()) {
      account.setStatus(null);
      account.setPerm(Integer.MIN_VALUE);
      accountMapper.update(account);
    } else {
      Account otherAccount = accountMapper.find(account.getId());
      if (otherAccount == null) return ApiResult.notFound();
      if (Account.permGt(user.getPerm(), otherAccount.getPerm())) {
        accountMapper.update(account);
      } else {
        return ApiResult.forbidden();
      }
    }
    return ApiResult.ok();
  }

  public ApiResult getAccount(RedisRememberMeService.User user, Optional<String> accountName) {
    Account account;
    if (accountName.isPresent()) {
      account = isEmail(accountName.get()) ? accountMapper.findByEmail(accountName.get()) :
        accountMapper.findByPhone(accountName.get());
      if (account != null && account.getId() != user.getUid() &&
          Account.permGt(account.getPerm(), user.getPerm())) {
        return ApiResult.forbidden();
      }
    } else {
      account = accountMapper.find(user.getUid());      
    }
    if (account == null) return ApiResult.notFound();
    return new ApiResult<Account>(account);
  }

  public ApiResult login(String accountName, String password,
                         Optional<String> idcode, HttpServletResponse response) {
    int errno = checkPatchca(accountName, idcode);
    if (errno != Errno.OK) return new ApiResult(errno);
    
    Account account;
    if (isEmail(accountName)) {
      account = null;
    } else {
      account = accountMapper.findByPhone(accountName);
    }
    
    if (account == null || !checkPassword(password, account.getPassword())) {
      errno = logLoginError(accountName);
      if (errno != Errno.OK) return new ApiResult(errno);
      return new ApiResult(account == null ? Errno.USER_NOT_FOUND : Errno.USER_PASSWORD_ERROR);
    }

    String token = rememberMeService.login(
      response, new RedisRememberMeService.User(account.getId(), account.getPerm()));
    
    return new ApiResult<String>(token);
  }

  public ApiResult logout(long id, HttpServletResponse response) {
    rememberMeService.logout(id, response);
    return ApiResult.ok();
  }
}
