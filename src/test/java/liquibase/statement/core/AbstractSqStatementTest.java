package liquibase.statement.core;

import liquibase.database.Database;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.test.TestContext;
import liquibase.statement.SqlStatement;
import static org.junit.Assert.fail;
import org.junit.Test;

public abstract class AbstractSqStatementTest<SqlStatementUnderTest extends SqlStatement> {

    protected abstract SqlStatementUnderTest createStatementUnderTest();

    @Test
    public void hasAtLeastOneGenerator() {
        for (Database database : TestContext.getInstance().getAllDatabases()) {
            if (SqlGeneratorFactory.getInstance().supports(createStatementUnderTest(), database)) {
                return;
            };
        }
        fail("did not find a generator");
    }

}
