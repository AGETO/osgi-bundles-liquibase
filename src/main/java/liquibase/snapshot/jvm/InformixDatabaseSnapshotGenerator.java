package liquibase.snapshot.jvm;

import liquibase.database.Database;
import liquibase.database.core.InformixDatabase;
import liquibase.database.structure.Column;
import liquibase.database.structure.ForeignKey;
import liquibase.database.structure.ForeignKeyInfo;
import liquibase.exception.DatabaseException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InformixDatabaseSnapshotGenerator extends JdbcDatabaseSnapshotGenerator {
    
    private static final Map<Integer, String> qualifiers = new HashMap<Integer, String>();
    
    static {
    	qualifiers.put(0, "YEAR");
    	qualifiers.put(2, "MONTH");
    	qualifiers.put(4, "DAY");
    	qualifiers.put(6, "HOUR");
    	qualifiers.put(8, "MINUTE");
    	qualifiers.put(10, "SECOND");
    	qualifiers.put(11, "FRACTION(1)");
    	qualifiers.put(12, "FRACTION(2)");
    	qualifiers.put(13, "FRACTION(3)");
    	qualifiers.put(14, "FRACTION(4)");
    	qualifiers.put(15, "FRACTION(5)");
    }

    public boolean supports(Database database) {
        return database instanceof InformixDatabase;
    }

    public int getPriority(Database database) {
        return PRIORITY_DATABASE;
    }

    @Override
    protected void getColumnTypeAndDefValue(Column columnInfo, ResultSet rs, Database database) throws SQLException, DatabaseException {
    	// See http://publib.boulder.ibm.com/infocenter/idshelp/v115/topic/com.ibm.sqlr.doc/sqlr07.htm
    	String typeName = rs.getString("TYPE_NAME").toUpperCase();
    	if ("DATETIME".equals(typeName) || "INTERVAL".equals(typeName)) {
    		int collength = columnInfo.getColumnSize();
    		//int positions = collength / 256;
    		int firstQualifierType = (collength % 256) / 16;
    		int lastQualifierType = (collength % 256) % 16;
    		String type = "DATETIME".equals(typeName) ? "DATETIME" : "INTERVAL";
    		String firstQualifier = qualifiers.get(firstQualifierType);
    		String lastQualifier = qualifiers.get(lastQualifierType);
    		columnInfo.setTypeName(type + " " + firstQualifier + " TO " + lastQualifier);
    		columnInfo.setLengthSemantics(Column.LengthSemantics.BYTE);
    	} else {
        	super.getColumnTypeAndDefValue(columnInfo, rs, database);
    	}
    }
    
    @Override
	public List<ForeignKey> getForeignKeys(String schemaName, String foreignKeyTableName, Database database) throws DatabaseException {
        List<ForeignKey> fkList = new ArrayList<ForeignKey>();
		try {
            String dbCatalog = database.convertRequestedSchemaToCatalog(schemaName);
            // Informix handles schema differently
            String dbSchema = null;
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
}
