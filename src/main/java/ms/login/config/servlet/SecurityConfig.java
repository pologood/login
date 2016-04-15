package ms.login.config.servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.web.authentication.RememberMeServices;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  @Autowired RememberMeServices rms;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and()
      .authorizeRequests()
      .antMatchers(HttpMethod.GET, "/jsondoc").permitAll()
      .antMatchers(HttpMethod.GET, "/api/idcode").permitAll()
      .antMatchers(HttpMethod.GET, "/api/regcode").permitAll()
      .antMatchers(HttpMethod.GET, "/api/login").permitAll()
      .antMatchers(HttpMethod.POST, "/api/account").permitAll()
      .antMatchers(HttpMethod.PUT, "/api/password").permitAll()
      .anyRequest().authenticated()
      .and()
      .rememberMe()
      .rememberMeServices(rms);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    RememberMeAuthenticationProvider p = new RememberMeAuthenticationProvider("N/A");
    auth.authenticationProvider(p);
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }
}
