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
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.model.BaseEntity;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test to create data.
 *
 * @author Vlad Mihalcea
 */
abstract class AbstractCreateDataTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(AbstractCreateDataTest.class);

	protected static final int BATCH_SIZE = 500;
	protected static final int OWNER_COUNT = 3;
	protected static final int VET_COUNT = 1000;
	protected static final int SPECIALTY_COUNT = 100;
	protected static final int TYPE_COUNT = 100;
	protected static final int PETS_PER_OWNER_COUNT = 100;
	protected static final int VISITS_PER_PET_COUNT = 500;

	/** Specialties assigned to each vet. */
	protected static final int SPECIALTIES_PER_VET = 5;

	protected static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

	public static final List<String> TABLES = List.of(
		"vets",
		"specialties",
		"vet_specialties",
		"types",
		"owners",
		"pets",
		"visits"
	);

	protected static final List<String> SEQUENCES = List.of(
		"owners_id_seq",
		"pets_id_seq",
		"specialties_id_seq",
		"types_id_seq",
		"vets_id_seq",
		"visits_id_seq"
	);

	protected final OwnerRepository owners;
	protected final PetTypeRepository types;
	protected final VetRepository vets;
	protected final EntityManager entityManager;
	protected final TransactionTemplate transactionTemplate;
	protected final JdbcTemplate jdbcTemplate;

	public AbstractCreateDataTest(
			OwnerRepository owners,
			PetTypeRepository types,
			VetRepository vets,
			EntityManager entityManager,
			TransactionTemplate transactionTemplate,
			JdbcTemplate jdbcTemplate) {
		this.owners = owners;
		this.types = types;
		this.vets = vets;
		this.entityManager = entityManager;
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@BeforeEach
	protected void init() {
		truncateAll();
	}

	protected void insertData() {
		// Insert in dependency order so that FK references are valid
		LOGGER.info("Starting data insertion");
		long startNanos = System.nanoTime();
		QueryCountHolder.clear();
		insertVets(insertSpecialties());
		insertOwners(insertTypes());
		syncWithDatabase();
		long endNanos = System.nanoTime();
		logInsertionResults(startNanos, endNanos);
		if(shouldExportData()) {
			exportTables();
		}
	}

	private void logInsertionResults(long startNanos, long endNanos) {
		QueryCount queryCount = QueryCountHolder.getGrandTotal();
		long insertCount = queryCount.getInsert();
		LOGGER.info("Inserting {} records took {} ms and generated {} INSERT statements",
			recordCount(),
			formatNumber(TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos)),
			formatNumber(insertCount)
		);
	}

	protected void syncWithDatabase() {
	}

	protected abstract List<Specialty> insertSpecialties();

	protected abstract void insertVets(List<Specialty> specialties);

	protected abstract List<PetType> insertTypes();

	protected abstract void insertOwners(List<PetType> petTypes);

	protected boolean shouldExportData() {
		return false;
	}

	protected String recordCount() {
		StringBuilder records = new StringBuilder();
		records.append("[");
		Map<String, Long> TABLE_COUNT_MAP = TABLES.stream().collect(Collectors.groupingBy(
			table -> table,
			LinkedHashMap::new,
			Collectors.summingLong(table -> jdbcTemplate.queryForObject(
				String.format("SELECT COUNT(*) FROM %s", table),
				Long.class
			))
		));
		TABLE_COUNT_MAP.forEach((table, count) -> {
			records.append(String.format("{%s=%S}", table, formatNumber(count)));
			if(!table.equals(TABLES.get(TABLES.size() - 1))) {
				records.append(", ");
			}
		});
		records.append("] → ");
		records.append(
			formatNumber(TABLE_COUNT_MAP.values().stream().mapToLong(Long::longValue).sum())
		);
		return records.toString();
	}

	protected String formatNumber(Number number) {
		return new DecimalFormat("#,###").format(number);
	}

	// -------------------------------------------------------------------------
	// Infrastructure helpers
	// -------------------------------------------------------------------------

	private void truncateAll() {
		transactionTemplate.execute(status -> {
			for (String table : TABLES.reversed()) {
				jdbcTemplate.execute(
					String.format("TRUNCATE %s RESTART IDENTITY CASCADE", table)
				);
			}
			for (String sequence : SEQUENCES) {
				jdbcTemplate.execute(
					String.format("ALTER SEQUENCE %s RESTART WITH 1", sequence)
				);
			}
			jdbcTemplate.execute(
				"COMMIT"
			);
			return null;
		});
	}

	/**
	 * Insert using batching.
	 */
	protected <T extends BaseEntity> List<T> insertBatch(
		Function<Integer, T> entitySupplier,
		int iterations) {
		List<T> entities = IntStream.range(0, iterations)
			.mapToObj(entitySupplier::apply).toList();

		spitInBatches(entities, BATCH_SIZE).forEach(this::executeBatch);

		return entities;
	}

	/**
	 * Insert using batching.
	 */
	protected <T extends BaseEntity> List<T> insertBatch(
			Function<Integer, T> entitySupplier,
			int iterations,
			int batchSize) {
		List<T> entities = IntStream.range(0, iterations)
			.mapToObj(entitySupplier::apply).toList();

		spitInBatches(entities, batchSize).forEach(this::executeBatch);

		return entities;
	}

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

	/**
	 * Split an element collection into fixed-size batches.
	 *
	 * @param elements elements to split
	 * @param batchSize maximum size of each batch
	 * @param <T> element type
	 * @return stream of batches
	 */
	protected <T> Stream<List<T>> spitInBatches(List<T> elements, int batchSize) {
		int elementCount = elements.size();
		if (elementCount <= 0) {
			return Stream.empty();
		}
		int batchCount = (elementCount - 1) / batchSize;
		return IntStream.range(0, batchCount + 1)
			.mapToObj(batchNumber -> elements.subList(
				batchNumber * batchSize,
				batchNumber == batchCount ? elementCount : (batchNumber + 1) * batchSize
			));
	}

	protected void exportTables() {
		File resourcesFolder;

		try {
			resourcesFolder = Paths.get(
					Thread
						.currentThread()
						.getContextClassLoader()
						.getResource("db/data/").toURI())
				.resolve("../../../../src/main/resources/db/data")
				.normalize()
				.toFile();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		assertTrue(resourcesFolder.exists(), "Test resources folder does not exist: " + resourcesFolder);

		String resourcesFolderPath = resourcesFolder.getAbsolutePath();

		transactionTemplate.execute(status -> {
			try {
				for(String table : TABLES) {
					jdbcTemplate.execute(
						String.format(
							"COPY %s TO '%s' DELIMITER ',' CSV;",
							table,
							Paths.get(resourcesFolderPath, String.format("%s.csv", table)).toAbsolutePath()
						)
					);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});
	}
}
