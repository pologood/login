package commons.saas;

import java.nio.file.*;
import java.util.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import redis.clients.jedis.JedisPool;
import commons.utils.JsonHelper;

public class XiaopLocalLoginService extends LoginService {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UserInfo {
    public String uid;
    public String name;
    public String uno;
  }

  private PublicKey publicKey;

  public XiaopLocalLoginService(String publicKeyFile, JedisPool jedisPool) {
    super(jedisPool);

    try {
      byte[] content = getPublicKeyByte(Files.readAllBytes(Paths.get(publicKeyFile)));

      byte[] keyBytes = Base64.getDecoder().decode(content);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      this.publicKey = keyFactory.generatePublic(spec);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] getPublicKeyByte(byte[] input) {
    int len = 0;
    boolean skip = false;
    for (int i = 0; i < input.length; ++i) {
      if (input[i] == '-') skip = true;
      else if (input[i] == '\n') skip = false;
      else if (!skip) len++;
    }

    skip = false;
    byte[] output = new byte[len];
    for (int i = 0, j = 0; i < input.length; ++i) {
      if (input[i] == '-') skip = true;
      else if (input[i] == '\n') skip = false;
      else if (!skip) output[j++] = input[i];
    }
    return output;
  }

  public String getName() {
    return "xiaop";
  }

  protected User doLogin(String encryptedData) {
    try {
      byte[] data = Base64.getDecoder().decode(encryptedData);

      Cipher decrypt = Cipher.getInstance("RSA");
      decrypt.init(Cipher.DECRYPT_MODE, publicKey);
      String json = new String(decrypt.doFinal(data), "UTF-8");
      UserInfo info = JsonHelper.readValue(json, UserInfo.class);

      LoginService.User user = new LoginService.User();
      user.setOpenId("xiaop_" + info.uid);
      user.setName(info.name);
      user.setHeadImg("https://puboa.sogou-inc.com/moa/sylla/mapi/portrait?uid=" + info.uid);

      if (info.uno.startsWith("OS")) {
        user.setId(9990000 + Integer.parseInt(info.uno.substring(2)));
      } else {
        user.setId(Integer.parseInt(info.uno));
      }

      return user;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
