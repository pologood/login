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
import commons.saas.LoginServiceProvider;
import commons.utils.StringHelper;
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
  @Autowired PermManager  permManager;

  static Pattern idCodePattern = Pattern.compile("^[a-zA-Z0-9]{16}$");

  @ApiMethod(description = "get identifying code, id OR account required")
  @RequestMapping(value = "/idcode", method = RequestMethod.GET)
  public ResponseEntity<Void> getIdentifyingCode(HttpServletResponse response,
    @ApiQueryParam(name = "id", description = "random alnum", format = "[a-zA-Z0-9]{16}")
    @RequestParam(required = false) Optional<String> id,
    @ApiQueryParam(name = "account", description = "phone OR email")
    @RequestParam(required = false) Optional<String> account) throws IOException {

    OutputStream os = response.getOutputStream();

    byte[] img;
    if (account.isPresent()) {
      img = loginManager.getIdentifyingCode(account.get());
    } else {
      if (id.isPresent()) {
        if (idCodePattern.matcher(id.get()).find()) {
          img = loginManager.getIdentifyingCode(id.get());
        } else {
          os.write("id format error".getBytes());
          return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
      } else {
        id = Optional.of(StringHelper.random(16));
        img = loginManager.getIdentifyingCode(id.get());
      }
    }

    // HttpHeaders not work, have not figure out
    response.setContentType("image/png");
    response.setHeader("cache", "no-cache");

    Cookie cookie = new Cookie("idcodetoken", account.isPresent() ? account.get() : id.get());
    cookie.setMaxAge(60);
    response.addCookie(cookie);
    
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
    @RequestParam String password,
    @ApiQueryParam(name = "name", description = "user nickname or realname", format = "\\w{1,16}")
    @RequestParam Optional<String> name,
    HttpServletResponse response) {
    if (name.isPresent()) {
      if (name.get().length() > 16) {
        return ApiResult.badRequest("length(name) <= 16");
      }
    }
    return loginManager.addAccount(account, password, regcode, name, response);
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
    @RequestParam(required = false) Optional<String> idcode,
    @ApiQueryParam(name = "id", description = "if use id get idcode, use id here")
    @RequestParam(required = false) Optional<String> id,
    @ApiQueryParam(name = "oldPassword", description = "old password", required = false)
    @RequestParam Optional<String> oldPassword,
    @ApiQueryParam(name = "password", description = "password")
    @RequestParam String password,
    @CookieValue(name = "idcodetoken", required = false) Optional<String> idc,
    HttpServletResponse response) {

    if (regcode.isPresent()) {
      if (account.isPresent()) {
        if (!id.isPresent()) id = idc;
        return loginManager.resetPassword(account.get(), password, regcode.get(), id, idcode);
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
    if (account.getName() != null) {
      if (account.getName().length() > 16) {
        return ApiResult.badRequest("length(name) <= 16");
      }
    }
      
    account.setId(user.getUid());
    return loginManager.updateAccount(user, account);
  }

  @ApiMethod(description = "get account")
  @RequestMapping(value = "/account", method = RequestMethod.GET)
  public ApiResult getAccount(
    @AuthenticationPrincipal RedisRememberMeService.User user) {
    return loginManager.getAccount(user);
  }
  
  @ApiMethod(description = "get other user account")
  @RequestMapping(value = "/account/{account}", method = RequestMethod.GET)
  public ApiResult getAccount(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiPathParam(name = "account", description = "other account's phone OR email OR openId")
    @PathVariable String account,
    @ApiQueryParam(name = "token", description = "use token to access other account")
    @RequestParam Optional<String> token) {
    return loginManager.getAccount(user, account, token);
  }

  @ApiMethod(description = "get other user account")
  @RequestMapping(value = "/account/uid/{uid}", method = RequestMethod.GET)
  public ApiResult getAccount(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiPathParam(name = "uid", description = "other account's uid")
    @PathVariable long uid,
    @ApiQueryParam(name = "token", description = "use token to access other account")
    @RequestParam Optional<String> token) {
    return loginManager.getAccount(user, uid, token);
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
    @ApiQueryParam(name = "id", description = "if use id get idcode, use id here")
    @RequestParam(required = false) Optional<String> id,
    @CookieValue(name = "idcodetoken", required = false) Optional<String> idc,
    @ApiQueryParam(name = "openId", description = "login and binding to openid") 
    @RequestParam Optional<String> openId,
    HttpServletResponse response) {
    
    if (!id.isPresent()) id = idc;
    return loginManager.login(account, password, id, idcode, openId.orElse(null), response);
  }

  @ApiMethod(description = "xiaop oauth login")
  @RequestMapping(value = "/login/oauth/xiaop", method = RequestMethod.GET)
  public ApiResult login(
    @ApiQueryParam(name = "token", description = "oauth token")
    @RequestParam String token,
    HttpServletResponse response) {
    return loginManager.login(token, LoginServiceProvider.Name.XiaoP, response);
  }

  @ApiMethod(description = "oauth login")
  @RequestMapping(value = "/login/oauth", method = RequestMethod.GET)
  public ApiResult login(
    @ApiQueryParam(name = "token", description = "oauth token")
    @RequestParam String token,
    @ApiQueryParam(name = "provider", description = "login service provider")
    @RequestParam LoginServiceProvider.Name provider,
    HttpServletResponse response) {
    return loginManager.login(token, provider, response);
  }

  @ApiMethod(description = "logout")
  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public ApiResult logout(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    HttpServletResponse response) {
    return loginManager.logout(user.getId(), response);
  }

  @ApiMethod(description = "get openId bind invitaion code")
  @RequestMapping(value = "/user/invitaionCode", method = RequestMethod.GET)
  public ApiResult getInvitaionCode(
    @AuthenticationPrincipal RedisRememberMeService.User user) {
    return permManager.getInvitationCode(user.getId());
  }

  @ApiMethod(description = "get openId bind list")
  @RequestMapping(value = "/user/openId", method = RequestMethod.GET)
  public ApiResult getBindOpenAccount(
    @AuthenticationPrincipal RedisRememberMeService.User user) {
    return loginManager.listBindOpenAccount(user.getUid());
  }

  @ApiMethod(description = "apply openId bind")
  @RequestMapping(value = "/user/openId", method = RequestMethod.PUT)
  public ApiResult applyBindOpenId(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiQueryParam(name = "code", description = "invitaion code")
    @RequestParam String code) {
    return loginManager.applyBindOpenId(user.getId(), code);
  }
  

  @ApiMethod(description = "accept openId bind")
  @RequestMapping(value = "/user/openId/{openId}", method = RequestMethod.PUT)
  public ApiResult acceptBindOpenId(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiPathParam(name = "openId", description = "the openId want to bind")
    @PathVariable String openId) {
    return loginManager.acceptBindOpenId(user.getUid(), openId);
  }
  
}
