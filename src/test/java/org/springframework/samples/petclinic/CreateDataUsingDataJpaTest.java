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

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.model.BaseEntity;
import org.springframework.samples.petclinic.owner.*;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Integration test to create data.
 *
 * @author Vlad Mihalcea
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("/application-postgres.properties")
class CreateDataUsingDataJpaTest extends AbstractCreateDataTest {

	public CreateDataUsingDataJpaTest(
			@Autowired OwnerRepository owners,
			@Autowired PetTypeRepository types,
			@Autowired VetRepository vets,
			@Autowired EntityManager entityManager,
			@Autowired TransactionTemplate transactionTemplate,
			@Autowired JdbcTemplate jdbcTemplate) {
		super(owners, types, vets, entityManager, transactionTemplate, jdbcTemplate);
	}

	// -------------------------------------------------------------------------
	// Main test
	// -------------------------------------------------------------------------

	@Test
	@Commit
	void testInsertData() {
		insertData();
	}

	@Override
	protected void syncWithDatabase() {
		entityManager.flush();
	}

	// -------------------------------------------------------------------------
	// Per-table insert helpers
	// -------------------------------------------------------------------------

	@Override
	protected List<Specialty> insertSpecialties() {
		LOGGER.info("Inserting {} specialties", formatNumber(SPECIALTY_COUNT));
		return insertBatch(
			i -> {
				Specialty specialty = new Specialty();
				specialty.setName(String.format("Specialty %d", i));
				return specialty;
			},
			SPECIALTY_COUNT
		);
	}

	@Override
	protected void insertVets(List<Specialty> specialties) {
		LOGGER.info("Inserting {} vets", formatNumber(VET_COUNT));

		insertBatch(i -> {
				Vet vet = new Vet();
				vet.setFirstName(String.format("Vet %d - first name", i));
				vet.setLastName(String.format("Vet %d - last name", i));
				while (vet.getSpecialties().size() < SPECIALTIES_PER_VET) {
					vet.addSpecialty(specialties.get(RANDOM.nextInt(specialties.size())));
				}
				return vet;
			},
			VET_COUNT
		);
	}

	@Override
	protected List<PetType> insertTypes() {
		LOGGER.info("Inserting {} pet types", formatNumber(TYPE_COUNT));
		return insertBatch(
			i -> {
				PetType type = new PetType();
				type.setName(String.format("Type %d", i));
				return type;
			},
			TYPE_COUNT
		);
	}

	@Override
	protected void insertOwners(List<PetType> petTypes) {
		LOGGER.info("Inserting {} owners", formatNumber(OWNER_COUNT));

		insertBatch(
			i -> {
				Owner owner = new Owner();
				owner.setFirstName(String.format("Owner %d - first name", i));
				owner.setLastName(String.format("Owner %d - last name", i));
				owner.setAddress(String.format("Owner %d - address", i));
				owner.setCity(String.format("Owner %d - city", i));
				owner.setTelephone(String.format("%010d", i));

				for (int j = 0; j < PETS_PER_OWNER_COUNT; j++) {
					Pet pet = new Pet();
					pet.setName(
						String.format("Pet %d - name ", ((i + 1) * PETS_PER_OWNER_COUNT) + (j + 1))
					);
					pet.setBirthDate(LocalDate.now().minusDays(i % 3650L));
					pet.setType(petTypes.get(RANDOM.nextInt(petTypes.size())));
					owner.addPet(pet);

					for (int k = 0; k < VISITS_PER_PET_COUNT; k++) {
						Visit visit = new Visit();
						visit.setDate(LocalDate.now().minusDays(i % 365L));
						visit.setDescription(
							String.format(
								"Visit %d - description ",
								(((i + 1) * PETS_PER_OWNER_COUNT) + (j + 1)) * VISITS_PER_PET_COUNT) + (k + 1)
						);
						pet.addVisit(visit);
					}
				}
				return owner;
			},
			OWNER_COUNT
		);
	}

	// -------------------------------------------------------------------------
	// Infrastructure helpers
	// -------------------------------------------------------------------------

	protected <T extends BaseEntity> void executeBatch(List<T> batch) {
		transactionTemplate.execute(status -> {
			for (T entity : batch) {
				if (entity.isNew()) {
					entityManager.persist(entity);
				}
			}
			return null;
		});
	}
}
