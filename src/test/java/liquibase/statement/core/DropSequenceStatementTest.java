package liquibase.statement.core;

import liquibase.statement.core.DropSequenceStatement;

public class DropSequenceStatementTest extends AbstractSqStatementTest<DropSequenceStatement> {

    @Override
    protected DropSequenceStatement createStatementUnderTest() {
        return new DropSequenceStatement(null, null);
    }

}
