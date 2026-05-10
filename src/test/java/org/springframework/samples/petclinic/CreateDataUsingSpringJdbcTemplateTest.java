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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Integration test that populates every table through plain
 * {@link JdbcTemplate} batch operations, without JPA and Hibernate.
 *
 * @author Vlad Mihalcea
 */
@SpringJUnitConfig(CreateDataUsingSpringTest.PetClinicConfig.class)
@TestPropertySource("/application-postgres.properties")
class CreateDataUsingSpringJdbcTemplateTest extends AbstractCreateDataTest {

	public CreateDataUsingSpringJdbcTemplateTest(
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
	void testInsertData() {
		transactionTemplate.execute(status -> {
			insertData();
			return null;
		});
	}

	// -------------------------------------------------------------------------
	// Per-table insert helpers
	// -------------------------------------------------------------------------

	@Override
	protected List<Specialty> insertSpecialties() {
		LOGGER.info("Inserting {} specialties", formatNumber(SPECIALTY_COUNT));

		List<Object[]> args = IntStream.range(0, SPECIALTY_COUNT)
			.mapToObj(i -> new Object[]{String.format("Specialty %d", i)})
			.toList();

		jdbcBatchInsert("INSERT INTO specialties (name) VALUES (?)", args);

		return IntStream.range(0, SPECIALTY_COUNT).mapToObj(i -> {
			Specialty s = new Specialty();
			s.setId(i + 1);
			s.setName(String.format("Specialty %d", i));
			return s;
		}).toList();
	}

	@Override
	protected void insertVets(List<Specialty> specialties) {
		LOGGER.info("Inserting {} vets", formatNumber(VET_COUNT));

		List<Object[]> vetArgs = IntStream.range(0, VET_COUNT)
			.mapToObj(i -> new Object[]{
				String.format("Vet %d - first name", i),
				String.format("Vet %d - last name", i)
			})
			.toList();

		jdbcBatchInsert("INSERT INTO vets (first_name, last_name) VALUES (?, ?)", vetArgs);

		List<Object[]> vsArgs = new ArrayList<>(VET_COUNT * SPECIALTIES_PER_VET);
		for (int i = 0; i < VET_COUNT; i++) {
			int vetId = i + 1;
			Set<Integer> picked = new HashSet<>();
			while (picked.size() < SPECIALTIES_PER_VET) {
				picked.add(specialties.get(RANDOM.nextInt(specialties.size())).getId());
			}
			for (int specialtyId : picked) {
				vsArgs.add(new Object[]{vetId, specialtyId});
			}
		}

		jdbcBatchInsert("INSERT INTO vet_specialties (vet_id, specialty_id) VALUES (?, ?)", vsArgs);
	}

	@Override
	protected List<PetType> insertTypes() {
		LOGGER.info("Inserting {} pet types", formatNumber(TYPE_COUNT));

		List<Object[]> args = IntStream.range(0, TYPE_COUNT)
			.mapToObj(i -> new Object[]{String.format("Type %d", i)})
			.toList();

		jdbcBatchInsert("INSERT INTO types (name) VALUES (?)", args);

		return IntStream.range(0, TYPE_COUNT).mapToObj(i -> {
			PetType t = new PetType();
			t.setId(i + 1);
			t.setName(String.format("Type %d", i));
			return t;
		}).toList();
	}

	@Override
	protected void insertOwners(List<PetType> petTypes) {
		LOGGER.info("Inserting {} owners", formatNumber(OWNER_COUNT));

		List<Object[]> ownerArgs = IntStream.range(0, OWNER_COUNT)
			.mapToObj(i -> new Object[]{
				String.format("Owner %d - first name", i),
				String.format("Owner %d - last name", i),
				String.format("Owner %d - address", i),
				String.format("Owner %d - city", i),
				String.format("%010d", i)
			})
			.toList();

		jdbcBatchInsert(
			"INSERT INTO owners (first_name, last_name, address, city, telephone) VALUES (?, ?, ?, ?, ?)",
			ownerArgs
		);

		int totalPets = OWNER_COUNT * PETS_PER_OWNER_COUNT;
		LOGGER.info("Inserting {} pets", formatNumber(totalPets));

		List<Object[]> petArgs = new ArrayList<>(totalPets);
		for (int ownerIdx = 0; ownerIdx < OWNER_COUNT; ownerIdx++) {
			int ownerId = ownerIdx + 1;
			for (int p = 0; p < PETS_PER_OWNER_COUNT; p++) {
				petArgs.add(new Object[]{
					String.format("Pet %d - name", p),
					LocalDate.now(),
					petTypes.get(RANDOM.nextInt(petTypes.size())).getId(),
					ownerId
				});
			}
		}

		jdbcBatchInsert(
			"INSERT INTO pets (name, birth_date, type_id, owner_id) VALUES (?, ?, ?, ?)",
			petArgs
		);

		int totalVisits = totalPets * VISITS_PER_PET_COUNT;
		LOGGER.info("Inserting {} visits", formatNumber(totalVisits));

		List<Object[]> visitArgs = new ArrayList<>(totalVisits);
		for (int petId = 1; petId <= totalPets; petId++) {
			for (int v = 0; v < VISITS_PER_PET_COUNT; v++) {
				visitArgs.add(new Object[]{
					petId,
					LocalDate.now(),
					String.format("Visit %d - description", v)
				});
			}
		}

		jdbcBatchInsert(
			"INSERT INTO visits (pet_id, visit_date, description) VALUES (?, ?, ?)",
			visitArgs
		);
	}

	// -------------------------------------------------------------------------
	// Infrastructure helpers
	// -------------------------------------------------------------------------

	private void jdbcBatchInsert(String sql, List<Object[]> args) {
		jdbcTemplate.batchUpdate(sql, args);
	}
}

