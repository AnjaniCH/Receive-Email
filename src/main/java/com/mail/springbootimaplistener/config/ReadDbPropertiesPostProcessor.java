package com.mail.springbootimaplistener.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class ReadDbPropertiesPostProcessor implements EnvironmentPostProcessor {

    /**
     * Name of the custom property source added by this post processor class
     */
    private static final String PROPERTY_SOURCE_NAME = "databaseProperties";

    private String[] KEYS = {
        "mail.imap.host",
        "mail.imap.port",
        "mail.imap.username",
        "mail.imap.password",
    };

    /**
     * Adds Spring Environment custom logic. This custom logic fetch properties
     * from database and setting highest precedence
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        Map<String, Object> propertySource = new HashMap<>();

        try {

            // Build manually datasource to ServiceConfig
            DataSource ds = (DataSource) DataSourceBuilder
                    .create()
                    .username(environment.getProperty("spring.datasource.username"))
                    .password(environment.getProperty("spring.mail.password"))
                    .url(environment.getProperty("spring.datasource.url"))
                    .driverClassName("com.mysql.jdbc.Driver")
                    .build();

            // Fetch all properties
            Connection connection = ds.getConnection();

            //JTrace.genLog(LogSeverity.informational, "cargando configuracion de la base de datos");

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT config FROM config WHERE id = ?");

            for (int i = 0; i < KEYS.length; i++) {

                String key = KEYS[i];

                preparedStatement.setString(1, key);

                ResultSet rs = preparedStatement.executeQuery();

                // Populate all properties into the property source
                while (rs.next()) {
                    propertySource.put(key, rs.getString("config"));
                }

                rs.close();
                preparedStatement.clearParameters();

            }

            preparedStatement.close();
            connection.close();

            // Create a custom property source with the highest precedence and add it to Spring Environment
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, propertySource));

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    
} // class ReadDbPropertiesPostProcessor end
