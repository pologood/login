package commons.spring;

import java.io.*;
import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.web.authentication.RememberMeServices;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import commons.utils.StringHelper;

public class RedisRememberMeService implements RememberMeServices {
  public static class User {
    private Optional<Long>   uid;
    private Optional<String> openId;
    private int              incId; 
    private List<Long>       permIds;

    public User(long uid) {
      this(uid, Integer.MIN_VALUE, null);
    }

    public User(long uid, int incId, List<Long> permIds) {
      this.uid     = Optional.of(uid);
      this.incId   = incId;
      this.permIds = permIds;
    }

    public long getUid() {
      return uid.get();
    }

    public String getId() {
      return uid.isPresent() ? String.valueOf(uid.get()) : openId.get();
    }

    public int getIncId() {
      return this.incId;
    }

    public long getPerm() {
      if (permIds == null) return Long.MAX_VALUE;
      else return permIds.get(0);
    }

    public List<Long> getPerms() {
      return permIds;
    }
  }
  
  private JedisPool jedisPool;
  private String    domain;
  private int       maxAge;
  private static final String KEY_PREFIX   = "RedisRMS_";
  private static final int    PARTS_NUMBER = 5;

  public RedisRememberMeService(JedisPool jedisPool, String domain, int maxAge) {
    this.jedisPool = jedisPool;
    this.domain = domain;
    this.maxAge = maxAge;
  }

  private String permsToString(List<Long> perms) {
    if (perms == null) return "";

    int i = 0;
    StringBuilder builder = new StringBuilder();
    for (Long perm : perms) {
      if (i != 0) builder.append(",");
      builder.append(String.valueOf(perm));
      i++;
    }
    return builder.toString();
  }

  private List<Long> stringToPerms(String perms) {
    if (perms.isEmpty()) return null;
    
    List<Long> permIds = new ArrayList<>();
    for (String part : perms.split(",")) {
      permIds.add(Long.valueOf(part));
    }
    return permIds;      
  }

  private String currentTime() {
    return Long.toString(System.currentTimeMillis()/1000);
  }

  private Cookie newCookie(String key, String value, int maxAge, boolean httpOnly) {
    Cookie cookie = new Cookie(key, value);
    cookie.setPath("/");
    cookie.setDomain(domain);
    cookie.setMaxAge(maxAge);
    if (httpOnly) cookie.setHttpOnly(true);
    return cookie;
  }    

  private String cacheKey(long uid) {
    return KEY_PREFIX + String.valueOf(uid);
  }

  private String cacheKey(String uid) {
    return KEY_PREFIX + uid;
  }
  
  public String login(HttpServletResponse response, User user) {
    String uid  = String.valueOf(user.getUid());
    String incId = user.getIncId() == Integer.MIN_VALUE ? "" : String.valueOf(user.getIncId());
    String perms = permsToString(user.getPerms());

    String token = String.join(":", uid, StringHelper.random(32));

    try (Jedis c = jedisPool.getResource()) {
      c.setex(cacheKey(uid), maxAge,
              String.join(":", token, currentTime(), incId, perms));
    }

    if (response != null) {
      response.addCookie(newCookie("uid", uid, maxAge, false));
      response.addCookie(newCookie("token", token, maxAge, true));
    }

    return token;    
  }

  public void logout(long uid, HttpServletResponse response) {
    try (Jedis c = jedisPool.getResource()) {
      c.del(cacheKey(uid));
    }
    
    if (response != null) {
      response.addCookie(newCookie("uid", null, 0, false));
      response.addCookie(newCookie("token", null, 0, true));
    }
  }

  public boolean update(User user) {
    String incId = user.getIncId() == Integer.MIN_VALUE ? "" : String.valueOf(user.getIncId());
    String perms = permsToString(user.getPerms());
    
    String token = null;
    try (Jedis c = jedisPool.getResource()) {
      String key = cacheKey(user.getUid());
      token = c.get(key);
      
      if (token == null) return false;
      String parts[] = token.split(":", PARTS_NUMBER);
      if (parts.length != PARTS_NUMBER) return false;
      
      c.setex(key, maxAge,
              String.join(":", parts[0], parts[1], currentTime(), incId, perms));
    }
    return true;
  }

  private User checkToken(String token) {
    if (token == null) return null;
    
    String parts[] = token.split(":", 2);
    if (parts.length != 2) return null;

    String key = cacheKey(parts[0]);

    String savedToken = null;
    try (Jedis c = jedisPool.getResource()) {
      savedToken = c.get(key);
    }
    
    if (savedToken == null) return null;
    String savedParts[] = savedToken.split(":", PARTS_NUMBER);
    if (savedParts.length != PARTS_NUMBER) return null;
    
    if (!savedParts[0].equals(parts[0]) || !savedParts[1].equals(parts[1])) {
      return null;
    }
    
    long createAt = Long.valueOf(savedParts[2]);
    long now = System.currentTimeMillis()/1000;
    if (createAt + maxAge/2 < now) {
      try (Jedis c = jedisPool.getResource()) {
        c.setex(key, maxAge,
                String.join(":", token, Long.toString(now), savedParts[3], savedParts[4]));
      }
    }
    
    int incId = savedParts[3].isEmpty()? Integer.MIN_VALUE : Integer.valueOf(savedParts[3]);
    return new User(Long.valueOf(parts[0]), incId, stringToPerms(savedParts[4]));
  }

  @Override
  public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
    String token = null;
    
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("token")) {
          token = cookie.getValue();
          break;
        }
      }
    }

    if (token == null) {
      token = request.getHeader("authorization");
    }

    User user = checkToken(token);
    if (user == null) return null;

    List<GrantedAuthority> grantedAuths = new ArrayList<>();
    if (user.getPerms() != null) {
      for (Long perm : user.getPerms()) {
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_PERM" + String.valueOf(perm)));
      }
    }

    return new RememberMeAuthenticationToken("N/A", user, grantedAuths);
  }

  @Override
  public void loginFail(HttpServletRequest request, HttpServletResponse response) {
    System.out.println("loginFaile");
  }

  @Override
  public void loginSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication auth) {
    System.out.println("loginSuccess");
  }
}
