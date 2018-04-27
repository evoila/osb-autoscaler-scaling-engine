package de.evoila;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import de.evoila.cf.autoscaler.engine.model.CfBean;
import de.evoila.cf.autoscaler.engine.model.EnginePlatform;
import de.evoila.cf.autoscaler.engine.model.ScalerBean;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableConfigurationProperties({CfBean.class, EnginePlatform.class, ScalerBean.class})
public class Application {

	public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        Assert.notNull(ctx, "Could not properly start the application.");
	}
}
