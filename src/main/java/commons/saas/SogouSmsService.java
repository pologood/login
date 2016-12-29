package commons.saas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPool;

public class SogouSmsService extends SmsService {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SendSmsRespBody {
    public int    code;
    public String desc;
  }

  private RestTemplate rest;
  private String uri;

  public SogouSmsService(RestTemplate rest, String appid, JedisPool jedisPool) {
    super(jedisPool);
    this.rest = rest;
    this.uri = "http://sms.sogou/portal/mobile/smsproxy.php?" +
      "number={number}&desc={desc}&type=json&appid=" + appid;
  }

  protected void sendInternal(String phone, String msg) {
    SendSmsRespBody resp = rest.getForObject(uri, SendSmsRespBody.class, phone, msg);
    if (resp.code != 0) throw new SmsException(resp.desc);
  }
}
