package ms.login.entity;

import org.jsondoc.core.annotation.*;

@ApiObject(name = "IncPerm", description = "corporation defined permission")
public class IncPerm {
  @ApiObjectField(description = "perm id")
  long   id;

  @ApiObjectField(description = "corporation id")
  int   incId;

  @ApiObjectField(description = "perm name")
  String name;
  
  @ApiObjectField(description = "perm description")
  String desc;

  public void setId(long id) {
    this.id = id;
  }
  public long getId() {
    return this.id;
  }

  public void setIncId(int incId) {
    this.incId = incId;
  }
  public int getIncId() {
    return this.incId;
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
