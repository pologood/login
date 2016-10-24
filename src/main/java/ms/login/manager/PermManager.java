package ms.login.manager;

import java.util.*;
import redis.clients.jedis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    updateRememberMe(uid, null);
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

  void updateRememberMe(long uid, List<UserPerm> perms) {
    Account account = accountMapper.find(uid);
    if (account.getPerm() == Account.BOSS) {
      perms = Arrays.asList(new UserPerm(account.getPerm()));
    }
    rememberMeService.update(
      new User(uid, account.getName(), account.getIncId(), perms));
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
      updateRememberMe(uid, Arrays.asList(new UserPerm(permId)));      
    } else {
      updateRememberMe(uid, null);
    }

    return ApiResult.ok();
  }

  public ApiResult deleteFromInc(int incId, long uid) {
    accountPermMapper.deleteAll(uid, incId);
    accountMapper.revokeIncIdAndPerm(uid, Account.INC_NOTEXIST, Account.PERM_NOTEXIST);
    updateRememberMe(uid, null);
    return ApiResult.ok();
  }

  public void grantPermImpl(long uid, int incId, long permId, boolean option) {
    AccountPerm perm = new AccountPerm();
    perm.setUid(uid);
    perm.setIncId(incId);
    perm.setPermId(permId);
    perm.setGrant(option);

    accountPermMapper.add(perm);
  }

  public ApiResult grantPerm(User user, long uid, List<Long> permIds, List<Boolean> options) {
    if (user.getPerm() != Account.BOSS) {
      List<AccountPerm> userPerms = accountPermMapper.getAll(user.getUid());
      for (long perm : permIds) {
        boolean hasPerm = false;
        for (AccountPerm userPerm : userPerms) {
          if (userPerm.getPermId() == perm) hasPerm = userPerm.getGrant();
        }
        if (!hasPerm) return ApiResult.forbidden();
      }
    }

    for (int i = 0; i < permIds.size(); ++i) {
      grantPermImpl(uid, user.getIncId(), permIds.get(i), options.get(i));
    }

    updateRememberMe(uid, accountPermMapper.get(uid));
    return ApiResult.ok();
  }
  
  public ApiResult revokePerm(long uid, int incId, List<Long> permIds) {
    for (long permId : permIds) {
      accountPermMapper.delete(uid, incId, permId);
    }
    
    updateRememberMe(uid, accountPermMapper.get(uid));
    return ApiResult.ok();
  }
}
