package com.agrosentinel.agrosentinel;

import org.springframework.core.env.Profiles;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.agrosentinel.agrosentinel.config.DemoDataSeeder;

@SpringBootApplication
public class AgrosentinelApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AgrosentinelApplication.class);
		application.addInitializers(context -> {
			var environment = context.getEnvironment();
			if (environment.acceptsProfiles(Profiles.of("demo-data"))
					&& environment.getProperty("agrosentinel.demo-data.enabled", Boolean.class, false)) {
				DemoDataSeeder.requireAllowedDatasource(
						environment.getProperty("spring.datasource.url"),
						environment.getProperty("agrosentinel.demo-data.allow-remote", Boolean.class, false),
						environment.getProperty("agrosentinel.demo-data.allowed-remote-host"),
						environment.getProperty("agrosentinel.demo-data.allowed-remote-database"));
				DemoDataSeeder.requireRemotePasswordKeyIfNeeded(
						environment.getProperty("spring.datasource.url"),
						environment.getProperty("agrosentinel.demo-data.password-key"));
			}
		});
		application.run(args);
	}

}
