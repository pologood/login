package ms.login.manager;

import java.util.*;
import redis.clients.jedis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.http.*;
import commons.utils.*;
import commons.spring.RedisRememberMeService;
import static commons.spring.RedisRememberMeService.User;
import static commons.spring.RedisRememberMeService.UserPerm;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.mapper.*;

@Component
public class PermManager {
  @Autowired AccountMapper          accountMapper;
  @Autowired SysPermMapper          sysPermMapper;
  @Autowired IncPermMapper          incPermMapper;
  @Autowired AccountPermMapper      accountPermMapper;
  @Autowired RedisRememberMeService rememberMeService;  
  @Autowired JedisPool              jedisPool;
  @Autowired LoginManager           loginManager;

  public ApiResult getSysPerm() {
    List<SysPerm> perms = sysPermMapper.getAll();
    return new ApiResult<List>(perms);
  }

  public ApiResult addSysPerm(String name, String desc) {
    SysPerm perm = new SysPerm();
    perm.setName(name);
    perm.setDesc(desc);

    sysPermMapper.add(perm);
    return new ApiResult<SysPerm>(perm);
  }

  public ApiResult updateSysPerm(long id, String name, String desc) {
    SysPerm perm = new SysPerm();
    perm.setId(id);
    perm.setName(name);
    perm.setDesc(desc);

    sysPermMapper.update(perm);
    return ApiResult.ok();
  }

  public ApiResult deleteSysPerm(long id) {
    SysPerm perm = new SysPerm();
    perm.setId(id);
    perm.setStatus(SysPerm.Status.DEPRESSED);
    
    sysPermMapper.update(perm);
    return ApiResult.ok();
  }

  public ApiResult getIncPerm(int incId) {
    List<IncPerm> perms = incPermMapper.getAll(incId);
    return new ApiResult<List>(perms);
  }

  public ApiResult addIncPerm(int incId, String name, String desc) {
    IncPerm perm = new IncPerm();
    perm.setIncId(incId);
    perm.setName(name);
    perm.setDesc(desc);

    incPermMapper.add(perm);
    return new ApiResult<IncPerm>(perm);
  }

  public ApiResult updateIncPerm(long permId, int incId, String name, String desc) {
    IncPerm perm = new IncPerm();
    perm.setId(permId);
    perm.setIncId(incId);
    perm.setName(name);
    perm.setDesc(desc);

    incPermMapper.update(perm);
    return ApiResult.ok();
  }

  public ApiResult deleteIncPerm(long permId, int incId) {
    incPermMapper.delete(permId, incId);
    return ApiResult.ok();
  }

  public ApiResult grantBoss(long uid, int incId) {
    int r = accountMapper.updateIncIdAndPerm(uid, incId, Account.BOSS);
    if (r <= 0) return new ApiResult(Errno.GRANT_BOSS_ERROR);
    updateRememberMe(uid);
    return ApiResult.ok();
  }

  private boolean isEmail(String account) {
    return account.indexOf('@') != -1;
  }

  public ApiResult grantOwner(User user, String accountName, String entity) {
    Account account = isEmail(accountName) ? accountMapper.findByEmail(accountName) :
      accountMapper.findByPhone(accountName);
    if (account == null) return ApiResult.notFound("account not found");

    return grantOwner(user, account.getId(), entity);
  }

  public ApiResult grantOwner(User user, long uid, String entity) {
    if (uid == user.getUid()) return ApiResult.ok();
    
    List<UserPerm> uperms = user.getPerms();
    boolean isOwner = false;
    for (UserPerm perm : uperms) {
      if (perm.getPermId() == Account.OWNER && entity.equals(perm.getEntity())) {
        isOwner = true;
        break;
      }
    }
    if (!isOwner) return ApiResult.forbidden();

    accountPermMapper.transfer(uid, user.getUid(), user.getIncId(), Account.OWNER, entity);
    updateRememberMe(uid);
    return ApiResult.ok();
  }

  public ApiResult getInvitationCode(String id) {
    return getInvitationCode(id, null, -1);
  }

  public ApiResult getInvitationCode(String id, List<String> accounts, long permId) {
    String code = StringHelper.random(32);
    if (accounts == null || accounts.isEmpty()) {
      JedisHelper.setex(jedisPool, code, 86400, id);
    } else {
      for (String account : accounts) {
        String value = id + ":" + account + ":" + permId;
        JedisHelper.setex(jedisPool, code, 86400, value);
      }
    }

    return new ApiResult<String>(code);
  }

  void updateRememberMe(long uid) {
    Account account = accountMapper.find(uid);
    if (account == null) return;

    List<UserPerm> perms = loginManager.getPermIds(account);
    rememberMeService.update(new User(uid, account.getName(), account.getIncId(), perms));
  }

  public ApiResult getIncMember(int incId) {
    List<AccountPerm> perms = accountPermMapper.getByIncId(incId);
    Map<Long, Account> map = new HashMap<>();
    for (AccountPerm perm : perms) {
      Account account = map.get(perm.getUid());
      if (account == null) {
        account = accountMapper.find(perm.getUid());
        if (account != null) {
          List<UserPerm> userPerms = new ArrayList<>();
          userPerms.add(new UserPerm(perm.getEntity(), perm.getPermId()));
          
          account.setGrantPerms(userPerms);
          map.put(perm.getUid(), account);
        }
      } else {
        account.getGrantPerms().add(new UserPerm(perm.getEntity(), perm.getPermId()));
      }
    }

    return new ApiResult<Collection>(map.values());
  }

  public ApiResult joinInc(long uid, String code) {
    String value = JedisHelper.get(jedisPool, code);
    if (value == null) return new ApiResult(Errno.EXPIRED_INVATATION_CODE);

    String parts[] = value.split(":", 3);
    if (parts.length != 3) return new ApiResult(Errno.INVALID_INVATATION_CODE);

    Account account = accountMapper.find(uid);
    if (!parts[1].equals(account.getPhone()) && !parts[1].equals(account.getEmail())) {
      return new ApiResult(Errno.INVALID_INVATATION_CODE);
    }

    int incId = Integer.parseInt(parts[0]);
    if (account.getIncId() >= 0 && account.getIncId() != incId) {
      return new ApiResult(Errno.INC_EXISTS);
    }
    accountMapper.updateIncIdAndPerm(uid, incId, Account.PERM_EXIST);

    long permId = Long.parseLong(parts[2]);
    if (permId != -1) {
      grantPermImpl(uid, incId, permId, false);
    }

    updateRememberMe(uid);
    return ApiResult.ok();
  }

  public ApiResult deleteFromInc(int incId, long uid) {
    accountPermMapper.deleteAll(uid, incId);
    accountMapper.revokeIncIdAndPerm(uid, Account.INC_NOTEXIST, Account.PERM_NOTEXIST);
    updateRememberMe(uid);
    return ApiResult.ok();
  }

  public ApiResult getAccountByEntity(User user, String entity) {
    boolean hasPerm = false;
    List<AccountPerm> perms = accountPermMapper.getEntityUser(entity);
    List<Account> accounts = new ArrayList<>();
    for (AccountPerm perm : perms) {
      if (user.getUid() == perm.getUid()) hasPerm = true;
      Account account = accountMapper.find(perm.getUid());
      if (account != null) {
        account.setPerm(perm.getPermId());
        account.setCreateTime(perm.getUpdateTime());
        accounts.add(account);
      }
    }

    if (!hasPerm) return ApiResult.forbidden();
    return new ApiResult<List>(accounts);      
  }

  public void grantPermImpl(long uid, int incId, long permId, boolean option) {
    AccountPerm perm = new AccountPerm();
    perm.setUid(uid);
    perm.setIncId(incId);
    perm.setPermId(permId);
    perm.setGrant(option);

    accountPermMapper.add(perm);
  }

  private List<AccountPerm> parsePerms(List<String> perms) {
    List<AccountPerm> uperms = new ArrayList<>();
    for (String perm : perms) {
      AccountPerm uperm = new AccountPerm();
      
      String parts[] = perm.split(":");
      uperm.setPermId(Long.parseLong(parts[0]));
      if (parts.length >= 2) {
        uperm.setGrant(parts[1].equals("t"));
      }
      if (parts.length >= 3) {
        uperm.setEntity(parts[2]);
      }

      uperms.add(uperm);
    }
    return uperms;
  }

  @Transactional
  public ApiResult grantPerm(User user, String accountName, int incId, List<String> perms) {
    Account account = isEmail(accountName) ? accountMapper.findByEmail(accountName) :
      accountMapper.findByPhone(accountName);
    if (account == null) return ApiResult.notFound("account not found");

    return grantPerm(user, account.getId(), incId, perms);
  }

  @Transactional
  public ApiResult grantPerm(User user, long uid, int incId, List<String> perms) {
    List<AccountPerm> uperms = parsePerms(perms);
    
    if (user.getPerm() != Account.BOSS && !user.isInternal()) {
      for (AccountPerm uperm : uperms) {
        // grant option check is skipped
        if (!user.canGrantPerm(uperm.getPermId(), uperm.getEntity())) {
          return ApiResult.forbidden();
        }
      }
    }

    for (AccountPerm perm : accountPermMapper.getAll(uid)) {
      for (AccountPerm uperm : uperms) {
        if (perm.canGrantPerm(uperm)) {
          return new ApiResult(Errno.HIGH_PERM_EXISTS);
        }
      }
    }

    for (AccountPerm perm : uperms) {
      perm.setUid(uid);
      perm.setIncId(incId);
      if (perm.getPermId() == Account.OWNER) perm.setGrant(true);
      accountPermMapper.add(perm);
    }

    updateRememberMe(uid);
    return ApiResult.ok();
  }

  @Transactional
  public ApiResult alterPerm(User user, String accountName, int incId,
                             List<String> operms, List<String> nperms) {
    Account account = isEmail(accountName) ? accountMapper.findByEmail(accountName) :
      accountMapper.findByPhone(accountName);
    if (account == null) return ApiResult.notFound("account not found");
    return alterPerm(user, account.getId(), incId, operms, nperms);
  }

  @Transactional
  public ApiResult alterPerm(User user, long uid, int incId,
                             List<String> operms, List<String> nperms) {
    ApiResult r = revokePerm(user, uid, incId, operms);
    return r.getCode() == 0 ? grantPerm(user, uid, incId, nperms) : r;
  }

  @Transactional
  public ApiResult revokePerm(User user, String accountName, int incId, List<String> perms) {
    Account account = isEmail(accountName) ? accountMapper.findByEmail(accountName) :
      accountMapper.findByPhone(accountName);
    if (account == null) return ApiResult.notFound("account not found");

    return revokePerm(user, account.getId(), incId, perms);
  }

  @Transactional
  public ApiResult revokePerm(User user, long uid, int incId, List<String> perms) {
    List<AccountPerm> uperms = parsePerms(perms);

    if (user.getPerm() != Account.BOSS && !user.isInternal()) {
      for (AccountPerm perm : uperms) {
        if (!user.canRevokePerm(perm.getPermId(), perm.getEntity())) {
          return ApiResult.forbidden();
        }
      }
    }

    for (AccountPerm perm : uperms) {
      perm.setUid(uid);
      perm.setIncId(incId);
      accountPermMapper.delete(perm);
    }

    updateRememberMe(uid);
    return ApiResult.ok();
  }
}
