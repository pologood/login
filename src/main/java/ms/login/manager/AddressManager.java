package ms.login.manager;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ms.login.model.*;
import ms.login.entity.*;
import ms.login.mapper.*;

@Component
public class AddressManager {
  @Autowired AddressMapper addressMapper;
  
  public ApiResult getAddress(String uid) {
    Address address = addressMapper.get(uid);
    return new ApiResult<Address>(address);
  }

  public ApiResult getDefaultAddress(String uid) {
    Address address = addressMapper.get(uid);
    if (address == null) address = new Address();
    return new ApiResult<String>(address.getDefAddress());
  }

  public ApiResult setAddress(Address address) {
    addressMapper.addOrUpdate(address);
    return ApiResult.ok();
  }

  public ApiResult deleteAddress(String uid, int id) {
    addressMapper.delete(uid, id);
    return ApiResult.ok();
  }
}
