package de.evoila;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class Application {

	public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        Assert.notNull(ctx, "Could not properly start the application.");
	}
}
