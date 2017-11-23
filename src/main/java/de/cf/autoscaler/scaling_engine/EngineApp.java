package de.cf.autoscaler.scaling_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class)
public class EngineApp {

	public static void main(String[] args) {
		
        ApplicationContext ctx = SpringApplication.run(EngineApp.class, args);
        Assert.notNull(ctx, "Could not properly start the application.");
	}
}
