package com.mail.springbootimaplistener.config;

import com.mail.springbootimaplistener.repository.ConfigRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

@Component("applicationConfigurations")
public class ApplicationConfigurations implements EnvironmentAware {

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void setEnvironment(Environment environment) {
        ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;

        Map<String, Object> propertySource = new HashMap<>();
        configRepository.findAll().stream().forEach(config -> propertySource.put(config.getConfigKey(), config.getConfigValue()));
        configurableEnvironment.getPropertySources().addAfter("systemEnvironment", new MapPropertySource("app-config", propertySource));
    }

}
