package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.AlterSequenceStatement;

public class AlterSequenceGenerator implements SqlGenerator<AlterSequenceStatement> {
    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    public boolean supports(AlterSequenceStatement statement, Database database) {
        return database.supportsSequences();
    }

    public ValidationErrors validate(AlterSequenceStatement alterSequenceStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();

        if (database instanceof FirebirdDatabase || database instanceof HsqlDatabase || database instanceof H2Database) {
            validationErrors.checkDisallowedField("incrementBy", alterSequenceStatement.getIncrementBy());
            validationErrors.checkDisallowedField("maxValue", alterSequenceStatement.getMaxValue());
            validationErrors.checkDisallowedField("minValue", alterSequenceStatement.getMinValue());
        }

        if (!(database instanceof OracleDatabase || database instanceof DB2Database || database instanceof MaxDBDatabase)) {
            validationErrors.checkDisallowedField("ordered", alterSequenceStatement.getOrdered());
        }

        validationErrors.checkRequiredField("sequenceName", alterSequenceStatement.getSequenceName());

        return validationErrors;
    }

    public Sql[] generateSql(AlterSequenceStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ALTER SEQUENCE ");
        buffer.append(database.escapeSequenceName(statement.getSchemaName(), statement.getSequenceName()));

        if (statement.getIncrementBy() != null) {
                buffer.append(" INCREMENT BY ").append(statement.getIncrementBy());
        }

        if (statement.getMinValue() != null) {
            if (database instanceof FirebirdDatabase || database instanceof HsqlDatabase || database instanceof H2Database) {
                buffer.append(" RESTART WITH ").append(statement.getMinValue());
            } else {
                buffer.append(" MINVALUE ").append(statement.getMinValue());
            }
        }

        if (statement.getMaxValue() != null) {
            buffer.append(" MAXVALUE ").append(statement.getMaxValue());
        }

        if (statement.getOrdered() != null) {
            if (statement.getOrdered()) {
                buffer.append(" ORDER");
            }
        }

        return new Sql[]{
                new UnparsedSql(buffer.toString())
        };
    }
}
