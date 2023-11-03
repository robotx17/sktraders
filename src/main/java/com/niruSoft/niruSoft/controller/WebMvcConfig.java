package com.niruSoft.niruSoft.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/ARIAL.ttf")
                .addResourceLocations("classpath:/static/");

        registry.addResourceHandler("/SKTRADERS.jpg")
                .addResourceLocations("classpath:/static/");
    }
}
