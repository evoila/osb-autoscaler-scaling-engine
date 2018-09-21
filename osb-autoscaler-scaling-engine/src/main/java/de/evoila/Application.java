package de.evoila;


import de.evoila.cf.autoscaler.engine.model.CfBean;
import de.evoila.cf.autoscaler.engine.model.EnginePlatform;
import de.evoila.cf.autoscaler.engine.model.ScalerBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableConfigurationProperties({CfBean.class, EnginePlatform.class, ScalerBean.class})
public class Application {

	public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.addListeners(new ApplicationPidFileWriter());
        ApplicationContext ctx = springApplication.run(args);

        Assert.notNull(ctx, "Could not properly start the application.");
	}
}
