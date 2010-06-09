package liquibase.snapshot.jvm;

import liquibase.database.Database;
import liquibase.database.core.DB2Database;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.statement.core.RawSqlStatement;

import java.util.List;
import java.util.Map;

public class DB2DatabaseSnapshotGenerator extends JdbcDatabaseSnapshotGenerator {
    public boolean supports(Database database) {
        return database instanceof DB2Database;
    }

    public int getPriority(Database database) {
        return PRIORITY_DATABASE;
    }

    @Override
    protected String convertTableNameToDatabaseTableName(String tableName) {
        return tableName.toUpperCase();
    }

    @Override
    protected String convertColumnNameToDatabaseTableName(String columnName) {
        return columnName.toUpperCase();
    }

    @Override
    public boolean isColumnAutoIncrement(Database database, String schemaName, String tableName, String columnName) throws DatabaseException {
        boolean autoIncrement = false;

        List<Map> rs = ExecutorService.getInstance().getExecutor(database).queryForList(new RawSqlStatement("SELECT IDENTITY FROM SYSCAT.COLUMNS WHERE TABSCHEMA = '" + database.convertRequestedSchemaToSchema(schemaName) + "' AND TABNAME = '" + tableName + "' AND COLNAME = '" + columnName + "' AND HIDDEN != 'S'"));

        for (Map row : rs) {
            String identity = row.get("IDENTITY").toString();
            if (identity.equalsIgnoreCase("Y")) {
                autoIncrement = true;
            }
        }

        return autoIncrement;
    }

}
