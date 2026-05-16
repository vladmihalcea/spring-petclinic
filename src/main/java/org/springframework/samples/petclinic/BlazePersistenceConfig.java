package org.springframework.samples.petclinic;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.integration.view.spring.EnableEntityViews;
import com.blazebit.persistence.spring.data.repository.config.EnableBlazeRepositories;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Blaze Persistence configuration. Registers the {@link CriteriaBuilderFactory} and
 * {@link EntityViewManager} Spring beans and enables scanning for entity-view
 * types and Blaze-enhanced Spring Data repositories.
 *
 * @author Vlad Mihalcea
 */
@Configuration
@EnableEntityViews(basePackages = "org.springframework.samples.petclinic.owner")
@EnableBlazeRepositories(basePackages = "org.springframework.samples.petclinic")
public class BlazePersistenceConfig {

	@Bean
	public CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory emf) {
		CriteriaBuilderConfiguration config = Criteria.getDefault();
		return config.createCriteriaBuilderFactory(emf);
	}

	@Bean
	public EntityViewManager entityViewManager(CriteriaBuilderFactory cbf,
			EntityViewConfiguration entityViewConfiguration) {
		return entityViewConfiguration.createEntityViewManager(cbf);
	}

}
