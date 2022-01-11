package com.mail.springbootimaplistener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SpringBootImapListenerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
            SpringApplication.run(SpringBootImapListenerApplication.class, args);
    }

}
