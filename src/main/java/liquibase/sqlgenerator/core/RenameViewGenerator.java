package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.RenameViewStatement;

public class RenameViewGenerator extends AbstractSqlGenerator<RenameViewStatement> {

    @Override
    public boolean supports(RenameViewStatement statement, Database database) {
        return !(database instanceof DerbyDatabase
                || database instanceof HsqlDatabase
                 || database  instanceof H2Database
                || database instanceof DB2Database
                || database instanceof CacheDatabase
                || database instanceof FirebirdDatabase
                || database instanceof InformixDatabase
                || database instanceof SybaseASADatabase);
    }

    public ValidationErrors validate(RenameViewStatement renameViewStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("oldViewName", renameViewStatement.getOldViewName());
        validationErrors.checkRequiredField("newViewName", renameViewStatement.getNewViewName());

        validationErrors.checkDisallowedField("schemaName", renameViewStatement.getSchemaName(), database, OracleDatabase.class);

        return validationErrors;
    }

    public Sql[] generateSql(RenameViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String sql;

        if (database instanceof MSSQLDatabase) {
            sql = "exec sp_rename '" + database.escapeViewName(statement.getSchemaName(), statement.getOldViewName()) + "', '" + statement.getNewViewName() + '\'';
        } else if (database instanceof MySQLDatabase) {
            sql = "RENAME TABLE " + database.escapeViewName(statement.getSchemaName(), statement.getOldViewName()) + " TO " + database.escapeViewName(statement.getSchemaName(), statement.getNewViewName());
        } else if (database instanceof PostgresDatabase) {
            sql = "ALTER TABLE " + database.escapeViewName(statement.getSchemaName(), statement.getOldViewName()) + " RENAME TO " + database.escapeViewName(null, statement.getNewViewName());
        } else if (database instanceof MaxDBDatabase) {
            sql = "RENAME VIEW " + database.escapeViewName(statement.getSchemaName(), statement.getOldViewName()) + " TO " + database.escapeViewName(null, statement.getNewViewName());
        } else {
            sql = "RENAME " + database.escapeViewName(statement.getSchemaName(), statement.getOldViewName()) + " TO " + database.escapeViewName(null, statement.getNewViewName());
        }

        return new Sql[]{
                new UnparsedSql(sql)
        };
    }
}
