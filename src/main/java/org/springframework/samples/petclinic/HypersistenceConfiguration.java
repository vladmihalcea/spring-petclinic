package org.springframework.samples.petclinic;

import java.util.concurrent.TimeUnit;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.Config;
import io.hypersistence.optimizer.core.config.JpaConfig;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/***
 * @author Vlad Mihalcea
 */
@Configuration
public class HypersistenceConfiguration {

	@Bean
	public HypersistenceOptimizer hypersistenceOptimizer(
			EntityManagerFactory entityManagerFactory) {

		return new HypersistenceOptimizer(
			new JpaConfig(
				entityManagerFactory
			)
			.setEventPersistence(
				new Config.EventPersistence()
					.setApplicationName("spring-petclinic")
					.setRuntimeRollupWindowMillis(TimeUnit.SECONDS.toMillis(5))
					.setWebAppEnabled(true)
					.setWebAppPort(8088)
			)
		);
	}
}
