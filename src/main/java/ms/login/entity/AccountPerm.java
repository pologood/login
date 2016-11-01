package ms.login.entity;

import org.jsondoc.core.annotation.*;

@ApiObject(name = "AccountPerm", description = "account's permission")
public class AccountPerm {
  @ApiObjectField(description = "account id")
  long   uid;
  
  @ApiObjectField(description = "corporation id")
  long   incId;

  @ApiObjectField(description = "permission entity")
  String entity;

  @ApiObjectField(description = "permission id")
  long   permId;

  @ApiObjectField(description = "grant option")
  boolean grant = false;

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
