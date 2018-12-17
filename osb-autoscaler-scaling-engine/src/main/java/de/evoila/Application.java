package de.evoila;

import de.evoila.cf.autoscaler.engine.properties.CloudFoundryConfigurationBean;
import de.evoila.cf.autoscaler.engine.properties.EngineBean;
import de.evoila.cf.autoscaler.engine.properties.ScalerBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * @author Marius Berger
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableConfigurationProperties({ CloudFoundryConfigurationBean.class, EngineBean.class, ScalerBean.class })
public class Application  {

	public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        Assert.notNull(ctx, "Could not properly start the application.");
	}
}
