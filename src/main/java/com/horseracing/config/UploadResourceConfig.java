package com.horseracing.config;

import com.horseracing.service.UploadService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    private final UploadService uploadService;

    public UploadResourceConfig(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadService.getEvidenceDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/evidence/**")
                .addResourceLocations(location);
    }
}
