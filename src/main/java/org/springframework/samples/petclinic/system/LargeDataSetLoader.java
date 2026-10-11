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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Seeds the large demo data set the first time the application starts against an empty
 * PostgreSQL database, so a plain {@code PetClinicApplication} run already has enough
 * rows (vets=1,000, specialties=100, vet_specialties=5,000, types=100, owners=3,
 * pets=300, visits=150,000) to make the runtime problems visible when browsing.
 *
 * <p>
 * The rows are streamed straight from the {@code db/data/*.csv} classpath resources
 * through the PostgreSQL {@code COPY ... FROM STDIN} protocol (the JDBC
 * {@link CopyManager}), which loads them client-side without needing the database server
 * to read a file or any superuser privilege.
 * </p>
 *
 * <p>
 * Loading is idempotent: if the tables already hold data, the loader does nothing, so
 * restarting the application does not duplicate the rows.
 * </p>
 */
@Configuration
@Profile("postgres")
class LargeDataSetLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(LargeDataSetLoader.class);

	/**
	 * The tables and their columns, in foreign-key dependency order, so the {@code COPY}
	 * commands never violate a reference.
	 */
	private static final List<TableImport> IMPORTS = List.of(new TableImport("vets", "id, first_name, last_name"),
			new TableImport("specialties", "id, name"), new TableImport("vet_specialties", "vet_id, specialty_id"),
			new TableImport("types", "id, name"),
			new TableImport("owners", "id, first_name, last_name, address, city, telephone"),
			new TableImport("pets", "id, name, birth_date, type_id, owner_id"),
			new TableImport("visits", "id, pet_id, visit_date, description"));

	@Bean
	ApplicationRunner loadLargeDataSet(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		return args -> {
			Long visitCount = jdbcTemplate.queryForObject("select count(*) from visits", Long.class);
			if (visitCount != null && visitCount > 0) {
				LOGGER.info("The database already holds {} visits, skipping the data load", visitCount);
				// The rows may have been loaded by a previous mapping that used other
				// identifier sequences, so make sure the current ones are past them.
				inTransaction(dataSource, this::advanceSequences);
				return;
			}

			LOGGER.info("Loading the large data set using the PostgreSQL COPY command");
			long startNanos = System.nanoTime();

			inTransaction(dataSource, connection -> {
				CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
				for (TableImport tableImport : IMPORTS) {
					try (InputStream csv = new ClassPathResource("db/data/" + tableImport.table() + ".csv")
						.getInputStream()) {
						long rows = copyManager.copyIn("COPY " + tableImport.table() + " (" + tableImport.columns()
								+ ") FROM STDIN WITH (FORMAT csv)", csv);
						LOGGER.info("Loaded {} rows into {}", rows, tableImport.table());
					}
				}
				advanceSequences(connection);
			});

			long millis = (System.nanoTime() - startNanos) / 1_000_000;
			LOGGER.info("The large data set was loaded in {} ms", millis);
		};
	}

	/**
	 * The connection pool hands out connections with auto-commit disabled, so the work
	 * runs in a single transaction that is committed explicitly.
	 */
	private void inTransaction(DataSource dataSource, ConnectionWork work) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				work.execute(connection);
				connection.commit();
			}
			catch (Exception e) {
				connection.rollback();
				throw e;
			}
			finally {
				connection.setAutoCommit(autoCommit);
			}
		}
	}

	/**
	 * The CSV rows carry explicit identifiers, so the identifier sequences must be
	 * advanced past the largest value, otherwise the first row the application inserts
	 * would collide with an existing identifier. Both the column's own {@code IDENTITY}
	 * sequence and the explicit sequence used by the {@code @BatchSequence} generators
	 * (e.g., {@code owners_seq}) are advanced, and a sequence never moves backwards. A
	 * missing sequence resolves to {@code NULL}, which {@code setval} simply ignores, so
	 * the same statements work whichever generator the mapping relies on.
	 */
	private void advanceSequences(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String table : List.of("vets", "specialties", "types", "owners", "pets", "visits")) {
				String maxId = "(select coalesce(max(id), 1) from " + table + ")";
				for (String sequence : List.of("pg_get_serial_sequence('" + table + "', 'id')::regclass",
						"to_regclass('" + table + "_seq')")) {
					statement.execute("select setval(s, greatest(" + maxId
							+ ", coalesce(pg_sequence_last_value(s), 1))) from (select " + sequence + " as s) as seq");
				}
			}
		}
	}

	@FunctionalInterface
	private interface ConnectionWork {

		void execute(Connection connection) throws Exception;

	}

	private record TableImport(String table, String columns) {
	}

}
