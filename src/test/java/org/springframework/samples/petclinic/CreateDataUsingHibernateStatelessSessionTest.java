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
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.owner.*;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.VetSpeciality;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test that populates every table through a Hibernate
 * {@link StatelessSession}.
 *
 * @author Vlad Mihalcea
 */
@SpringJUnitConfig(CreateDataUsingSpringTest.PetClinicConfig.class)
@TestPropertySource("/application-postgres.properties")
class CreateDataUsingHibernateStatelessSessionTest extends AbstractCreateDataTest {

	private StatelessSession activeSession;

	public CreateDataUsingHibernateStatelessSessionTest(
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
		SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
		activeSession = sessionFactory.openStatelessSession();
		activeSession.beginTransaction();
		try {
			insertData();
		} catch (Exception e) {
			activeSession.getTransaction().rollback();
			fail(e.getMessage());

		} finally {
			activeSession.close();
		}
	}

	@Override
	protected void syncWithDatabase() {
		activeSession.getTransaction().commit();
	}

	@Override
	protected List<Specialty> insertSpecialties() {
		LOGGER.info("Inserting {} specialties", formatNumber(SPECIALTY_COUNT));
		List<Specialty> specialties = new ArrayList<>(SPECIALTY_COUNT);
		for (int i = 0; i < SPECIALTY_COUNT; i++) {
			Specialty specialty = new Specialty();
			specialty.setName(String.format("Specialty %d", i));
			specialties.add(specialty);
		}
		activeSession.insertMultiple(specialties);
		return specialties;
	}

	@Override
	protected void insertVets(List<Specialty> specialties) {
		LOGGER.info("Inserting {} vets", formatNumber(VET_COUNT));
		List<Vet> vets = new ArrayList<>(VET_COUNT);
		for (int i = 0; i < VET_COUNT; i++) {
			Vet vet = new Vet();
			vet.setFirstName(String.format("Vet %d - first name", i));
			vet.setLastName(String.format("Vet %d - last name", i));

			vets.add(vet);
		}
		LOGGER.info("Inserting {} vets", formatNumber(vets.size()));
		activeSession.insertMultiple(vets);
		//Now the Vet have identifiers assigned by their IDENTITY-based sequence generator
		List<VetSpeciality> vetSpecialities = new ArrayList<>(VET_COUNT);
		for(Vet vet : vets) {
			for (Specialty specialty : randomSpecialities(specialties)) {
				vetSpecialities.add(new VetSpeciality(vet, specialty));
			}
		}
		LOGGER.info("Inserting {} vet_specialties", formatNumber(vetSpecialities.size()));
		activeSession.insertMultiple(vetSpecialities);
	}

	private List<Specialty> randomSpecialities(List<Specialty> specialties) {
		List<Specialty> randomSpecialties = new ArrayList<>();
		while (randomSpecialties.size() < SPECIALTIES_PER_VET) {
			Specialty specialty = specialties.get(RANDOM.nextInt(specialties.size()));
			if (!randomSpecialties.contains(specialty)) {
				randomSpecialties.add(specialty);
			}
		}
		return randomSpecialties;
	}

	@Override
	protected List<PetType> insertTypes() {
		LOGGER.info("Inserting {} pet types", formatNumber(TYPE_COUNT));
		List<PetType> petTypes = new ArrayList<>(TYPE_COUNT);
		for (int i = 0; i < TYPE_COUNT; i++) {
			PetType petType = new PetType();
			petType.setName(String.format("Type %d", i));
			petTypes.add(petType);
		}
		activeSession.insertMultiple(petTypes);
		return petTypes;
	}

	@Override
	protected void insertOwners(List<PetType> petTypes) {
		List<Owner> owners = new ArrayList<>(OWNER_COUNT);
		List<Pet> pets = new ArrayList<>(OWNER_COUNT);
		List<Visit> visits = new ArrayList<>(OWNER_COUNT);

		for (int ownerIdx = 0; ownerIdx < OWNER_COUNT; ownerIdx++) {
			Owner owner = new Owner();
			owner.setFirstName(String.format("Owner %d - first name", ownerIdx));
			owner.setLastName(String.format("Owner %d - last name", ownerIdx));
			owner.setAddress(String.format("Owner %d - address", ownerIdx));
			owner.setCity(String.format("Owner %d - city", ownerIdx));
			owner.setTelephone(String.format("%010d", ownerIdx));
			owners.add(owner);

			for (int p = 0; p < PETS_PER_OWNER_COUNT; p++) {
				Pet pet = new Pet();
				pet.setName(String.format("Pet %d - name", p));
				pet.setBirthDate(LocalDate.now());
				pet.setType(petTypes.get(RANDOM.nextInt(petTypes.size())));
				pet.setOwner(owner);

				pets.add(pet);

				for (int v = 0; v < VISITS_PER_PET_COUNT; v++) {
					Visit visit = new Visit();
					visit.setDate(LocalDate.now());
					visit.setDescription(String.format("Visit %d - description", v));

					visit.setPet(pet);
					visits.add(visit);
				}
			}
		}
		LOGGER.info("Inserting {} owners", formatNumber(owners.size()));
		activeSession.insertMultiple(owners);
		LOGGER.info("Inserting {} pets", formatNumber(pets.size()));
		activeSession.insertMultiple(pets);
		LOGGER.info("Inserting {} visits", formatNumber(visits.size()));
		activeSession.insertMultiple(visits);
	}
}

