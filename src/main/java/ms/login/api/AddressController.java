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
  name = "Address API",
  description = "Address Manage API"
)
@ApiAuthToken
@RestController
@RequestMapping("/api")
public class AddressController {
  @Autowired AddressManager addressManager;

  @ApiMethod(description = "Get Address")
  @RequestMapping(value = "/address", method = RequestMethod.GET)
  public ApiResult getAddress(@AuthenticationPrincipal User user) {
    return addressManager.getAddress(user.getId());
  }

  @ApiMethod(description = "Get Default Address")
  @RequestMapping(value = "/address/default", method = RequestMethod.GET)
  public ApiResult getDefaultAddress(@AuthenticationPrincipal User user) {
    return addressManager.getDefaultAddress(user.getId());
  }
  
  @ApiMethod(description = "Set Address")
  @RequestMapping(value = "/address", method = RequestMethod.PUT)
  public ApiResult setAddress(
    @AuthenticationPrincipal User user,
    @ApiQueryParam(name = "def", description = "default address")
    @RequestParam(required = false) Optional<Integer> def,
    @ApiQueryParam(name = "address1", description = "address 1")
    @RequestParam(required = false) String address1,
    @ApiQueryParam(name = "address2", description = "address 2")
    @RequestParam(required = false) String address2,
    @ApiQueryParam(name = "address3", description = "address 3")
    @RequestParam(required = false) String address3,
    @ApiQueryParam(name = "address4", description = "address 4")
    @RequestParam(required = false) String address4,
    @ApiQueryParam(name = "address5", description = "address 5")
    @RequestParam(required = false) String address5,
    @ApiQueryParam(name = "address6", description = "address 6")
    @RequestParam(required = false) String address6,
    @ApiQueryParam(name = "address7", description = "address 7")
    @RequestParam(required = false) String address7,
    @ApiQueryParam(name = "address8", description = "address 8")
    @RequestParam(required = false) String address8,
    @ApiQueryParam(name = "address9", description = "address 9")
    @RequestParam(required = false) String address9) {

    if (!def.isPresent() && address1 == null && address2 == null && address3 == null &&
        address4 == null && address5 == null && address6 == null && address7 == null &&
        address8 == null && address9 == null) {
      return ApiResult.ok();
    }

    Address address = new Address();
    address.setUid(user.getId());
    if (def.isPresent()) address.setDef(def.get());
    if (address1 != null) address.setAddress1(address1);
    if (address2 != null) address.setAddress2(address2);
    if (address3 != null) address.setAddress3(address3);
    if (address4 != null) address.setAddress4(address4);
    if (address5 != null) address.setAddress5(address5);
    if (address6 != null) address.setAddress6(address6);
    if (address7 != null) address.setAddress7(address7);
    if (address8 != null) address.setAddress8(address8);
    if (address9 != null) address.setAddress9(address9);

    address.makeXssSafe();

    return addressManager.setAddress(address);
  }

  @ApiMethod(description = "Delete Address")
  @RequestMapping(value = "/address/{id}", method = RequestMethod.DELETE)
  public ApiResult deleteAddress(
    @AuthenticationPrincipal User user,
    @ApiPathParam(name = "id", description = "address id")
    @PathVariable int id) {
    return addressManager.deleteAddress(user.getId(), id);
  }
}
