package io.transatron.transaction.manager.db;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@SpringBootTest(
    classes = {
        MigrationTest.MigrationTestConfiguration.class
    },
    properties = {
        "spring.liquibase.change-log=classpath:liquibase/db/changelog/changelog-current.xml",
        "spring.liquibase.contexts=test",
        "spring.datasource.url=jdbc:postgresql://${embedded.postgresql.host}:${embedded.postgresql.port}/${embedded.postgresql.schema}",
        "spring.datasource.username=${embedded.postgresql.user}",
        "spring.datasource.password=${embedded.postgresql.password}"
    }
)
class MigrationTest {

    private static final String CHANGE_LOG_FILE = "liquibase/db/changelog/changelog-current.xml";

    @Autowired
    protected DataSource dataSource;

    @Test
    @SneakyThrows
    void shouldUpdateAndRollback() {
        var liquibaseContext = "test";
        var liquibase = getLiquibase();

        liquibase.update(liquibaseContext);
        liquibase.rollback(liquibase.getDatabase().getRanChangeSetList().size(), liquibaseContext);
        liquibase.update(liquibaseContext);
    }

    @SneakyThrows
    private Liquibase getLiquibase() {
        return new Liquibase(
            CHANGE_LOG_FILE,
            new ClassLoaderResourceAccessor(),
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()))
        );
    }

    @EnableAutoConfiguration
    @Configuration
    public static class MigrationTestConfiguration {

    }

}