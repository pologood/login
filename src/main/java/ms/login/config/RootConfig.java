package ms.login.config;

import java.util.Arrays;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPool;
import commons.saas.*;
import commons.spring.*;

@Configuration
@ComponentScan({ProjectInfo.PKG_PREFIX + ".api", ProjectInfo.PKG_PREFIX + ".manager"})
@PropertySource("classpath:application-default.properties")
@PropertySource("classpath:application-${spring.profiles.active}.properties")
public class RootConfig {
  @Autowired Environment env;

  @Bean
  public JedisPool jedisPool() {
    return new JedisPool(
      env.getRequiredProperty("redis.url"),
      env.getRequiredProperty("redis.port", Integer.class));
  }

  @Bean
  public PatchcaService patchcaService() {
    return new PatchcaService(jedisPool());
  }

  @Bean
  public LoggerFilter loggerFilter() {
    return new LoggerFilter(env);
  }

  @Bean
  public XssFilter xssFilter() {
    return new XssFilter(env);
  }

  @Bean
  public SmsService smsService() {
    return new QCloudSmsService(
      restTemplate(),
      env.getRequiredProperty("sms.appid"),
      env.getRequiredProperty("sms.appkey"),
      jedisPool()
      );
  }

  @Bean
  public RestNameService restNameService() {
    return new RestNameService(env);
  }

  @Bean
  public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory =
      new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(Integer.parseInt(env.getProperty("rest.timeout.connect", "1000")));
    factory.setReadTimeout(Integer.parseInt(env.getProperty("rest.timeout.read", "10000")));
    
    RestTemplate rest = new RestTemplate(factory);
    rest.setInterceptors(Arrays.asList(new RestTemplateFilter()));
    rest.getMessageConverters().add(new LooseGsonHttpMessageConverter());

    return rest;
  }

  @Bean
  public RedisRememberMeService rememberMeServices() {
    return new RedisRememberMeService(
      jedisPool(), env.getProperty("rest.tokenpool", ""),
      env.getRequiredProperty("web.host"),
      86400 * 7);
  }

  @Bean
  public LoginServiceProvider loginServiceProvider() {
    XiaopLoginService xiaop = new XiaopLoginService(restTemplate(), jedisPool());

    LoginServiceProvider provider = new LoginServiceProvider();
    provider.register(LoginServiceProvider.Name.XiaoP, xiaop);

    if (env.getProperty("login.wx.appid") != null) {
      WxLoginService wx = new WxLoginService(
        restTemplate(), jedisPool(),
        env.getRequiredProperty("login.wx.appid"), env.getRequiredProperty("login.wx.secret"));
      provider.register(LoginServiceProvider.Name.WeiXin, wx);
    }
    
    return provider;
  }
}
