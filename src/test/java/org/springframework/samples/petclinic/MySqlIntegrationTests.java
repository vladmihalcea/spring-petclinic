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
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.vet.PetRepository;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.VisitRepository;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("mysql")
@DisabledInNativeImage
@DisabledInAotMode
class MySqlIntegrationTests {

	public static final Logger LOGGER = LoggerFactory.getLogger(MySqlIntegrationTests.class);

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private PetRepository pets;

	@Autowired
	private VisitRepository visits;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private DataSource dataSource;

	@Test
	@Commit
	void testInsertData() {
		insertVisits();
	}

	private void insertVets() {
		transactionTemplate.execute(status -> {
			long vetCount = vets.count();

			int vetInsertCount = 100_000;
			int batchSize = 1000;
			for (int i = 0; i < vetInsertCount; i++) {
				Vet vet = new Vet();
				vet.setFirstName("Dr. nr. " + (vetCount + i));
				vet.setLastName("Smith" + (vetCount + i));

				entityManager.persist(vet);
				if (i > 0 && i % batchSize == 0) {
					entityManager.flush();
					entityManager.clear();
				}
			}

			return null;
		});
	}

	private void insertPets() {
		for (int j = 0; j < 40; j++) {
			transactionTemplate.execute(status -> {
				int petCount = (int) pets.count();
				List<PetType> types = pets.types();

				int petInsertCount = 10_000;
				int batchSize = 1000;

				for (int i = 0; i < petInsertCount; i++) {
					int petIndex = petCount + i;
					Pet pet = new Pet();
					pet.setName("Pet nr. " + petIndex);
					pet.setBirthDate(LocalDateTime.now().minusMinutes(petIndex).toLocalDate());
					pet.setType(types.get(i % types.size()));

					entityManager.persist(pet);
					if (i > 0 && i % batchSize == 0) {
						entityManager.flush();
						entityManager.clear();
					}
				}

				return null;
			});
		}
	}

	private void insertVisits() {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			for (int j = 1; j <= 3; j++) {
				LOGGER.info("Inserting Visits for Pet: {}", j);
				final int petId = j;
				for (int k = 0; k < 1000; k++) {
					LOGGER.info("Running batch number : {}", k + 1);
					int batchSize = 1000;
					int visitCount = Math.toIntExact(visits.countByPetId(petId));

					try (PreparedStatement ps = connection.prepareStatement("""
							INSERT INTO `petclinic`.`visits` (
								`pet_id`,
								`visit_date`,
								`description`
							)
							VALUES (
								?,
								?,
								?
							)
							""")) {
						for (int i = 0; i < batchSize; i++) {
							int visitIndex = visitCount + i;
							ps.setLong(1, petId);
							ps.setDate(2, Date.valueOf(LocalDateTime.now().minusMinutes(visitIndex).toLocalDate()));
							ps.setString(3, "Description nr. " + visitIndex);

							ps.addBatch();
						}
						ps.executeBatch();
					}
					connection.commit();
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
