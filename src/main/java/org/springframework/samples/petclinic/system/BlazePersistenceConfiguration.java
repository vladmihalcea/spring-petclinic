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
package org.springframework.samples.petclinic.system;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.samples.petclinic.owner.view.*;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViews;

@Configuration(proxyBeanMethods = false)
class BlazePersistenceConfiguration {

	@Bean
	CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory entityManagerFactory) {
		CriteriaBuilderConfiguration config = Criteria.getDefault();
		return config.createCriteriaBuilderFactory(entityManagerFactory);
	}

	@Bean
	EntityViewManager entityViewManager(CriteriaBuilderFactory criteriaBuilderFactory) {
		EntityViewConfiguration config = EntityViews.createDefaultConfiguration();
		config.addEntityView(OwnerDetailsView.class);
		config.addEntityView(OwnerListView.class);
		config.addEntityView(OwnerPetView.class);
		config.addEntityView(OwnerPetNameView.class);
		config.addEntityView(OwnerVisitView.class);
		return config.createEntityViewManager(criteriaBuilderFactory);
	}

}
