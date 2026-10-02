package com.agrosentinel.agrosentinel.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FailureEvidenceWebConfig implements WebMvcConfigurer {
    private final String storageLocation;
    private final String maintenanceStorageLocation;

    public FailureEvidenceWebConfig(
            @Value("${agrosentinel.failure-evidence.location}") String storageLocation,
            @Value("${agrosentinel.maintenance-evidence.location}") String maintenanceStorageLocation) {
        this.storageLocation = Path.of(storageLocation).toAbsolutePath().normalize().toUri().toString();
        this.maintenanceStorageLocation = Path.of(maintenanceStorageLocation).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/failure-evidence/**")
                .addResourceLocations(storageLocation.endsWith("/") ? storageLocation : storageLocation + "/");
        registry.addResourceHandler("/uploads/maintenance-evidence/**")
                .addResourceLocations(maintenanceStorageLocation.endsWith("/")
                        ? maintenanceStorageLocation
                        : maintenanceStorageLocation + "/");
    }
}
