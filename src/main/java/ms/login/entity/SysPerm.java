package ms.login.entity;

import org.jsondoc.core.annotation.*;

@ApiObject(name = "SysPerm", description = "system defined permission")
public class SysPerm {
  public static enum Status {
    OK(1), DEPRESSED(2);
    private int value;
    Status(int value) {this.value = value;}
    public int getValue() {return this.value;}
  };
  
  @ApiObjectField(description = "perm id")
  long   id;

  @ApiObjectField(description = "perm name")
  String name;

  @ApiObjectField(description = "perm description")
  String desc;
  
  @ApiObjectField(description = "status")
  Status status;

  public void setId(long id) {
    this.id = id;
  }
  public long getId() {
    return this.id;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
  public Status getStatus() {
    return this.status;
  }

  public void setName(String name) {
    this.name = name;
  }
  public String getName() {
    return this.name;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }
  public String getDesc() {
    return this.desc;
  }
}
