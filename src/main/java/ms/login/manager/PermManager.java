package ms.login.manager;

import java.util.*;
import redis.clients.jedis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import commons.utils.*;
import static commons.spring.RedisRememberMeService.User;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.mapper.*;

@Component
public class PermManager {
  @Autowired AccountMapper     accountMapper;
  @Autowired SysPermMapper     sysPermMapper;
  @Autowired IncPermMapper     incPermMapper;
  @Autowired AccountPermMapper accountPermMapper;
  @Autowired JedisPool         jedisPool;

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
    accountMapper.updateIncIdAndPerm(uid, incId, Account.BOSS);
    return ApiResult.ok();
  }

  public ApiResult getInvitationCode(int incId) {
    String inc = String.valueOf(incId);
    String code = StringHelper.random(32);

    try (Jedis c = jedisPool.getResource()) {
      c.setex(code, 86400, inc);
    }

    String url = "/inc/user?incId=" + inc + "&code=" + code;
    return new ApiResult<String>(url);
  }

  public ApiResult joinInc(long uid, int incId, String code) {
    String inc = null;
    try (Jedis c = jedisPool.getResource()) {
      inc = c.get(code);
    }
    if (inc == null) return new ApiResult(Errno.EXPIRED_INVATATION_CODE);
    if (!inc.equals(String.valueOf(incId))) return new ApiResult(Errno.INVALID_INVATATION_CODE);

    accountMapper.updateIncIdAndPerm(uid, incId, Account.PERM_EXIST);
    return ApiResult.ok();
  }

  public ApiResult grantPerm(User user, long uid, List<Long> permIds, List<Boolean> options) {
    if (user.getPerm() != Account.BOSS) {
      List<AccountPerm> userPerms = accountPermMapper.getAll(user.getUid());
      for (long perm : permIds) {
        boolean hasPerm = false;
        for (AccountPerm userPerm : userPerms) {
          if (userPerm.getPerm() == perm) hasPerm = userPerm.getGrant();
        }
        if (!hasPerm) return ApiResult.forbidden();
      }
    }

    List<AccountPerm> perms = new ArrayList<>();
    for (int i = 0; i < permIds.size(); ++i) {
      AccountPerm perm = new AccountPerm();
      perm.setUid(uid);
      perm.setIncId(user.getIncId());
      perm.setPerm(permIds.get(i));
      perm.setGrant(options.get(i));

      perms.add(perm);
    }

    accountPermMapper.add(perms);
    return ApiResult.ok();
  }
  
  public ApiResult revokePerm(long uid, List<Long> permIds) {
    for (long permId : permIds) {
      accountPermMapper.delete(uid, permId);
    }
    
    return ApiResult.ok();
  }
}
