package ms.login.entity;

import javax.validation.constraints.*;
import org.jsondoc.core.annotation.*;

@ApiObject(name = "OpenAccount", description = "OpenAccount")
public class OpenAccount {
  @ApiObjectField(description = "openId")
  @Size(max = 32)
  String openId;

  @ApiObjectField(description = "uid")
  long uid = Long.MIN_VALUE;

  public void setOpenId(String openId) {
    this.openId = openId;
  }
  public String getOpenId() {
    return this.openId;
  }

  public void setUid(long uid) {
    this.uid = uid;
  }
  public long getUid() {
    return this.uid;
  }

}

