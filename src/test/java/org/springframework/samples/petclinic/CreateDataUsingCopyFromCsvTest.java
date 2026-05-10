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

import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that populates every table using CSV.
 *
 * @author Vlad Mihalcea
 */
@SpringJUnitConfig(CreateDataUsingSpringTest.PetClinicConfig.class)
@TestPropertySource("/application-postgres.properties")
class CreateDataUsingCopyFromCsvTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(AbstractCreateDataTest.class);

	public static final List<String> TABLES = List.of(
		"vets",
		"specialties",
		"vet_specialties",
		"types",
		"owners",
		"pets",
		"visits"
	);

	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;
	private final String csvFolderPath;

	public CreateDataUsingCopyFromCsvTest(
		@Autowired TransactionTemplate transactionTemplate,
		@Autowired JdbcTemplate jdbcTemplate) {
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
		try {
			File csvDataFolder = Paths.get(Thread.currentThread()
					.getContextClassLoader()
					.getResource("db/data/").toURI()
				)
				.normalize()
				.toFile();
			assertTrue(
				csvDataFolder.exists(),
				"Test resources folder does not exist: " + csvDataFolder
			);
			this.csvFolderPath = csvDataFolder.getAbsolutePath();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------------------------------------------------------------
	// Main test
	// -------------------------------------------------------------------------

	@BeforeEach
	protected void init() {
		transactionTemplate.execute(status -> {
			for (String table : AbstractCreateDataTest.TABLES.reversed()) {
				jdbcTemplate.execute(
					String.format("TRUNCATE %s RESTART IDENTITY CASCADE", table)
				);
			}
			return null;
		});
	}

	@Test
	void testInsertData() {
		LOGGER.info("Starting data insertion");
		long startNanos = System.nanoTime();
		transactionTemplate.execute(status -> {
			try {
				for(String table : AbstractCreateDataTest.TABLES) {
					LOGGER.info("Import data into {} table", table);
					jdbcTemplate.execute(
						String.format(
							"COPY %s FROM '%s' DELIMITER ',' CSV;",
							table,
							Paths.get(csvFolderPath, String.format("%s.csv", table)).toAbsolutePath()
						)
					);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});
		long endNanos = System.nanoTime();
		logInsertionResults(startNanos, endNanos);
	}

	void logInsertionResults(long startNanos, long endNanos) {
		QueryCount queryCount = QueryCountHolder.getGrandTotal();
		long insertCount = queryCount.getInsert();
		LOGGER.info("Inserting {} records took {} ms and generated {} INSERT statements",
			recordCount(),
			formatNumber(TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos)),
			formatNumber(insertCount)
		);
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
}
