package org.springframework.samples.petclinic.system;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.Config;
import io.hypersistence.optimizer.core.config.JpaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class HypersistenceConfiguration {

    @Bean
    public HypersistenceOptimizer hypersistenceOptimizer(
            EntityManagerFactory entityManagerFactory) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(Config.Property.Session.TIMEOUT_MILLIS, 1000);
        properties.put(Config.Property.Session.FLUSH_TIMEOUT_MILLIS, 500);

        return new HypersistenceOptimizer(
            new JpaConfig(
                entityManagerFactory
            )
            .setProperties(properties)
        );
    }
}
