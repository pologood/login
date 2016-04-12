package ms.login.api;

import java.util.*;
import java.util.regex.*;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import ms.login.model.*;
import ms.login.manager.*;

@Api(
  name = "register/login API",
  description = "api about register login and basic account info"
)
@RestController
@RequestMapping("/api")
public class LoginController {
  @Autowired LoginManager loginManager;

  static Pattern idCodePattern = Pattern.compile("^[a-zA-Z0-9]{32}$");

  @ApiMethod(description = "get identifying code, id OR account required")
  @RequestMapping(value = "/idcode", method = RequestMethod.GET)
  public ApiResult getIdentifyingCode(
    @ApiQueryParam(name = "id", description = "random alnum", format = "[a-zA-Z0-9]{32}")
    @RequestParam(required = false) Optional<String> id,
    @ApiQueryParam(name = "account", description = "phone OR email")
    @RequestParam(required = false) Optional<String> account) {
    if (account.isPresent()) {
      return loginManager.getIdentifyingCode(account.get());
    } else if (id.isPresent()) {
      if (idCodePattern.matcher(id.get()).find()) {
        return loginManager.getIdentifyingCode(id.get());
      } else {
        return ApiResult.badRequest("id format error");
      }
    } else {
      return ApiResult.badRequest("id or account is required");
    }
  }

  @ApiMethod(description = "get register code")
  @RequestMapping(value = "regcode", method = RequestMethod.GET)
  public ApiResult getRegisterCode(
    @ApiQueryParam(name = "account", description = "phone or email")
    @RequestParam String account) {
    return loginManager.getRegisterCode(account);
  }

  @ApiMethod(description = "register account")
  @RequestMapping(value = "/account", method = RequestMethod.POST)
  public ApiResult addAccount() {
    return ApiResult.ok();
  }

  @ApiMethod(description = "update phone")
  @RequestMapping(value = "/account/{id}/phone", method = RequestMethod.PUT,
                  consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public ApiResult updateAccountPhone(
    @ApiPathParam(name = "id", description = "account id", format = "digit")
    @PathVariable long id,
    @ApiQueryParam(name = "phone", description = "new phone")
    @RequestParam String phone,
    @ApiQueryParam(name = "email", description = "current email")
    @RequestParam String email) {
    return ApiResult.ok();
  }  

  @ApiMethod(description = "login, phone OR email must provide one")
  @RequestMapping(value = "/login", method = RequestMethod.GET)
  public ApiResult login(
    @ApiQueryParam(name = "account", description = "account phone or email")
    @RequestParam String account,
    @ApiQueryParam(name = "password", description = "account password")
    @RequestParam String password,
    @ApiQueryParam(name = "idcode", description = "when password wrong more than 3, need idcode")
    @RequestParam(required = false) Optional<String> idcode) {
    return loginManager.login(account, password, idcode);
  }

  /*
    if (account.indexOf('@') != -1) {
      return loginManager.loginByEmail(email.get(), password);
    } else {
      return loginManager.loginByPhone(phone.get(), password);
    }
    }*/

  @ApiMethod(description = "logout")
  @RequestMapping(value = "/logout/{id}", method = RequestMethod.GET)
  public ApiResult logout(
    @ApiPathParam(name = "id", description = "account id", format = "digit")
    @PathVariable long id) {
    return loginManager.logout(id);
  }
}
