package com.cms.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NetworkKeyProperties.class)
public class NetworkKeyConfig {
}
