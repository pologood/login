package commons.saas;

public class LoginServiceProvider {
  public static enum Name {
    XiaoP, WeiXin
  }

  private LoginService xiaop;
  private LoginService weixin;

  public LoginService get(Name name) {
    switch (name) {
    case XiaoP: return xiaop;      
    case WeiXin: return weixin;
    default: return null;
    }
  }

  public static Name getProvider(String openId) {
    if (openId.startsWith("xiaop_")) return Name.XiaoP;
    else if (openId.startsWith("weixin_")) return Name.WeiXin;
    return null;    
  }

  public LoginService get(String openId) {
    Name name = getProvider(openId);
    return get(name);
  }

  public void register(Name name, LoginService service) {
    switch (name) {
    case XiaoP: xiaop = service;
    case WeiXin: weixin = service;
    }
  }
}
