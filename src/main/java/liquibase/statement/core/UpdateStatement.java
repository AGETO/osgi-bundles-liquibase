package liquibase.statement.core;

import liquibase.statement.AbstractSqlStatement;

import java.util.*;

public class UpdateStatement extends AbstractSqlStatement {
    private String schemaName;
    private String tableName;
    private SortedMap<String, Object> newColumnValues = new TreeMap<String, Object>();
    private String whereClause;
    private List<Object> whereParameters = new ArrayList<Object>();


    public UpdateStatement(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public UpdateStatement addNewColumnValue(String columnName, Object newValue) {
        newColumnValues.put(columnName, newValue);

        return this;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public UpdateStatement setWhereClause(String whereClause) {
        this.whereClause = whereClause;

        return this;
    }

    public UpdateStatement addWhereParameter(Object value) {
        this.whereParameters.add(value);

        return this;
    }

    public UpdateStatement addWhereParameters(Object... value) {
        this.whereParameters.addAll(Arrays.asList(value));

        return this;
    }

    public Map<String, Object> getNewColumnValues() {
        return newColumnValues;
    }

    public List<Object> getWhereParameters() {
        return whereParameters;
    }
}
