package ms.login.entity;

import java.time.LocalDateTime;
import org.jsondoc.core.annotation.*;

@ApiObject(name = "OpenAccount", description = "Account based OpenId")
public class OpenAccount {
  @ApiObjectField(description = "openid")
  String   id;

  String accessToken;
  LocalDateTime accessTokenExpireAt;

  String refreshToken;
  LocalDateTime refreshTokenExpireAt;

  @ApiObjectField(description = "nickname")
  String name;

  @ApiObjectField(description = "headimg")
  String headImg;
  
  public void setId(String id) {
    this.id = id;
  }
  public String getId() {
    return this.id;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
  public String getAccessToken() {
    return this.accessToken;
  }

  public void setAccessTokenExpireAt(LocalDateTime dt) {
    this.accessTokenExpireAt = dt;
  }
  public LocalDateTime getAccessTokenExpireAt() {
    return this.accessTokenExpireAt;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
  public String getRefreshToken() {
    return this.refreshToken;
  }

  public void setRefreshTokenExpireAt(LocalDateTime dt) {
    this.refreshTokenExpireAt = dt;
  }
  public LocalDateTime getRefreshTokenExpireAt() {
    return this.refreshTokenExpireAt;
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
}
