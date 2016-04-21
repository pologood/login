package ms.login.api;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.http.*;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import commons.spring.RedisRememberMeService;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.manager.*;

@Api(
  name = "register/login API",
  description = "api about register login and basic account info"
)
@ApiAuthToken
@RestController
@RequestMapping("/api")
public class LoginController {
  @Autowired LoginManager loginManager;

  static Pattern idCodePattern = Pattern.compile("^[a-zA-Z0-9]{16}$");

  @ApiMethod(description = "get identifying code, id OR account required")
  @RequestMapping(value = "/idcode", method = RequestMethod.GET)
  public ResponseEntity<Void> getIdentifyingCode(HttpServletResponse response,
    @ApiQueryParam(name = "id", description = "random alnum", format = "[a-zA-Z0-9]{32}")
    @RequestParam(required = false) Optional<String> id,
    @ApiQueryParam(name = "account", description = "phone OR email")
    @RequestParam(required = false) Optional<String> account) throws IOException {

    OutputStream os = response.getOutputStream();

    byte[] img;
    if (account.isPresent()) {
      img = loginManager.getIdentifyingCode(account.get());
    } else if (id.isPresent()) {
      if (idCodePattern.matcher(id.get()).find()) {
        img = loginManager.getIdentifyingCode(id.get());
      } else {
        os.write("id format error".getBytes());
        return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
      }
    } else {
      os.write("id or account is required".getBytes());
      return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    // HttpHeaders not work, have not figure out
    response.setContentType("image/png");
    response.setHeader("cache", "no-cache");
    os.write(img);
    return new ResponseEntity<Void>(HttpStatus.OK);
  }

  @ApiMethod(description = "get register code")
  @RequestMapping(value = "/regcode", method = RequestMethod.GET)
  public ApiResult getRegisterCode(
    @ApiQueryParam(name = "account", description = "phone or email")
    @RequestParam String account) {
    return loginManager.getRegisterCode(account);
  }

  @ApiMethod(description = "register account")
  @RequestMapping(value = "/account", method = RequestMethod.POST)
  public ApiResult addAccount(
    @ApiQueryParam(name = "account", description = "phone OR email")
    @RequestParam String account,
    @ApiQueryParam(name = "regcode", description = "register code")
    @RequestParam String regcode,
    @ApiQueryParam(name = "password", description = "password")
    @RequestParam String password) {
    return loginManager.addAccount(account, password, regcode);
  }

  @ApiMethod(description = "reset password, use regcode or oldPassword")
  @RequestMapping(value = "/password", method = RequestMethod.PUT)
  public ApiResult resetPassword(
    @AuthenticationPrincipal RedisRememberMeService.User user,    
    @ApiQueryParam(name = "account", description = "phone OR email", required = false)
    @RequestParam Optional<String> account,
    @ApiQueryParam(name = "regcode", description = "register code", required = false)
    @RequestParam Optional<String> regcode,
    @ApiQueryParam(name = "idcode", description = "when regcode wrong more than 3")
    @RequestParam Optional<String> idcode,
    @ApiQueryParam(name = "oldPassword", description = "old password", required = false)
    @RequestParam Optional<String> oldPassword,
    @ApiQueryParam(name = "password", description = "password")
    @RequestParam String password,
    HttpServletResponse response) {

    if (regcode.isPresent()) {
      if (account.isPresent()) {
        return loginManager.resetPassword(account.get(), password, regcode.get(), idcode);
      } else {
        return ApiResult.badRequest("account is required");
      }
    } else if (oldPassword.isPresent()) {
      if (user != null) {
        return loginManager.resetPassword(user, password, oldPassword.get(), response);
      } else {
        return ApiResult.unAuthorized();
      }
    } else {
      return ApiResult.badRequest("regcode OR oldPassword is required");
    }
  }

  @ApiMethod(description = "update account")
  @RequestMapping(value = "/account", method = RequestMethod.PUT,
                  consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}
  )
  public ApiResult updateAccount(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiBodyObject Account account) {
    account.setId(user.getUid());
    return loginManager.updateAccount(account);
  }

  @ApiMethod(description = "get account")
  @RequestMapping(value = {"/account", "/account/{account}"}, method = RequestMethod.GET)
  public ApiResult getAccount(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiPathParam(name = "account", description = "other account's phone OR email")
    @PathVariable Optional<String> account) {
    return loginManager.getAccount(user, account);
  }

  @ApiMethod(description = "login, phone OR email must provide one")
  @RequestMapping(value = "/login", method = RequestMethod.GET)
  public ApiResult login(
    @ApiQueryParam(name = "account", description = "account phone or email")
    @RequestParam String account,
    @ApiQueryParam(name = "password", description = "account password")
    @RequestParam String password,
    @ApiQueryParam(name = "idcode", description = "when password wrong more than 3, need idcode")
    @RequestParam(required = false) Optional<String> idcode,
    HttpServletResponse response) {
    return loginManager.login(account, password, idcode, response);
  }

  @ApiMethod(description = "logout")
  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public ApiResult logout(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    HttpServletResponse response) {
    return loginManager.logout(user.getUid(), response);
  }
}
