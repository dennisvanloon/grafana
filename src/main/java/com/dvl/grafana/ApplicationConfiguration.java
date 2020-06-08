package com.dvl.grafana;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public PropertiesConfiguration propertiesConfiguration(@Value("${spring.config.location}") String path) throws Exception {
        String filePath = new File(path).getCanonicalPath();
        PropertiesConfiguration configuration = new PropertiesConfiguration(new File(filePath));
        FileChangedReloadingStrategy fileChangedReloadingStrategy = new FileChangedReloadingStrategy();
        fileChangedReloadingStrategy.setRefreshDelay(2000);
        configuration.setReloadingStrategy(fileChangedReloadingStrategy);
        return configuration;
    }
}
