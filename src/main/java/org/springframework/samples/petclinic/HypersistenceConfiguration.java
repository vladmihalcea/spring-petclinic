package org.springframework.samples.petclinic;

import io.hypersistence.optimizer.HypersistenceOptimizer;
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
			.setRuntimeScannerEnabled(false)
		);
	}
}
