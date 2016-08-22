package commons.saas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPool;

@JsonIgnoreProperties(ignoreUnknown = true)
class GetUserTokenResult {
  @JsonProperty("access_token")
  public String accessToken;
  @JsonProperty("expires_in")
  public int expiresIn = 7200;

  @JsonProperty("refresh_token")
  public String refreshToken;

  @JsonProperty("openid")
  public String openId;

  public String scope;

  @JsonProperty("errcode")
  public int code = Integer.MAX_VALUE;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class GetUserInfoResult {
  public String openid;
  public String nickname;
  public String sex;
  public String province;
  public String city;
  public String country;

  @JsonProperty("headimgurl")
  public String headImgUrl;

  @JsonProperty("errcode")
  public int code = Integer.MAX_VALUE;
}

public class WxLoginService extends LoginService {
  public static final String OAUTH_ACCESS_TOKEN_API = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={appid}&secret={secret}&code={code}&grant_type=authorization_code";
  public static final String SNS_USERINFO_API       = "https://api.weixin.qq.com/sns/userinfo?access_token={access_token}&openid={openid}&lang=zh_CN";

  RestTemplate rest;
  String appid;
  String secret;
    
  public WxLoginService(RestTemplate rest, JedisPool jedisPool, String appid, String secret) {
    super(jedisPool);
    this.rest = rest;
    this.appid = appid;
    this.secret = secret;
  }

  protected User doLogin(String tmpToken) {
    GetUserTokenResult tokenResult = rest.getForObject(
      OAUTH_ACCESS_TOKEN_API, GetUserTokenResult.class, appid, secret, tmpToken);

    if (tokenResult == null || tokenResult.code != 0) return null;

    GetUserInfoResult infoResult = rest.getForObject(
      SNS_USERINFO_API, GetUserInfoResult.class, tokenResult.accessToken, tokenResult.openId);

    if (infoResult == null || infoResult.code != 0) return null;
    
    LoginService.User user = new LoginService.User();
    user.setOpenId("weixin_" + infoResult.openid);
    user.setName(infoResult.nickname);
    user.setHeadImg(infoResult.headImgUrl);

    return user;
  }
}
