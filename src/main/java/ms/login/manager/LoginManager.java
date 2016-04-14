package ms.login.manager;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import redis.clients.jedis.*;
import ms.login.model.*;
import commons.saas.*;

@Component
public class LoginManager {
  @Autowired PatchcaService patchcaService;
  @Autowired JedisPool jedisPool;

  boolean isEmail(String account) {
    return account.indexOf('@') != -1;
  }

  public byte[] getIdentifyingCode(String id) {
    return patchcaService.getPatchca(id);
  }

  public ApiResult getRegisterCode(String account) {
    return ApiResult.ok();
  }

  public ApiResult login(String account, String password, Optional<String> idcode) {
    return ApiResult.ok();
  }

  public ApiResult logout(long id) {
    return ApiResult.ok();
  }
}
