package ms.login.entity;

import java.time.LocalDateTime;
import org.jsondoc.core.annotation.*;

@ApiObject(name = "AccountPerm", description = "account's permission")
public class AccountPerm {
  @ApiObjectField(description = "account id")
  private long   uid;
  
  @ApiObjectField(description = "corporation id")
  private long   incId;

  @ApiObjectField(description = "permission entity")
  private String entity;

  @ApiObjectField(description = "permission id")
  private long   permId;

  @ApiObjectField(description = "grant option")
  private boolean grant = false;

  @ApiObjectField(name = "createTime", description = "create time")
  private LocalDateTime createTime;

  @ApiObjectField(name = "updateTime", description = "update time")
  private LocalDateTime updateTime;

  public void setUid(long uid) {
    this.uid = uid;
  }
  public long getUid() {
    return this.uid;
  }

  public void setIncId(long incId) {
    this.incId = incId;
  }
  public long getIncId() {
    return this.incId;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }
  public String getEntity() {
    return this.entity;
  }

  public void setPermId(long permId) {
    this.permId = permId;
  }
  public long getPermId() {
    return this.permId;
  }

  public void setGrant(boolean grant) {
    this.grant = grant;
  }
  public boolean getGrant() {
    return this.grant;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
  public LocalDateTime getCreateTime() {
    return this.createTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }
  public LocalDateTime getUpdateTime() {
    return this.updateTime;
  }

  private boolean entityEqual(String entity) {
    if (this.entity == null) {
      return entity == null;
    } else {
      return this.entity.equals(entity);
    }
  }

  public boolean canGrantPerm(AccountPerm perm) {
    boolean idOk;
    if (permId <= Account.OWNER) {
      idOk = permId < perm.getPermId();
    } else if (permId < Account.PERM_EXIST) {
      idOk = permId <= perm.getPermId();
    } else {
      idOk = permId == perm.getPermId();
    }

    boolean entityOk = entityEqual(perm.getEntity());
    return idOk && entityOk && grant;
  }

  public boolean canRevokePerm(AccountPerm perm) {
    boolean idOk = true;
    if (permId < Account.PERM_EXIST) {
      idOk = permId < perm.getPermId();
    }

    return idOk && entityEqual(perm.getEntity());
  }
}
