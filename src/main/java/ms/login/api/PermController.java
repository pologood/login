package ms.login.api;

import java.util.*;
import javax.servlet.http.*;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import static commons.spring.RedisRememberMeService.User;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.manager.*;

@Api(
  name = "Permission API",
  description = "Permission create/grant/revoke API"
)
@ApiAuthToken
@RestController
@RequestMapping("/api")
public class PermController {
  @Autowired PermManager permManager;

  @ApiMethod(description = "Get System Range Permission")
  @RequestMapping(value = "/perm/sys", method = RequestMethod.GET)
  public ApiResult getSysPerm() {
    return permManager.getSysPerm();
  }

  @ApiMethod(description = "Add System Range Permission")
  @RequestMapping(value = "/perm/sys", method = RequestMethod.POST)
  public ApiResult addSysPerm(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "name", description = "permission name")
    @RequestParam String name,
    @ApiQueryParam(name = "desc", description = "permission description")
    @RequestParam(required = false) String desc) {
    if (!Account.permGe(user.getPerm(), Account.PLAT_ADMIN)) return ApiResult.forbidden();
    return permManager.addSysPerm(name, desc);
  }

  @ApiMethod(description = "Update System Range Permission Name")
  @RequestMapping(value = "/perm/sys/{id}", method = RequestMethod.PUT)
  public ApiResult updateSysPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "id", description = "permission id")
    @PathVariable long id,
    @ApiQueryParam(name = "name", description = "permission name")
    @RequestParam(required = false) String name,
    @ApiQueryParam(name = "desc", description = "permission description")
    @RequestParam(required = false) String desc) {
    if (!Account.permGe(user.getPerm(), Account.PLAT_ADMIN)) return ApiResult.forbidden();
    if (name == null || desc == null) return ApiResult.badRequest("");
    return permManager.updateSysPerm(id, name, desc);
  }

  @ApiMethod(description = "Delete System Range Permission")
  @RequestMapping(value = "/perm/sys/{id}", method = RequestMethod.DELETE)
  public ApiResult deleteSysPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "id", description = "permission id")
    @PathVariable long id) {
    if (!Account.permGe(user.getPerm(), Account.PLAT_ADMIN)) return ApiResult.forbidden();
    return permManager.deleteSysPerm(id);
  }

  @ApiMethod(description = "Get Corporation Range Permission")
  @RequestMapping(value = "/perm/inc/{id}", method = RequestMethod.GET)
  public ApiResult getIncPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "id", description = "corporation id")
    @PathVariable int id) {
    if (user.getIncId() != id) return ApiResult.forbidden();
    return permManager.getIncPerm(id);
  }

  @ApiMethod(description = "Add Corporation Range Permission")
  @RequestMapping(value = "/perm/inc/{id}", method = RequestMethod.POST)
  public ApiResult addIncPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "id", description = "corporation id")
    @PathVariable int id,
    @ApiQueryParam(name = "name", description = "permission name")
    @RequestParam String name,
    @ApiQueryParam(name = "desc", description = "permission description")
    @RequestParam(required = false) String desc) {
    if (user.getIncId() != id || user.getPerm() != Account.BOSS) return ApiResult.forbidden();
    return permManager.addIncPerm(id, name, desc);
  }

  @ApiMethod(description = "Update Corporation Range Permission Name")
  @RequestMapping(value = "/perm/inc/{incId}/{permId}", method = RequestMethod.PUT)
  public ApiResult updateIncPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "incId", description = "corporation id")
    @PathVariable int incId,
    @ApiPathParam(name = "permId", description = "permission id")
    @PathVariable long permId,
    @ApiQueryParam(name = "name", description = "permission name")
    @RequestParam(required = false) String name,
    @ApiQueryParam(name = "desc", description = "permission description")
    @RequestParam(required = false) String desc) {
    if (user.getIncId() != incId || user.getPerm() != Account.BOSS) return ApiResult.forbidden();
    if (name == null && desc == null) return ApiResult.badRequest("");
    return permManager.updateIncPerm(permId, incId, name, desc);
  }

  @ApiMethod(description = "Delete Corportation Range Permission")
  @RequestMapping(value = "/perm/inc/{incId}/{permId}", method = RequestMethod.DELETE)
  public ApiResult deleteIncPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "incId", description = "corporation id")
    @PathVariable int incId,
    @ApiPathParam(name = "permId", description = "permission id")
    @PathVariable long permId) {
    if (user.getIncId() != incId || user.getPerm() != Account.BOSS) return ApiResult.forbidden();
    return permManager.deleteIncPerm(permId, incId);
  }

  @ApiMethod(description = "Grant Boss permission")
  @RequestMapping(value = "/perm/boss/{uid}", method = RequestMethod.PUT)
  public ApiResult grantBoss(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "uid", description = "user id")
    @PathVariable long uid,
    @ApiQueryParam(name = "incId", description = "corporation id")
    @RequestParam int incId) {
    if (!user.isPlatformAdmin()) return ApiResult.forbidden();
    return permManager.grantBoss(uid, incId);
  }

  @ApiMethod(description = "get invitation code")
  @RequestMapping(value = "/inc/user/invitaionCode", method = RequestMethod.GET)
  public ApiResult getInvitationCode(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "account", description = "invitation account, phone or email")
    @RequestParam List<String> account,
    @ApiQueryParam(name = "permId", description = "perm Id")
    @RequestParam Optional<Long> permId) {
    if (user.getPerm() != Account.BOSS) return ApiResult.forbidden();
    return permManager.getInvitationCode(user.getIncIdString(), account, permId.orElse(-1L));
  }

  @ApiMethod(description = "get inc member")
  @RequestMapping(value = "/inc/user", method = RequestMethod.GET)
  public ApiResult getIncMember(@AuthenticationPrincipal User user) {
    return permManager.getIncMember(user.getIncId());
  }
  

  @ApiMethod(description = "Invite uid Join Corporation")
  @RequestMapping(value = "/inc/user", method = RequestMethod.PUT)
  public ApiResult joinInc(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "code", description = "invitation code")
    @RequestParam String code) {
    return permManager.joinInc(user.getUid(), code);
  }

  @ApiMethod(description = "delete user from inc")
  @RequestMapping(value = "/inc/user/{uid}", method = RequestMethod.DELETE)
  public ApiResult deleteFromInc(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "uid", description = "user id")
    @PathVariable long uid) {
    
    if (!user.isBoss()) return ApiResult.forbidden();
    return permManager.deleteFromInc(user.getIncId(), uid);
  }

  @ApiMethod(description = "get account by entity")
  @RequestMapping(value = "/perm/entity/{entity}", method = RequestMethod.GET)
  public ApiResult getAccountByEntity(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "entity", description = "entity")
    @PathVariable String entity) {

    return permManager.getAccountByEntity(user, entity);
  }

  /* WARNINGS: account may something like me@google.com,
   *  if put in request path, .com will be spring truncated
   */
  @ApiMethod(description = "transfer owner")
  @RequestMapping(value = "/perm/owner/{entity}/account", method = RequestMethod.POST)
  public ApiResult transferOwner(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "entity", description = "entity")
    @PathVariable String entity,
    @ApiQueryParam(name = "account", description = "user account")
    @RequestParam String account) {

    return permManager.grantOwner(user, account, entity);
  }  

  @ApiMethod(description = "transfer owner")
  @RequestMapping(value = "/perm/owner/{entity}/user/{uid}", method = RequestMethod.POST)
  public ApiResult transferOwner(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "entity", description = "entity")
    @PathVariable String entity,
    @ApiPathParam(name = "uid", description = "user id")
    @PathVariable long uid) {

    return permManager.grantOwner(user, uid, entity);
  }

  @ApiMethod(description = "Grant permission to Email/Phone")
  @RequestMapping(value = "/perm/account", method = RequestMethod.PUT)
  public ApiResult grantPerm(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "account", description = "user account")
    @RequestParam String account,
    @ApiQueryParam(name = "perms", description = "permission list")
    @RequestParam List<String> perms) {
    return permManager.grantPerm(user, account, -1, perms);
  }  

  @ApiMethod(description = "Grant permission")
  @RequestMapping(value = "/perm/user/{uid}", method = RequestMethod.PUT)
  public ApiResult grantPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "uid", description = "user id")
    @PathVariable long uid,
    @ApiQueryParam(name = "perms", description = "permission list")
    @RequestParam List<String> perms) {
    return permManager.grantPerm(user, uid, -1, perms);
  }

  @ApiMethod(description = "Alter permission of Email/Phone")
  @RequestMapping(value = "/xperm/account", method = RequestMethod.PUT)
  public ApiResult alterPerm(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "account", description = "user account")
    @RequestParam String account,
    @ApiQueryParam(name = "oldPerms", description = "old permission list")
    @RequestParam List<String> oldPerms,
    @ApiQueryParam(name = "newPerms", description = "new permission list")
    @RequestParam List<String> newPerms) {
    return permManager.alterPerm(user, account, -1, oldPerms, newPerms);
  }  

  @ApiMethod(description = "alter permission of Email/Phone")
  @RequestMapping(value = "/xperm/user/{uid}", method = RequestMethod.PUT)
  public ApiResult alterPerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "uid", description = "user id")
    @PathVariable long uid,
    @ApiQueryParam(name = "oldPerms", description = "old permission list")
    @RequestParam List<String> oldPerms,
    @ApiQueryParam(name = "newPerms", description = "new permission list")
    @RequestParam List<String> newPerms) {
    return permManager.alterPerm(user, uid, -1, oldPerms, newPerms);
  }

  @ApiMethod(description = "Revoke permission from Email/Phone")
  @RequestMapping(value = "/perm/account", method = RequestMethod.DELETE)
  public ApiResult revokePerm(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "account", description = "user account")
    @RequestParam String account,
    @ApiQueryParam(name = "perms", description = "permission list")
    @RequestParam List<String> perms) {
    
    return permManager.revokePerm(user, account, -1, perms);    
  }  

  @ApiMethod(description = "Revoke permission")
  @RequestMapping(value = "/perm/user/{uid}", method = RequestMethod.DELETE)
  public ApiResult revokePerm(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "uid", description = "user id")
    @PathVariable long uid,
    @ApiQueryParam(name = "perms", description = "permission list")
    @RequestParam List<String> perms) {
    
    return permManager.revokePerm(user, uid, -1, perms);    
  }
}
