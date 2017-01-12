package ms.login.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import javax.validation.constraints.*;
import org.jsondoc.core.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import static commons.spring.RedisRememberMeService.UserPerm;

@ApiObject(name = "Account", description = "Account info")
@JsonIgnoreProperties("password")
public class Account {
  public static final long PLAT_BOSS  = 0;
  public static final long PLAT_ROBOT = 1;
  public static final long PLAT_ADMIN = 10;
  public static final long BOSS       = 100;
  public static final long OWNER      = 101;
  public static final long ADMIN      = 201;
  public static final long WATCHER    = 1001;
  public static final long PERM_EXIST = 9_999;

  public static final long SYS_PERM_MIN   = 10_000;
  public static final long SYS_PERM_MAX   = 99_999;
  public static final long PERM_GROUP_MIN = 1_000_000;
  public static final long PERM_GROUP_MAX = 9_999_999;
  public static final long INC_PERM_MIN   = 10_000_000;

  public static final int  INC_NOTEXIST = Integer.MIN_VALUE;
  public static final long PERM_NOTEXIST = Long.MAX_VALUE;

  public static enum Status {
    OK(1);
    private int value;
    Status(int value) {this.value = value;}
    public int getValue() {return this.value;}
  };

  @ApiObjectField(description = "account id")
  long   id;

  @ApiObjectField(description = "phone number")
  @Size(max = 15)
  String phone;

  @ApiObjectField(description = "email address")
  @Size(max = 64)
  String email;

  @ApiObjectField(description = "password")
  String password;

  @ApiObjectField(description = "nickname")
  @Size(max = 16)
  String name;

  @ApiObjectField(description = "headimg")
  @Size(max = 256)
  String headImg;

  @ApiObjectField(description = "status")
  Status status;

  @ApiObjectField(description = "corporation id")
  int incId = Integer.MIN_VALUE;

  @ApiObjectField(description = "perm")
  long perm = Long.MAX_VALUE;

  @ApiObjectField(description = "grantPerms")
  List<UserPerm> grantPerms;

  @ApiObjectField(name = "createTime", description = "create time")
  LocalDateTime createTime;

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

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  public static boolean permGt(long a, long b) {
    return a < b;
  }
  public static boolean permGe(long a, long b) {
    return a <= b;
  }

  public void setGrantPerms(List<UserPerm> grantPerms) {
    this.grantPerms = grantPerms;
  }
  public List<UserPerm> getGrantPerms() {
    return this.grantPerms;
  }

  public Map<String, List<Long>> getPermsMap() {
    if (grantPerms == null) return null;

    Map<String, List<Long>> map = new HashMap<>();
    for (UserPerm perm : grantPerms) {
      String key = perm.getEntity() == null ? "" : perm.getEntity();

      List<Long> permIds = map.get(key);
      if (permIds == null) permIds = new ArrayList<>();
      permIds.add(perm.getPermId());

      map.put(key, permIds);
    }

    return map;
  }
}
