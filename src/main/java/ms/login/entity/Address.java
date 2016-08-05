package ms.login.entity;

import org.jsondoc.core.annotation.*;
import commons.utils.XssHelper;

@ApiObject(name = "Address", description = "user address")
public class Address {
  @ApiObjectField(description = "uid or openid")
  String uid;
  
  @ApiObjectField(description = "default adress")
  int    def = Integer.MIN_VALUE;

  @ApiObjectField(description = "address 1")
  String address1;
  
  @ApiObjectField(description = "address 2")
  String address2;

  @ApiObjectField(description = "address 3")
  String address3;

  @ApiObjectField(description = "address 4")
  String address4;
  
  @ApiObjectField(description = "address 5")
  String address5;

  @ApiObjectField(description = "address 6")
  String address6;

  @ApiObjectField(description = "address 7")
  String address7;

  @ApiObjectField(description = "address 8")
  String address8;
  
  @ApiObjectField(description = "address 9")
  String address9;

  public void setUid(String uid) {
    this.uid = uid;
  }
  public String getUid() {
    return this.uid;
  }

  public void setDef(int def) {
    if (def < 1 || def > 9) def = 1;
    this.def = def;
  }
  public int getDef() {
    return this.def;
  }

  public String getDefAddress() {
    switch (def) {
    case 1: return address1;
    case 2: return address2;
    case 3: return address3;
    case 4: return address4;
    case 5: return address5;
    case 6: return address6;
    case 7: return address7;
    case 8: return address8;
    case 9: return address9;
    default: return null;
    }
  }

  public void setAddress1(String address1) {
    this.address1 = address1;
  }
  public String getAddress1() {
    return this.address1;
  }

  public void setAddress2(String address2) {
    this.address2 = address2;
  }
  public String getAddress2() {
    return this.address2;
  }

  public void setAddress3(String address3) {
    this.address3 = address3;
  }
  public String getAddress3() {
    return this.address3;
  }

  public void setAddress4(String address4) {
    this.address4 = address4;
  }
  public String getAddress4() {
    return this.address4;
  }

  public void setAddress5(String address5) {
    this.address5 = address5;
  }
  public String getAddress5() {
    return this.address5;
  }

  public void setAddress6(String address6) {
    this.address6 = address6;
  }
  public String getAddress6() {
    return this.address6;
  }

  public void setAddress7(String address7) {
    this.address7 = address7;
  }
  public String getAddress7() {
    return this.address7;
  }

  public void setAddress8(String address8) {
    this.address8 = address8;
  }
  public String getAddress8() {
    return this.address8;
  }

  public void setAddress9(String address9) {
    this.address9 = address9;
  }
  public String getAddress9() {
    return this.address9;
  }

  public void makeXssSafe() {
    if (address1 != null) address1 = XssHelper.makeJsonSafe(address1);
    if (address2 != null) address2 = XssHelper.makeJsonSafe(address2);
    if (address3 != null) address3 = XssHelper.makeJsonSafe(address3);
    if (address4 != null) address4 = XssHelper.makeJsonSafe(address4);
    if (address5 != null) address5 = XssHelper.makeJsonSafe(address5);
    if (address6 != null) address6 = XssHelper.makeJsonSafe(address6);
    if (address7 != null) address7 = XssHelper.makeJsonSafe(address7);
    if (address8 != null) address8 = XssHelper.makeJsonSafe(address8);
    if (address9 != null) address9 = XssHelper.makeJsonSafe(address9);        
  }
}
