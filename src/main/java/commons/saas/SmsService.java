package commons.saas;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;

public abstract class SmsService {
  public static String CODE_PREFIX = "SmsService_";
  public static String CODE_SEND_PREFIX = "SmsServiceSend_";
  
  private JedisPool jedisPool;
  
  public SmsService(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  protected abstract void sendInternal(String phone, String msg) throws SmsException;
  
  public boolean send(String phone, String code, String msg) throws SmsException {
    String sendedCode;
    try (Jedis c = jedisPool.getResource()) {
      sendedCode = c.get(CODE_SEND_PREFIX + phone);
    }
    if (sendedCode != null) return false;

    sendInternal(phone, msg);

    try (Jedis c = jedisPool.getResource()) {
      c.setex(CODE_SEND_PREFIX + phone, 60, "");
      c.setex(CODE_PREFIX + phone, 5 * 60, code);
    }
    return true;
  };

  public String get(String phone) {
    String code;
    try (Jedis c = jedisPool.getResource()) {
      code = c.get(CODE_PREFIX + phone);
    }
    return code;
  }
}
