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
package org.springframework.samples.petclinic.owner;

import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.view.OwnerDetailsView;
import org.springframework.samples.petclinic.owner.view.OwnerListView;
import org.springframework.stereotype.Repository;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;

@Repository
class OwnerViewRepository {

	@PersistenceContext
	private EntityManager entityManager;

	private final CriteriaBuilderFactory criteriaBuilderFactory;

	private final EntityViewManager entityViewManager;

	OwnerViewRepository(CriteriaBuilderFactory criteriaBuilderFactory, EntityViewManager entityViewManager) {
		this.criteriaBuilderFactory = criteriaBuilderFactory;
		this.entityViewManager = entityViewManager;
	}

	Optional<OwnerDetailsView> findOwnerDetailsById(Integer ownerId) {
		var ownerById = this.criteriaBuilderFactory.create(this.entityManager, Owner.class)
			.where(Owner_.ID)
			.eq(ownerId);
		return this.entityViewManager.applySetting(EntityViewSetting.create(OwnerDetailsView.class), ownerById)
			.getResultStream()
			.findFirst();
	}

	Page<OwnerListView> findByLastNameStartingWith(String lastName, Pageable pageable) {
		var cb = this.criteriaBuilderFactory.create(this.entityManager, Owner.class)
			.orderBy(Owner_.ID, true)
			.where(Owner_.LAST_NAME)
			.like()
			.value(lastName + "%")
			.noEscape();

		var countQuery = cb.getCountQuery();
		long total = (Long) countQuery.getSingleResult();

		var query = this.entityViewManager.applySetting(
				EntityViewSetting.create(OwnerListView.class, pageable.getOffset(), pageable.getPageSize()), cb);

		return new PageImpl<>(query.getResultList(), pageable, total);
	}

}
