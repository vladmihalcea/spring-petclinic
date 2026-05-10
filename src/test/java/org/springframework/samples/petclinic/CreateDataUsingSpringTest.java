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

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.samples.petclinic.model.BaseEntity;
import org.springframework.samples.petclinic.owner.*;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

/**
 * Integration test to create data using a plain Spring Framework context,
 * without {@code @DataJpaTest}.
 *
 * @author Vlad Mihalcea
 */
@SpringJUnitConfig(CreateDataUsingSpringTest.PetClinicConfig.class)
@TestPropertySource("/application-postgres.properties")
class CreateDataUsingSpringTest extends AbstractCreateDataTest {

	public CreateDataUsingSpringTest(
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
	@Disabled
	void testInsertData() {
		insertData();
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
			SPECIALTY_COUNT,
			BATCH_SIZE
		);
	}

	@Override
	protected void insertVets(List<Specialty> specialties) {
		LOGGER.info("Inserting {} vets", formatNumber(VET_COUNT));

		insertBatch(i -> {
				Vet vet = new Vet();
				vet.setFirstName(String.format("Vet %d - first name", i));
				vet.setLastName(String.format("Vet %d - last name", i));
				for (int s = 0; s < SPECIALTIES_PER_VET; s++) {
					// Round-robin over specialties; IDs are 1-based
					vet.addSpecialty(specialties.get(RANDOM.nextInt(specialties.size())));
				}
				return vet;
			},
			VET_COUNT,
			BATCH_SIZE
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
			TYPE_COUNT,
			BATCH_SIZE
		);
	}

	@Override
	protected void insertOwners(List<PetType> petTypes) {
		LOGGER.info("Inserting {} owners", formatNumber(OWNER_COUNT));

		List<Owner> owners = IntStream.range(0, OWNER_COUNT)
			.mapToObj((int i) -> {
				Owner owner = new Owner();
				owner.setFirstName(String.format("Owner %d - first name", i));
				owner.setLastName(String.format("Owner %d - last name", i));
				owner.setAddress(String.format("Owner %d - address", i));
				owner.setCity(String.format("Owner %d - city", i));
				owner.setTelephone(String.format("%010d", i));

				return owner;
			}).toList();

		insertBatch(owners, BATCH_SIZE);

		List<Pet> pets = owners.stream().flatMap(owner ->
			IntStream.range(0, PETS_PER_OWNER_COUNT).mapToObj((int i) -> {
				Pet pet = new Pet();
				pet.setName(String.format("Pet %d - name", i));
				pet.setBirthDate(LocalDate.now());
				pet.setType(petTypes.get(RANDOM.nextInt(petTypes.size())));
				//Requires a JPA mapping change
				//pet.setOwner(owner);

				return pet;
			})).toList();

		LOGGER.info("Inserting {} pets", pets.size());
		insertBatch(pets, BATCH_SIZE);

		List<Visit> visits = pets.stream().flatMap(pet ->
			IntStream.range(0, VISITS_PER_PET_COUNT).mapToObj((int i) -> {
				Visit visit = new Visit();
				visit.setDate(LocalDate.now());
				visit.setDescription(String.format("Visit %d - description", i));
				//Requires a JPA mapping change
				//visit.setPet(pet);

				return visit;
			})).toList();

		LOGGER.info("Inserting {} visits", visits.size());
		//insertBatch(visits, BATCH_SIZE, Executors.newFixedThreadPool(8));
		insertBatch(visits, BATCH_SIZE);
	}

	// -------------------------------------------------------------------------
	// Infrastructure helpers
	// -------------------------------------------------------------------------

	private <T extends BaseEntity> void insertBatch(
		List<T> entities,
		int batchSize) {
		spitInBatches(entities, batchSize).forEach(this::executeBatch);
	}

	private <T extends BaseEntity> void insertBatch(
		List<T> entities,
		int batchSize,
		ExecutorService executor) {
		spitInBatches(entities, batchSize).map(
			batch -> executor.submit(() -> executeBatch(batch))
		).forEach(future -> {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
	}

	// -------------------------------------------------------------------------
	// Plain Spring Framework infrastructure configuration
	// -------------------------------------------------------------------------

	/**
	 * Minimal Spring Framework configuration that provides all beans required by
	 * {@link AbstractCreateDataTest}. No Spring Boot auto-configuration is used;
	 * every bean is declared explicitly.
	 */
	@Configuration
	@EnableTransactionManagement
	@EnableJpaRepositories(basePackages = {
			"org.springframework.samples.petclinic.owner",
			"org.springframework.samples.petclinic.vet"
	})
	@EnableCaching
	static class PetClinicConfig {

		/**
		 * Must be {@code static} so that it is instantiated early enough by the
		 * {@code BeanFactoryPostProcessor} machinery to resolve {@code @Value}
		 * placeholders in other bean definitions.
		 */
		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		/**
		 * Simple in-memory cache manager that satisfies the {@code @Cacheable("vets")}
		 * annotation on {@link VetRepository#findAll()}.
		 */
		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("vets");
		}

		@Bean
		public DataSource dataSource(
				@Value("${spring.datasource.url}") String url,
				@Value("${spring.datasource.username}") String username,
				@Value("${spring.datasource.password}") String password) {
			HikariDataSource ds = new HikariDataSource();
			ds.setJdbcUrl(url);
			ds.setUsername(username);
			ds.setPassword(password);

			ChainListener listener = new ChainListener();
			listener.addListener(new SLF4JQueryLoggingListener());
			listener.addListener(new DataSourceQueryCountListener());
			return ProxyDataSourceBuilder
				.create(ds)
				.name("DS-Proxy")
				.listener(listener)
				.build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
			emf.setDataSource(dataSource);
			emf.setPackagesToScan("org.springframework.samples.petclinic");

			HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
			adapter.setDatabase(Database.POSTGRESQL);
			adapter.setGenerateDdl(false);
			emf.setJpaVendorAdapter(adapter);
			emf.setPackagesToScan("org.springframework.samples.petclinic");

			Properties jpaProps = new Properties();
			// Schema is managed externally; do not alter it during tests.
			jpaProps.setProperty("hibernate.hbm2ddl.auto", "none");
			// Batch insert settings — keep in sync with BATCH_SIZE in the base class.
			jpaProps.setProperty("hibernate.jdbc.batch_size", "500");
			jpaProps.setProperty("hibernate.order_inserts", "true");
			jpaProps.setProperty("hibernate.order_updates", "true");
			jpaProps.setProperty("hibernate.jdbc.batch_versioned_data", "true");
			jpaProps.setProperty("hibernate.id.sequence.increment_size_mismatch_strategy", "log");
			jpaProps.setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, PhysicalNamingStrategySnakeCaseImpl.class.getName());
			emf.setJpaProperties(jpaProps);

			return emf;
		}

		@Bean
		public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}

		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}
	}
}

