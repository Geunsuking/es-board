package esboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // s가 붙은 addResourceHandlers (복수형) 메서드를 오버라이드해야 합니다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/uploads/");
    }
}