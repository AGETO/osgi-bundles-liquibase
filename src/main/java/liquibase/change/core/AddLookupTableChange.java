package liquibase.change.core;

import liquibase.change.AbstractChange;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.database.Database;
import liquibase.database.core.DB2Database;
import liquibase.database.core.InformixDatabase;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.SybaseASADatabase;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.statement.core.ReorganizeTableStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts data from an existing column to create a lookup table.
 * A foreign key is created between the old column and the new lookup table.
 */
public class AddLookupTableChange extends AbstractChange {

    private String existingTableSchemaName;
    private String existingTableName;
    private String existingColumnName;

    private String newTableSchemaName;
    private String newTableName;
    private String newColumnName;
    private String newColumnDataType;
    private String constraintName;

    public AddLookupTableChange() {
        super("addLookupTable", "Add Lookup Table", ChangeMetaData.PRIORITY_DEFAULT);
    }

    public String getExistingTableSchemaName() {
        return existingTableSchemaName;
    }

    public void setExistingTableSchemaName(String existingTableSchemaName) {
        this.existingTableSchemaName = existingTableSchemaName;
    }

    public String getExistingTableName() {
        return existingTableName;
    }

    public void setExistingTableName(String existingTableName) {
        this.existingTableName = existingTableName;
    }

    public String getExistingColumnName() {
        return existingColumnName;
    }

    public void setExistingColumnName(String existingColumnName) {
        this.existingColumnName = existingColumnName;
    }


    public String getNewTableSchemaName() {
        return newTableSchemaName;
    }

    public void setNewTableSchemaName(String newTableSchemaName) {
        this.newTableSchemaName = newTableSchemaName;
    }

    public String getNewTableName() {
        return newTableName;
    }

    public void setNewTableName(String newTableName) {
        this.newTableName = newTableName;
    }

    public String getNewColumnName() {
        return newColumnName;
    }

    public void setNewColumnName(String newColumnName) {
        this.newColumnName = newColumnName;
    }

    public String getNewColumnDataType() {
        return newColumnDataType;
    }

    public void setNewColumnDataType(String newColumnDataType) {
        this.newColumnDataType = newColumnDataType;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public String getFinalConstraintName() {
        if (constraintName == null) {
            return ("FK_" + getExistingTableName() + "_" + getNewTableName()).toUpperCase();
        } else {
            return constraintName;
        }
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    @Override
    protected Change[] createInverses() {
        DropForeignKeyConstraintChange dropFK = new DropForeignKeyConstraintChange();
        dropFK.setBaseTableSchemaName(getExistingTableSchemaName());
        dropFK.setBaseTableName(getExistingTableName());
        dropFK.setConstraintName(getFinalConstraintName());

        DropTableChange dropTable = new DropTableChange();
        dropTable.setSchemaName(getNewTableSchemaName());
        dropTable.setTableName(getNewTableName());

        return new Change[]{
                dropFK,
                dropTable,
        };
    }

    public SqlStatement[] generateStatements(Database database) {
        List<SqlStatement> statements = new ArrayList<SqlStatement>();

        String newTableSchemaName = getNewTableSchemaName() == null?database.getDefaultSchemaName():getNewTableSchemaName();
        String existingTableSchemaName = getExistingTableSchemaName() == null?database.getDefaultSchemaName():getExistingTableSchemaName();

        SqlStatement[] createTablesSQL = {new RawSqlStatement("CREATE TABLE " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " AS SELECT DISTINCT " + getExistingColumnName() + " AS " + getNewColumnName() + " FROM " + database.escapeTableName(existingTableSchemaName, getExistingTableName()) + " WHERE " + getExistingColumnName() + " IS NOT NULL")};
        if (database instanceof MSSQLDatabase) {
            createTablesSQL = new SqlStatement[]{new RawSqlStatement("SELECT DISTINCT " + getExistingColumnName() + " AS " + getNewColumnName() + " INTO " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " FROM " + database.escapeTableName(existingTableSchemaName, getExistingTableName()) + " WHERE " + getExistingColumnName() + " IS NOT NULL"),};
        } else if (database instanceof SybaseASADatabase) {
            createTablesSQL = new SqlStatement[]{new RawSqlStatement("SELECT DISTINCT " + getExistingColumnName() + " AS " + getNewColumnName() + " INTO " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " FROM " + database.escapeTableName(existingTableSchemaName, getExistingTableName()) + " WHERE " + getExistingColumnName() + " IS NOT NULL"),};
        } else if (database instanceof DB2Database) {
            createTablesSQL = new SqlStatement[]{
                    new RawSqlStatement("CREATE TABLE " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " AS (SELECT " + getExistingColumnName() + " AS " + getNewColumnName() + " FROM " + database.escapeTableName(existingTableSchemaName, getExistingTableName()) + ") WITH NO DATA"),
                    new RawSqlStatement("INSERT INTO " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " SELECT DISTINCT " + getExistingColumnName() + " FROM " + database.escapeTableName(existingTableSchemaName, getExistingTableName()) + " WHERE " + getExistingColumnName() + " IS NOT NULL"),
            };
        } else if (database instanceof InformixDatabase) {
            createTablesSQL = new SqlStatement[] {
                    new RawSqlStatement("CREATE TABLE " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " ( "  + getNewColumnName() + " " + getNewColumnDataType() + " )"),
                    new RawSqlStatement("INSERT INTO " + database.escapeTableName(newTableSchemaName, getNewTableName()) + " ( "  + getNewColumnName() + " ) SELECT DISTINCT "  + getExistingColumnName() + " FROM " + database.escapeTableName(existingTableSchemaName, getExistingTableName()) + " WHERE " + getExistingColumnName() + " IS NOT NULL"),
            };
        }

        statements.addAll(Arrays.asList(createTablesSQL));

        if (!(database instanceof OracleDatabase)) {
            AddNotNullConstraintChange addNotNullChange = new AddNotNullConstraintChange();
            addNotNullChange.setSchemaName(newTableSchemaName);
            addNotNullChange.setTableName(getNewTableName());
            addNotNullChange.setColumnName(getNewColumnName());
            addNotNullChange.setColumnDataType(getNewColumnDataType());
            statements.addAll(Arrays.asList(addNotNullChange.generateStatements(database)));
        }

        if (database instanceof DB2Database) {
            statements.add(new ReorganizeTableStatement(newTableSchemaName, getNewTableName()));
        }

        AddPrimaryKeyChange addPKChange = new AddPrimaryKeyChange();
        addPKChange.setSchemaName(newTableSchemaName);
        addPKChange.setTableName(getNewTableName());
        addPKChange.setColumnNames(getNewColumnName());
        statements.addAll(Arrays.asList(addPKChange.generateStatements(database)));

        if (database instanceof DB2Database) {
            statements.add(new ReorganizeTableStatement(newTableSchemaName, getNewTableName()));
        }

        AddForeignKeyConstraintChange addFKChange = new AddForeignKeyConstraintChange();
        addFKChange.setBaseTableSchemaName(existingTableSchemaName);
        addFKChange.setBaseTableName(getExistingTableName());
        addFKChange.setBaseColumnNames(getExistingColumnName());
        addFKChange.setReferencedTableSchemaName(newTableSchemaName);
        addFKChange.setReferencedTableName(getNewTableName());
        addFKChange.setReferencedColumnNames(getNewColumnName());

        addFKChange.setConstraintName(getFinalConstraintName());
        statements.addAll(Arrays.asList(addFKChange.generateStatements(database)));

        return statements.toArray(new SqlStatement[statements.size()]);
    }

    public String getConfirmationMessage() {
        return "Lookup table added for "+getExistingTableName()+"."+getExistingColumnName();
    }
}
