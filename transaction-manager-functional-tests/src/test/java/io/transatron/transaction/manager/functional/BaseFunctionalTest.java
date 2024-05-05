package io.transatron.transaction.manager.functional;

import io.transatron.transaction.manager.TronTransactionManagerApplication;
import io.transatron.transaction.manager.functional.configuration.StepsConfiguration;
import io.transatron.transaction.manager.functional.steps.ServiceSteps;
import io.transatron.transaction.manager.functional.steps.TestUser;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@SpringBootTest(
    webEnvironment = DEFINED_PORT,
    classes = {
        TronTransactionManagerApplication.class,
        StepsConfiguration.class
    }
)
@ActiveProfiles({"test", "cloud"})
public abstract class BaseFunctionalTest {

    private static final AtomicLong USER_ID_SEED = new AtomicLong(100);
    private static final AtomicLong USER_SESSION_SEED = new AtomicLong(100000);
    protected TestUser user = TestUser.of(0, 0);

    @LocalServerPort
    protected int serverPort;

    @LocalManagementPort
    protected int managementPort;

    @Autowired
    protected ServiceSteps serviceSteps;

    @BeforeEach
    public void setUp() {
        user = TestUser.of(USER_ID_SEED.getAndIncrement(), USER_SESSION_SEED.getAndIncrement());

        RestAssured.port = serverPort;
    }

}
