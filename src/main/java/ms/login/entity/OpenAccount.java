package ms.login.entity;

import javax.validation.constraints.*;
import org.jsondoc.core.annotation.*;
import static commons.saas.LoginService.User;

@ApiObject(name = "OpenAccount", description = "OpenAccount")
public class OpenAccount {
  @ApiObject
  public static enum Status {
    WAIT_AGREE(1), AGREE(2);
    private int value;
    Status(int value) { this.value = value; }
    public int getValue() { return this.value; }
  }
    
  @ApiObjectField(description = "openId")
  @Size(max = 32)
  String openId;

  @ApiObjectField(description = "uid")
  long uid = Long.MIN_VALUE;

  public OpenAccount() {
  }

  public OpenAccount(User user) {
    this.openId = openId;
    this.nickname = user.getName();
    this.headImg = headImg;
  }

  public void setOpenId(String openId) {
    this.openId = openId;
  }
  public String getOpenId() {
    return this.openId;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
  public String getNickname() {
    return this.nickname;
  }

  public void setHeadImg(String headImg) {
    this.headImg = headImg;
  }
  public String getHeadImg() {
    return this.headImg;
  }

  public void setUid(long uid) {
    this.uid = uid;
  }
  public long getUid() {
    return this.uid;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
  public Status getStatus() {
    return this.status;
  }
}

