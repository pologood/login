package ms.login.entity;

import java.util.List;
import org.jsondoc.core.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@ApiObject(name = "Account", description = "Account info")
@JsonIgnoreProperties("password")
public class Account {
  public static final long PLAT_BOSS  = 0;
  public static final long PLAT_ROBOT = 1;
  public static final long PLAT_ADMIN = 10;
  public static final long BOSS       = 100;
  public static final long PERM_EXIST = 9_999;
  
  public static final long SYS_PERM_MIN   = 10_000;
  public static final long SYS_PERM_MAX   = 99_999;
  public static final long PERM_GROUP_MIN = 1_000_000;
  public static final long PERM_GROUP_MAX = 9_999_999;
  public static final long INC_PERM_MIN   = 10_000_000;
  
  public static enum Status {
    OK(1);
    private int value;
    Status(int value) {this.value = value;}
    public int getValue() {return this.value;}
  };
  
  @ApiObjectField(description = "account id")
  long   id;

  @ApiObjectField(description = "phone number")
  String phone;

  @ApiObjectField(description = "email address")
  String email;

  @ApiObjectField(description = "password")
  String password;

  @ApiObjectField(description = "nickname")
  String name;

  @ApiObjectField(description = "headimg")
  String headImg;
  
  @ApiObjectField(description = "status")
  Status status;

  @ApiObjectField(description = "corporation id")
  int incId = Integer.MIN_VALUE;

  @ApiObjectField(description = "perm")
  long perm = Long.MAX_VALUE;

  @ApiObjectField(description = "grantPerms")
  List<Long> grantPerms;

  public void setId(long id) {
    this.id = id;
  }
  public long getId() {
    return this.id;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }
  public String getPhone() {
    return this.phone;
  }

  public void setEmail(String email) {
    this.email = email;
  }
  public String getEmail() {
    return this.email;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  public String getPassword() {
    return this.password;
  }

  public void setName(String name) {
    this.name = name;
  }
  public String getName() {
    return this.name;
  }

  public void setHeadImg(String headImg) {
    this.headImg = headImg;
  }
  public String getHeadImg() {
    return this.headImg;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
  public Status getStatus() {
    return this.status;
  }

  public void setIncId(int incId) {
    this.incId = incId;
  }
  public int getIncId() {
    return this.incId;
  }

  public void setPerm(long perm) {
    this.perm = perm;
  }
  public long getPerm() {
    return this.perm;
  }

  public static boolean permGt(long a, long b) {
    return a < b;
  }
  public static boolean permGe(long a, long b) {
    return a <= b;
  }

  public void setGrantPerms(List<Long> grantPerms) {
    this.grantPerms = grantPerms;
  }
  public List<Long> getGrantPerms() {
    return this.grantPerms;
  }
}
