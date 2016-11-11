package ms.login.config.servlet;

import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ms.login.config.*;

@Configuration
@EnableWebMvc
@ComponentScan({ProjectInfo.API_PKG})
public class WebConfig extends WebMvcConfigurerAdapter {
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
    stringConverter.setWriteAcceptCharset(false);
    converters.add(stringConverter);

    converters.add(new FormHttpMessageConverter());

    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    builder.serializationInclusion(JsonInclude.Include.NON_NULL);
    builder.serializerByType(LocalDateTime.class, new LocalDateTimeJsonSerializer());
    converters.add(new MappingJackson2HttpMessageConverter(builder.build()));
  }

  @Bean
  public MultipartResolver multipartResolver() throws IOException {
    return new StandardServletMultipartResolver();
  }  

  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    RequestMappingHandlerMapping r = new RequestMappingHandlerMapping();
    r.setRemoveSemicolonContent(false);
    return r;
  }
}
