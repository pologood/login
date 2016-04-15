package ms.login.entity;

import org.jsondoc.core.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@ApiObject(name = "Account", description = "Account info")
@JsonIgnoreProperties("password")
public class Account {
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

  @ApiObjectField(description = "perm")
  int perm = Integer.MIN_VALUE;

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

  public void setPerm(int perm) {
    this.perm = perm;
  }
  public int getPerm() {
    return this.perm;
  }

  public static boolean permGt(int a, int b) {
    return (b / 100 * 100 - a / 100 * 100 >= 100);
  }
}
