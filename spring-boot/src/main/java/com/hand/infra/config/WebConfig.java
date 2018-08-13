package com.hand.infra.config;

import com.hand.api.interceptor.TestInterceptor;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.BeanNameViewResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer
{
    @Bean
    public FilterRegistrationBean<HttpPutFormContentFilter> FilterRegistration() {
        FilterRegistrationBean<HttpPutFormContentFilter> registration = new FilterRegistrationBean<HttpPutFormContentFilter>();
        registration.setFilter(new HttpPutFormContentFilter());//添加过滤器
        registration.addUrlPatterns("/*");//设置过滤路径，/*所有路径
        registration.setName("HttpPutFormContentFilter");//设置优先级
        registration.setOrder(2);//设置优先级
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TestInterceptor());
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        BeanNameViewResolver beanNameViewResolver = new BeanNameViewResolver();
        beanNameViewResolver.setOrder(100);
        registry.viewResolver(beanNameViewResolver);
    }
}
