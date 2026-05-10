package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;

/**
 * @author Vlad Mihalcea
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource("/application-postgres.properties")
@ContextConfiguration(classes = CreateDataUsingSpringTest.PetClinicConfig.class)
public class DropPostgreSQLPublicSchemaTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

    private boolean drop = true;

    @Test
    public void test() {
        if (drop) {
            try {
                transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
					jdbcTemplate.execute((Connection connection) -> {
						ScriptUtils.executeSqlScript(connection,
							new EncodedResource(
								new ClassPathResource(
									String.format("db/%1$s/drop.sql", "postgres")
								)
							),
							true, true,
							ScriptUtils.DEFAULT_COMMENT_PREFIX,
							ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
							ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER,
							ScriptUtils.DEFAULT_COMMENT_PREFIX);

						return null;
					});
                    return null;
                });
            } catch (TransactionException e) {
                LOGGER.error("Failure", e);
            }
        }
    }
}
