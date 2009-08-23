package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.CreateSequenceStatement;

public class CreateSequenceGenerator implements SqlGenerator<CreateSequenceStatement> {
    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    public boolean supports(CreateSequenceStatement statement, Database database) {
        return database.supportsSequences();
    }

    public ValidationErrors validate(CreateSequenceStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();

        validationErrors.checkRequiredField("sequenceName", statement.getSequenceName());

        if (database instanceof FirebirdDatabase) {
            validationErrors.checkDisallowedField("startValue", statement.getStartValue());
            validationErrors.checkDisallowedField("incrementBy", statement.getIncrementBy());
        }

        if (database instanceof FirebirdDatabase || database instanceof HsqlDatabase || database  instanceof H2Database) {
            validationErrors.checkDisallowedField("minValue", statement.getMinValue());
            validationErrors.checkDisallowedField("maxValue", statement.getMaxValue());
        }

        if (database instanceof FirebirdDatabase || database instanceof HsqlDatabase || database  instanceof H2Database) {
            validationErrors.addError("Database does not support creating sequences with maxValue");
        }

        if (statement.getOrdered() != null && !(database instanceof OracleDatabase || database instanceof DB2Database || database instanceof MaxDBDatabase)) {
            validationErrors.checkDisallowedField("ordered", statement.getOrdered());
        }


        return validationErrors;
    }

    public Sql[] generateSql(CreateSequenceStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE SEQUENCE ");
        buffer.append(database.escapeSequenceName(statement.getSchemaName(), statement.getSequenceName()));
        if (statement.getStartValue() != null) {
            buffer.append(" START WITH ").append(statement.getStartValue());
        }
        if (statement.getIncrementBy() != null) {
            buffer.append(" INCREMENT BY ").append(statement.getIncrementBy());
        }
        if (statement.getMinValue() != null) {
            buffer.append(" MINVALUE ").append(statement.getMinValue());
        }
        if (statement.getMaxValue() != null) {
            buffer.append(" MAXVALUE ").append(statement.getMaxValue());
        }

        if (statement.getOrdered() != null) {
            if (statement.getOrdered()) {
                buffer.append(" ORDER");
            }
        }

        return new Sql[]{new UnparsedSql(buffer.toString())};
    }
}
