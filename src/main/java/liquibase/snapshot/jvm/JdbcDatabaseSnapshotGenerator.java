package liquibase.snapshot.jvm;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.database.core.InformixDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.structure.*;
import liquibase.database.typeconversion.TypeConverterFactory;
import liquibase.diff.DiffStatusListener;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.logging.LogFactory;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.DatabaseSnapshotGenerator;
import liquibase.statement.core.GetViewDefinitionStatement;
import liquibase.statement.core.SelectSequencesStatement;
import liquibase.util.StringUtils;

import java.sql.*;
import java.text.ParseException;
import java.util.*;

public abstract class JdbcDatabaseSnapshotGenerator implements DatabaseSnapshotGenerator {

    private Set<DiffStatusListener> statusListeners;

    protected String convertTableNameToDatabaseTableName(String tableName) {
        return tableName;
    }

    protected String convertColumnNameToDatabaseTableName(String columnName) {
        return columnName;
    }

    public Table getDatabaseChangeLogTable(Database database) throws DatabaseException {
        return getTable(database.getLiquibaseSchemaName(), database.getDatabaseChangeLogTableName(), database);
    }

    public Table getDatabaseChangeLogLockTable(Database database) throws DatabaseException {
        return getTable(database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName(), database);
    }

    public boolean hasDatabaseChangeLogTable(Database database) {
        return hasTable(database.getLiquibaseSchemaName(), database.getDatabaseChangeLogTableName(), database);
    }

    public boolean hasDatabaseChangeLogLockTable(Database database) {
        return hasTable(database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName(), database);
    }

    public boolean hasTable(String schemaName, String tableName, Database database) {
        try {
            ResultSet rs = getMetaData(database).getTables(database.convertRequestedSchemaToCatalog(schemaName), database.convertRequestedSchemaToSchema(schemaName), convertTableNameToDatabaseTableName(tableName), new String[]{"TABLE"});
            try {
                return rs.next();
            } finally {
                try {
                    rs.close();
                } catch (SQLException ignore) { }
            }
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }
    
    public boolean hasView(String schemaName, String viewName, Database database) {
        try {
            ResultSet rs = getMetaData(database).getTables(database.convertRequestedSchemaToCatalog(schemaName), database.convertRequestedSchemaToSchema(schemaName), convertTableNameToDatabaseTableName(viewName), new String[]{"VIEW"});
            try {
                return rs.next();
            } finally {
                try {
                    rs.close();
                } catch (SQLException ignore) { }
            }
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    public Table getTable(String schemaName, String tableName, Database database) throws DatabaseException {
        ResultSet rs = null;
        try {
            DatabaseMetaData metaData = getMetaData(database);
            rs = metaData.getTables(database.convertRequestedSchemaToCatalog(schemaName), database.convertRequestedSchemaToSchema(schemaName), convertTableNameToDatabaseTableName(tableName), new String[]{"TABLE"});

            Table table;
            try {
                if (!rs.next()) {
                    return null;
                }

                table = readTable(rs, database);
            } finally {
                rs.close();
            }

            rs = metaData.getColumns(database.convertRequestedSchemaToCatalog(schemaName), database.convertRequestedSchemaToSchema(schemaName), convertTableNameToDatabaseTableName(tableName), null);
            try {
                while (rs.next()) {
                    table.getColumns().add(readColumn(rs, database));
                }
            } finally {
                rs.close();
            }

            return table;
        } catch (Exception e) {
            throw new DatabaseException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) { }
            }
        }
    }

    public Column getColumn(String schemaName, String tableName, String columnName, Database database) throws DatabaseException {
        ResultSet rs = null;
        try {
            rs = getMetaData(database).getColumns(database.convertRequestedSchemaToCatalog(schemaName), database.convertRequestedSchemaToSchema(schemaName), convertTableNameToDatabaseTableName(tableName), convertColumnNameToDatabaseTableName(columnName));

            if (!rs.next()) {
                return null;
            }

            return readColumn(rs, database);
        } catch (Exception e) {
            throw new DatabaseException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) { }
            }
        }
    }

    private Table readTable(ResultSet rs, Database database) throws SQLException {
        String name = convertFromDatabaseName(rs.getString("TABLE_NAME"));
        String schemaName = convertFromDatabaseName(rs.getString("TABLE_SCHEM"));
        String remarks = rs.getString("REMARKS");

        Table table = new Table(name);
        table.setRemarks(StringUtils.trimToNull(remarks));
        table.setDatabase(database);
        table.setSchema(schemaName);
        table.setRawSchemaName(rs.getString("TABLE_SCHEM"));
        table.setRawCatalogName(rs.getString("TABLE_CAT"));

        return table;
    }

    private View readView(ResultSet rs, Database database) throws SQLException, DatabaseException {
        String name = convertFromDatabaseName(rs.getString("TABLE_NAME"));
        String schemaName = convertFromDatabaseName(rs.getString("TABLE_SCHEM"));

        View view = new View();
        view.setName(name);
        view.setSchema(schemaName);
        view.setRawSchemaName(rs.getString("TABLE_SCHEM"));
        view.setRawCatalogName(rs.getString("TABLE_CAT"));
        try {
            view.setDefinition(database.getViewDefinition(rs.getString("TABLE_SCHEM"), name));
        } catch (DatabaseException e) {
            throw new DatabaseException("Error getting " + database.getConnection().getURL() + " view with " + new GetViewDefinitionStatement(view.getSchema(), name), e);
        }

        return view;
    }

    private Column readColumn(ResultSet rs, Database database) throws SQLException, DatabaseException {
        Column column = new Column();

        String tableName = convertFromDatabaseName(rs.getString("TABLE_NAME"));
        String columnName = convertFromDatabaseName(rs.getString("COLUMN_NAME"));
        String schemaName = convertFromDatabaseName(rs.getString("TABLE_SCHEM"));
        String catalogName = convertFromDatabaseName(rs.getString("TABLE_CAT"));
        String remarks = rs.getString("REMARKS");

        if (database.isSystemTable(catalogName, schemaName, tableName) || database.isSystemView(catalogName, schemaName, tableName)) {
            return null;
        }

	    column.setName(columnName);

	    Table table = new Table(tableName);
        table.setSchema(schemaName);
        column.setTable(table);

	    configureColumnType(column, rs);

	    int nullable = rs.getInt("NULLABLE");
        if (nullable == DatabaseMetaData.columnNoNulls) {
            column.setNullable(false);
        } else if (nullable == DatabaseMetaData.columnNullable) {
            column.setNullable(true);
        }

        getColumnTypeAndDefValue(column, rs, database);
        column.setRemarks(remarks);

        return column;
    }

	/**
	 * Configuration of column's type.
	 * @param column Column to configure
	 * @param rs Result set, used as a property resource.
	 * @throws java.sql.SQLException wrong Result Set content 
	 * */
	protected void configureColumnType(Column column, ResultSet rs) throws SQLException {
		column.setDataType(rs.getInt("DATA_TYPE"));
		column.setColumnSize(rs.getInt("COLUMN_SIZE"));
		column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));

		// Set true, if precision should be initialize
		column.setInitPrecision(
				!((column.getDataType() == Types.DECIMAL ||
				   column.getDataType() == Types.NUMERIC ||
				   column.getDataType() == Types.REAL) && rs.getString("DECIMAL_DIGITS") == null)
		);
	}

	public DatabaseSnapshot createSnapshot(Database database, String requestedSchema, Set<DiffStatusListener> listeners) throws DatabaseException {

        if (requestedSchema == null) {
            requestedSchema = database.getDefaultSchemaName();
        }

        try {

            DatabaseMetaData databaseMetaData = getMetaData(database);
            this.statusListeners = listeners;

            DatabaseSnapshot snapshot = new DatabaseSnapshot(database, requestedSchema);

            readTables(snapshot, requestedSchema, databaseMetaData);
            readViews(snapshot, requestedSchema, databaseMetaData);
            readForeignKeyInformation(snapshot, requestedSchema, databaseMetaData);
            readPrimaryKeys(snapshot, requestedSchema, databaseMetaData);
            readColumns(snapshot, requestedSchema, databaseMetaData);
            readUniqueConstraints(snapshot, requestedSchema, databaseMetaData);
            readIndexes(snapshot, requestedSchema, databaseMetaData);
            readSequences(snapshot, requestedSchema, databaseMetaData);

            return snapshot;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    protected DatabaseMetaData getMetaData(Database database) throws SQLException {
        DatabaseMetaData databaseMetaData = null;
        if (database.getConnection() != null) {
            databaseMetaData = ((JdbcConnection) database.getConnection()).getUnderlyingConnection().getMetaData();
        }
        return databaseMetaData;
    }


    protected void readTables(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws SQLException, DatabaseException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading tables for " + database.toString() + " ...");

        ResultSet rs = databaseMetaData.getTables(database.convertRequestedSchemaToCatalog(schema), database.convertRequestedSchemaToSchema(schema), null, new String[]{"TABLE", "ALIAS"});
        try {
            while (rs.next()) {
                Table table = readTable(rs, database);
                table.setSchema(schema); //not always set for some reason
                if (database.isLiquibaseTable(table.getName())) {
                    if (table.getName().equalsIgnoreCase(database.getDatabaseChangeLogTableName())) {
                        snapshot.setDatabaseChangeLogTable(table);
                        continue;
                    }

                    if (table.getName().equalsIgnoreCase(database.getDatabaseChangeLogLockTableName())) {
                        snapshot.setDatabaseChangeLogLockTable(table);
                        continue;
                    }
                }
                if (database.isSystemTable(table.getRawCatalogName(), table.getRawSchemaName(), table.getName()) || database.isSystemView(table.getRawCatalogName(), table.getRawSchemaName(), table.getName())) {
                    continue;
                }

                snapshot.getTables().add(table);
            }
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) { }
        }
    }

    protected void readViews(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws SQLException, DatabaseException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading views for " + database.toString() + " ...");

        ResultSet rs = databaseMetaData.getTables(database.convertRequestedSchemaToCatalog(schema), database.convertRequestedSchemaToSchema(schema), null, new String[]{"VIEW"});
        try {
            while (rs.next()) {
                View view = readView(rs, database);
                if (database.isSystemView(view.getRawCatalogName(), view.getRawSchemaName(), view.getName())) {
                    continue;
                }

                snapshot.getViews().add(view);
            }
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) { }
        }
    }

    protected String convertFromDatabaseName(String objectName) {
        if (objectName == null) {
            return null;
        }
        return objectName;
    }

    protected void readColumns(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws SQLException, DatabaseException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading columns for " + database.toString() + " ...");

        Statement selectStatement = null;
        ResultSet rs = null;
        try {
            selectStatement = ((JdbcConnection) database.getConnection()).getUnderlyingConnection().createStatement();
            rs = databaseMetaData.getColumns(database.convertRequestedSchemaToCatalog(schema), database.convertRequestedSchemaToSchema(schema), null, null);
            while (rs.next()) {
                Column column = readColumn(rs, database);

                if (column == null) {
                    continue;
                }

                //replace temp table in column with real table
                Table tempTable = column.getTable();
                column.setTable(null);

                Table table;
                if (database.isLiquibaseTable(tempTable.getName())) {
                    if (tempTable.getName().equalsIgnoreCase(database.getDatabaseChangeLogTableName())) {
                        table = snapshot.getDatabaseChangeLogTable();
                    } else if (tempTable.getName().equalsIgnoreCase(database.getDatabaseChangeLogLockTableName())) {
                        table = snapshot.getDatabaseChangeLogLockTable();
                    } else {
                        throw new UnexpectedLiquibaseException("Unknown liquibase table: " + tempTable.getName());
                    }
                } else {
                    table = snapshot.getTable(tempTable.getName());
                }
                if (table == null) {
                    View view = snapshot.getView(tempTable.getName());
                    if (view == null) {
                        LogFactory.getLogger().debug("Could not find table or view " + tempTable.getName() + " for column " + column.getName());
                        continue;
                    } else {
                        column.setView(view);
                        column.setAutoIncrement(false);
                        view.getColumns().add(column);
                    }
                } else {
                    column.setTable(table);
                    column.setAutoIncrement(isColumnAutoIncrement(database, table.getSchema(), table.getName(), column.getName()));
                    table.getColumns().add(column);
                }

                column.setPrimaryKey(snapshot.isPrimaryKey(column));
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) { }
            }
            if (selectStatement != null) {
                try {
                    selectStatement.close();
                } catch (SQLException ignored) { }
            }
        }
    }

    /**
     * Method assigns correct column type and default value to Column object.
     * <p/>
     * This method should be database engine specific. JDBC implementation requires
     * database engine vendors to convert native DB types to java objects.
     * During conversion some metadata information are being lost or reported incorrectly via DatabaseMetaData objects.
     * This method, if necessary, must be overriden. It must go below DatabaseMetaData implementation and talk directly to database to get correct metadata information.
     */
    protected void getColumnTypeAndDefValue(Column columnInfo, ResultSet rs, Database database) throws SQLException, DatabaseException {
        Object defaultValue = rs.getObject("COLUMN_DEF");
        try {
            columnInfo.setDefaultValue(TypeConverterFactory.getInstance().findTypeConverter(database).convertDatabaseValueToObject(defaultValue, columnInfo.getDataType(), columnInfo.getColumnSize(), columnInfo.getDecimalDigits(), database));
        } catch (ParseException e) {
            throw new DatabaseException(e);
        }
        columnInfo.setTypeName(TypeConverterFactory.getInstance().findTypeConverter(database).getDataType(rs.getString("TYPE_NAME"), columnInfo.isAutoIncrement()).toString());
    } // end of method getColumnTypeAndDefValue()

    protected void readForeignKeyInformation(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws DatabaseException, SQLException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading foreign keys for " + database.toString() + " ...");

	    String dbSchema = database.convertRequestedSchemaToSchema(schema);
	    // First we try to find all database-specific FKs.
	    // TODO: there are some filters bellow in for loop. Are they needed here too?
	    snapshot.getForeignKeys().addAll(getAdditionalForeignKeys(dbSchema, database));

        // Then tries to find all other standard FKs
	    for (Table table : snapshot.getTables()) {
	        for (ForeignKey fk : getForeignKeys(schema, table.getName(), snapshot.getDatabase())) {

		        Table tempPKTable = fk.getPrimaryKeyTable();
		        Table pkTable = snapshot.getTable(tempPKTable.getName());
		        if (pkTable == null) {
                              LogFactory.getLogger().warning("Foreign key " + fk.getName() + " references table " + tempPKTable + ", which is in a different schema. Retaining FK in diff, but table will not be diffed.");
		        }

		        Table tempFkTable = fk.getForeignKeyTable();
		        Table fkTable = snapshot.getTable(tempFkTable.getName());
		        if (fkTable == null) {
                            LogFactory.getLogger().warning("Foreign key " + fk.getName() + " is in table " + tempFkTable + ", which we cannot find. Ignoring.");
                            continue;
		        }

		        snapshot.getForeignKeys().add(fk);
	        }
        }
    }

    public boolean hasIndex(String schemaName, String tableName, String indexName, Database database, String columnNames) throws DatabaseException {
        DatabaseSnapshot databaseSnapshot = createSnapshot(database, schemaName, null);
        if (databaseSnapshot.getIndex(indexName) != null) {
            return true;
        }
        if (tableName != null && columnNames != null) {
            for (Index index : databaseSnapshot.getIndexes()) {
                if (index.getColumnNames().replaceAll("\\s+","").equalsIgnoreCase(columnNames.replaceAll("\\s+",""))) {
                    return true;
                }
            }
        }
        return false;
    }

    public ForeignKey getForeignKeyByForeignKeyTable(String schemaName, String foreignKeyTableName, String fkName, Database database) throws DatabaseException {
        for (ForeignKey fk : getForeignKeys(schemaName, foreignKeyTableName, database)) {
            if (fk.getName().equalsIgnoreCase(fkName)) {
                return fk;
            }
        }

        return null;
    }

	/**
	 * Generation of Foreign Key based on information about it.
	 *
	 * @param fkInfo contains all needed properties of FK
	 * @param database current database
	 * @param fkList list of already generated keys
	 * @return generated Foreing Key
	 * @throws liquibase.exception.DatabaseException Database Exception
	 * */
	public ForeignKey generateForeignKey(ForeignKeyInfo fkInfo, Database database, List<ForeignKey> fkList) throws DatabaseException {
		//Simple (non-composite) keys have KEY_SEQ=1, so create the ForeignKey.
		//In case of subsequent parts of composite keys (KEY_SEQ>1) don't create new instance, just reuse the one from previous call.
		//According to #getExportedKeys() contract, the result set rows are properly sorted, so the reuse of previous FK instance is safe.
		ForeignKey foreignKey = null;

		if (fkInfo.getKeySeq() == 1 || (fkInfo.getReferencesUniqueColumn() && fkInfo.getKeySeq() == 0)) {
			foreignKey = new ForeignKey();
		} else {
			for (ForeignKey foundFK : fkList) {
				if (foundFK.getName().equalsIgnoreCase(fkInfo.getFkName())) {
					foreignKey = foundFK;
				}
			}
			if (foreignKey == null) {
				throw new DatabaseException("Database returned out of sequence foreign key column for " + fkInfo.getFkName());
			}
		}

		foreignKey.setName(fkInfo.getFkName());

                final Table pkTable = new Table(fkInfo.getPkTableName());
                pkTable.setSchema(fkInfo.getPkTableSchema());
		foreignKey.setPrimaryKeyTable(pkTable);
		foreignKey.addPrimaryKeyColumn(fkInfo.getPkColumn());

                final String fkTableName = fkInfo.getFkTableName();
		Table fkTable = new Table(fkTableName);
		fkTable.setSchema(fkInfo.getFkSchema());
		foreignKey.setForeignKeyTable(fkTable);
		foreignKey.addForeignKeyColumn(fkInfo.getFkColumn());

		foreignKey.setUpdateRule(fkInfo.getUpdateRule());
		foreignKey.setDeleteRule(fkInfo.getDeleteRule());

		foreignKey.setReferencesUniqueColumn(fkInfo.getReferencesUniqueColumn());

		if (database.supportsInitiallyDeferrableColumns()) {

			if (fkInfo.getDeferrablility() == DatabaseMetaData.importedKeyInitiallyDeferred) {
				foreignKey.setDeferrable(Boolean.TRUE);
				foreignKey.setInitiallyDeferred(Boolean.TRUE);
			} else if (fkInfo.getDeferrablility() == DatabaseMetaData.importedKeyInitiallyImmediate) {
				foreignKey.setDeferrable(Boolean.TRUE);
				foreignKey.setInitiallyDeferred(Boolean.FALSE);
			} else if (fkInfo.getDeferrablility() == DatabaseMetaData.importedKeyNotDeferrable) {
				foreignKey.setDeferrable(Boolean.FALSE);
				foreignKey.setInitiallyDeferred(Boolean.FALSE);
			}
		}

		return foreignKey;
	}

	/**
	 * It finds <u>only</u> all database-specific Foreign Keys.
	 * By default it returns an empty ArrayList.
	 * @param schemaName current shemaName
	 * @param database current database
	 * @return list of database-specific Foreing Keys
	 * @throws liquibase.exception.DatabaseException any kinds of SQLException errors
	 * */
	public List<ForeignKey> getAdditionalForeignKeys(String schemaName, Database database) throws DatabaseException{
		return new ArrayList<ForeignKey>();
	}

	public List<ForeignKey> getForeignKeys(String schemaName, String foreignKeyTableName, Database database) throws DatabaseException {
        List<ForeignKey> fkList = new ArrayList<ForeignKey>();
		try {
            String dbCatalog = database.convertRequestedSchemaToCatalog(schemaName);
            String dbSchema = database.convertRequestedSchemaToSchema(schemaName);
            ResultSet rs = getMetaData(database).getImportedKeys(dbCatalog, dbSchema, convertTableNameToDatabaseTableName(foreignKeyTableName));

            try {
                while (rs.next()) {
                    ForeignKeyInfo fkInfo = fillForeignKeyInfo(rs);

                    fkList.add(generateForeignKey(fkInfo, database, fkList));
                }
            } finally {
                rs.close();
            }

            return fkList;

        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Fill foreign key information from the current register of a getImportedKeys resultset
     * @param rs The resultset returned by getImportedKeys
     * @return Foreign key information 
     */
    protected ForeignKeyInfo fillForeignKeyInfo(ResultSet rs) throws DatabaseException, SQLException {
        ForeignKeyInfo fkInfo = new ForeignKeyInfo();
        fkInfo.setFkName(convertFromDatabaseName(rs.getString("FK_NAME")));
        fkInfo.setFkSchema(convertFromDatabaseName(rs.getString("FKTABLE_SCHEM")));
        fkInfo.setFkTableName(convertFromDatabaseName(rs.getString("FKTABLE_NAME")));
        fkInfo.setFkColumn(convertFromDatabaseName(rs.getString("FKCOLUMN_NAME")));
        fkInfo.setPkTableSchema(rs.getString("PKTABLE_SCHEM"));
        fkInfo.setPkTableName(convertFromDatabaseName(rs.getString("PKTABLE_NAME")));
        fkInfo.setPkColumn(convertFromDatabaseName(rs.getString("PKCOLUMN_NAME")));
        fkInfo.setKeySeq(rs.getInt("KEY_SEQ"));
        ForeignKeyConstraintType updateRule = convertToForeignKeyConstraintType(rs.getInt("UPDATE_RULE"));
        if (rs.wasNull()) {
            updateRule = null;
        }
        fkInfo.setUpdateRule(updateRule);
        ForeignKeyConstraintType deleteRule = convertToForeignKeyConstraintType(rs.getInt("DELETE_RULE"));
        if (rs.wasNull()) {
            deleteRule = null;
        }
        fkInfo.setDeleteRule(deleteRule);
        fkInfo.setDeferrablility(rs.getShort("DEFERRABILITY"));
        return fkInfo;
    }        

  
    protected ForeignKeyConstraintType convertToForeignKeyConstraintType(int jdbcType) throws DatabaseException {
        if (jdbcType == DatabaseMetaData.importedKeyCascade) {
            return ForeignKeyConstraintType.importedKeyCascade;
        } else if (jdbcType == DatabaseMetaData.importedKeyNoAction) {
            return ForeignKeyConstraintType.importedKeyNoAction;
        } else if (jdbcType == DatabaseMetaData.importedKeyRestrict) {
            return ForeignKeyConstraintType.importedKeyRestrict;
        } else if (jdbcType == DatabaseMetaData.importedKeySetDefault) {
            return ForeignKeyConstraintType.importedKeySetDefault;
        } else if (jdbcType == DatabaseMetaData.importedKeySetNull) {
            return ForeignKeyConstraintType.importedKeySetNull;
        } else {
            throw new DatabaseException("Unknown constraint type: " + jdbcType);
        }
    }

    protected void readIndexes(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws DatabaseException, SQLException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading indexes for " + database.toString() + " ...");

        for (Table table : snapshot.getTables()) {
            ResultSet rs = null;
            Statement statement = null;
            try {
                if (database instanceof OracleDatabase) {
                    //oracle getIndexInfo is buggy and slow.  See Issue 1824548 and http://forums.oracle.com/forums/thread.jspa?messageID=578383&#578383
                    statement = ((JdbcConnection) database.getConnection()).getUnderlyingConnection().createStatement();
                    String sql = "SELECT INDEX_NAME, 3 AS TYPE, TABLE_NAME, COLUMN_NAME, COLUMN_POSITION AS ORDINAL_POSITION, null AS FILTER_CONDITION FROM ALL_IND_COLUMNS WHERE TABLE_OWNER='" + database.convertRequestedSchemaToSchema(schema) + "' AND TABLE_NAME='" + table.getName() + "' ORDER BY INDEX_NAME, ORDINAL_POSITION";
                    rs = statement.executeQuery(sql);
                } else {
                    rs = databaseMetaData.getIndexInfo(database.convertRequestedSchemaToCatalog(schema), database.convertRequestedSchemaToSchema(schema), table.getName(), false, true);
                }
                Map<String, Index> indexMap = new HashMap<String, Index>();
                while (rs.next()) {
                    String indexName = convertFromDatabaseName(rs.getString("INDEX_NAME"));
                    /*
                     * TODO Informix generates indexnames with a leading blank if no name given.
                     * An identifier with a leading blank is not allowed.
                     * So here is it replaced.
                     */
                    if (database instanceof InformixDatabase && indexName.startsWith(" ")) {
                        indexName = "_generated_index_" + indexName.substring(1);
                    }
                    short type = rs.getShort("TYPE");
    //                String tableName = rs.getString("TABLE_NAME");
                    boolean nonUnique = true;
                    try {
                        nonUnique = rs.getBoolean("NON_UNIQUE");
                    } catch (SQLException e) {
                        //doesn't exist in all databases
                    }
                    String columnName = convertFromDatabaseName(rs.getString("COLUMN_NAME"));
                    short position = rs.getShort("ORDINAL_POSITION");
                    /*
                     * TODO maybe bug in jdbc driver? Need to investigate.
                     * If this "if" is commented out ArrayOutOfBoundsException is thrown
                     * because it tries to access an element -1 of a List (position-1)
                     */
                    if (database instanceof InformixDatabase
                            && type != DatabaseMetaData.tableIndexStatistic
                            && position == 0) {
                        System.out.println(this.getClass().getName() + ": corrected position to " + ++position);
                    }
                    String filterCondition = rs.getString("FILTER_CONDITION");

                    if (type == DatabaseMetaData.tableIndexStatistic) {
                        continue;
                    }
    //                if (type == DatabaseMetaData.tableIndexOther) {
    //                    continue;
    //                }

                    if (columnName == null) {
                        //nothing to index, not sure why these come through sometimes
                        continue;
                    }
                    Index indexInformation;
                    if (indexMap.containsKey(indexName)) {
                        indexInformation = indexMap.get(indexName);
                    } else {
                        indexInformation = new Index();
                        indexInformation.setTable(table);
                        indexInformation.setName(indexName);
                        indexInformation.setUnique(!nonUnique);
                        indexInformation.setFilterCondition(filterCondition);
                        indexMap.put(indexName, indexInformation);
                    }

                    for (int i = indexInformation.getColumns().size(); i < position; i++) {
                        indexInformation.getColumns().add(null);
                    }
                    indexInformation.getColumns().set(position - 1, columnName);
                }
                for (Map.Entry<String, Index> entry : indexMap.entrySet()) {
                    snapshot.getIndexes().add(entry.getValue());
                }
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException ignored) { }
                }
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException ignored) { }
                }
            }
        }

        Set<Index> indexesToRemove = new HashSet<Index>();

	    /*
	    * marks indexes as "associated with" instead of "remove it"
	    * Index should have associations with:
	    * foreignKey, primaryKey or uniqueConstraint
	    * */
        for (Index index : snapshot.getIndexes()) {
            for (PrimaryKey pk : snapshot.getPrimaryKeys()) {
                if (index.getTable().getName().equalsIgnoreCase(pk.getTable().getName()) && index.getColumnNames().equals(pk.getColumnNames())) {
                    index.addAssociatedWith(Index.MARK_PRIMARY_KEY);
                }
            }
            for (ForeignKey fk : snapshot.getForeignKeys()) {
                if (index.getTable().getName().equalsIgnoreCase(fk.getForeignKeyTable().getName()) && index.getColumnNames().equals(fk.getForeignKeyColumns())) {
	                index.addAssociatedWith(Index.MARK_FOREIGN_KEY);
                }
            }
            for (UniqueConstraint uc : snapshot.getUniqueConstraints()) {
                if (index.getTable().getName().equalsIgnoreCase(uc.getTable().getName()) && index.getColumnNames().equals(uc.getColumnNames())) {
	                index.addAssociatedWith(Index.MARK_UNIQUE_CONSTRAINT);
                }
            }

        }
        snapshot.getIndexes().removeAll(indexesToRemove);
    }

    protected void readPrimaryKeys(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws DatabaseException, SQLException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading primary keys for " + database.toString() + " ...");

        //we can't add directly to the this.primaryKeys hashSet because adding columns to an exising PK changes the hashCode and .contains() fails
        List<PrimaryKey> foundPKs = new ArrayList<PrimaryKey>();

        for (Table table : snapshot.getTables()) {
            ResultSet rs = databaseMetaData.getPrimaryKeys(database.convertRequestedSchemaToCatalog(schema), database.convertRequestedSchemaToSchema(schema), table.getName());

            try {
                while (rs.next()) {
                    String tableName = convertFromDatabaseName(rs.getString("TABLE_NAME"));
                    String columnName = convertFromDatabaseName(rs.getString("COLUMN_NAME"));
                    short position = rs.getShort("KEY_SEQ");

                    boolean foundExistingPK = false;
                    for (PrimaryKey pk : foundPKs) {
                        if (pk.getTable().getName().equals(tableName)) {
                            pk.addColumnName(position - 1, columnName);

                            foundExistingPK = true;
                        }
                    }

                    if (!foundExistingPK) {
                        PrimaryKey primaryKey = new PrimaryKey();
                        primaryKey.setTable(table);
                        primaryKey.addColumnName(position - 1, columnName);
                        primaryKey.setName(convertPrimaryKeyName(rs.getString("PK_NAME")));

                        foundPKs.add(primaryKey);
                    }
                }
            } finally {
                rs.close();
            }

        }

        snapshot.getPrimaryKeys().addAll(foundPKs);
    }

    protected String convertPrimaryKeyName(String pkName) throws SQLException {
        return pkName;
    }

    protected void readUniqueConstraints(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws DatabaseException, SQLException {
        Database database = snapshot.getDatabase();
        updateListeners("Reading unique constraints for " + database.toString() + " ...");
    }

//    private void readUniqueConstraints(String catalog, String schema) throws DatabaseException, SQLException {
//        updateListeners("Reading unique constraints for " + database.toString() + " ...");
//
//        //noinspection unchecked
//        List<String> sequenceNamess = (List<String>) new Executor(database).queryForList(database.findUniqueConstraints(schema), String.class);
//
//        for (String sequenceName : sequenceNamess) {
//            Sequence seq = new Sequence();
//            seq.setName(sequenceName);
//
//            sequences.add(seq);
//        }
//    }

    protected void readSequences(DatabaseSnapshot snapshot, String schema, DatabaseMetaData databaseMetaData) throws DatabaseException {
        Database database = snapshot.getDatabase();
        if (database.supportsSequences()) {
            updateListeners("Reading sequences for " + database.toString() + " ...");

            String convertedSchemaName = database.convertRequestedSchemaToSchema(schema);

            //noinspection unchecked
            List<String> sequenceNames = (List<String>) ExecutorService.getInstance().getExecutor(database).queryForList(new SelectSequencesStatement(schema), String.class);


            if (sequenceNames != null) {
                for (String sequenceName : sequenceNames) {
                    Sequence seq = new Sequence();
                    seq.setName(sequenceName.trim());
                    seq.setSchema(convertedSchemaName);

                    snapshot.getSequences().add(seq);
                }
            }
        } else {
            updateListeners("Sequences not supported for " + database.toString() + " ...");
        }
    }

    protected void updateListeners(String message) {
        if (this.statusListeners == null) {
            return;
        }
        LogFactory.getLogger().debug(message);
        for (DiffStatusListener listener : this.statusListeners) {
            listener.statusUpdate(message);
        }
    }

    public boolean isColumnAutoIncrement(Database database, String schemaName, String tableName, String columnName) throws SQLException, DatabaseException {
        if (!database.supportsAutoIncrement()) {
            return false;
        }

        boolean autoIncrement = false;

        Statement statement = null;
        ResultSet selectRS = null;
        try {
            statement = ((JdbcConnection) database.getConnection()).getUnderlyingConnection().createStatement();
            selectRS = statement.executeQuery("SELECT " + database.escapeColumnName(schemaName, tableName, columnName) + " FROM " + database.escapeTableName(schemaName, tableName) + " WHERE 1 = 0");
            ResultSetMetaData meta = selectRS.getMetaData();
            autoIncrement = meta.isAutoIncrement(1);
        } finally {
            if (selectRS != null) {
                try {
                    selectRS.close();
                } catch (SQLException ignored) { }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) { }
            }
        }

        return autoIncrement;
    }

    public int getDatabaseType(int type, Database database) {
        int returnType = type;
        if (returnType == java.sql.Types.BOOLEAN) {
            String booleanType = TypeConverterFactory.getInstance().findTypeConverter(database).getBooleanType().getDataTypeName();
            if (!booleanType.equalsIgnoreCase("boolean")) {
                returnType = java.sql.Types.TINYINT;
            }
        }

        return returnType;
    }
}
