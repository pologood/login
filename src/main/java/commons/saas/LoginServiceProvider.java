package commons.saas;

public class LoginServiceProvider {
  public static enum Name {
    XiaoP, WeiXin, XiaoPLocal,
  }

  private LoginService xiaop;
  private LoginService weixin;
  private LoginService xiaopl;

  public LoginService get(Name name) {
    switch (name) {
    case XiaoP: return xiaop;      
    case WeiXin: return weixin;
    case XiaoPLocal: return xiaopl;
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
    case XiaoPLocal: xiaopl = service;
    }
  }
}
