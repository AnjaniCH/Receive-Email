package com.mail.springbootimaplistener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SpringBootImapListenerApplication {
    
    @Bean
	public RestTemplate myRestTemplate(){
		return new RestTemplate();
	}
 
    public static void main(String[] args) {
        SpringApplication.run(SpringBootImapListenerApplication.class, args);
    }

}
