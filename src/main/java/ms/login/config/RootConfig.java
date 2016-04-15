package ms.login.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import redis.clients.jedis.JedisPool;
import commons.saas.*;
import commons.spring.*;

@Configuration
@ComponentScan({ProjectInfo.PKG_PREFIX + ".api", ProjectInfo.PKG_PREFIX + ".manager"})
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
  public SmsService smsService() {
    return new QCloudSmsService(
      env.getRequiredProperty("sms.appid"),
      env.getRequiredProperty("sms.appkey"),
      jedisPool()
      );
  }

  @Bean
  public RedisRememberMeService rememberMeServices() {
    return new RedisRememberMeService(
      jedisPool(),
      env.getRequiredProperty("web.host"),
      86400 * 7);
  }
}
