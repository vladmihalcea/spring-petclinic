/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic;

import java.util.concurrent.TimeUnit;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.Config;
import io.hypersistence.optimizer.core.config.JpaConfig;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Hypersistence Optimizer with the Event Persistence feature enabled, so the
 * configuration, mapping, and runtime events it detects are stored in the same database
 * the application uses, and the embedded Event Persistence Web App is started on
 * http://localhost:8088/ to browse them.
 * <p>
 * The mapping and configuration scanners run asynchronously, in two background threads,
 * so they don't add any overhead to the application bootstrap.
 *
 * @author Vlad Mihalcea
 */
@Configuration
public class HypersistenceConfiguration {

	/**
	 * The {@link EntityManagerFactory} is resolved lazily through an
	 * {@link ObjectProvider}, so that web-layer test slices, which load this
	 * configuration without a JPA context, simply skip the Optimizer instead of failing
	 * to start.
	 */
	@Bean
	public HypersistenceOptimizer hypersistenceOptimizer(ObjectProvider<EntityManagerFactory> entityManagerFactory) {

		EntityManagerFactory emf = entityManagerFactory.getIfAvailable();
		if (emf == null) {
			return null;
		}

		return new HypersistenceOptimizer(new JpaConfig(emf)
			.setEventPersistence(new Config.EventPersistence().setApplicationName("spring-petclinic")
				.setRuntimeRollupWindowMillis(TimeUnit.SECONDS.toMillis(5))
				.setWebAppEnabled(true)
				.setWebAppPort(8088))
			.setAsyncMetadataScannersEnabled(true));
	}

}
