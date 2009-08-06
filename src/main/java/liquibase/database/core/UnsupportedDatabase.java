package liquibase.database.core;

import liquibase.database.AbstractDatabase;
import liquibase.database.DataType;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;

public class UnsupportedDatabase extends AbstractDatabase {
    private String dateTimeType;
    private static final DataType BOOLEAN_TYPE = new DataType("INT", false);
    private static final DataType CURRENCY_TYPE = new DataType("DECIMAL", true);
    private static final DataType UUID_TYPE = new DataType("CHAR(36)", false);
    private static final DataType CLOB_TYPE = new DataType("CLOB", false);
    private static final DataType BLOB_TYPE = new DataType("BLOB", false);


    @Override
    public void setConnection(DatabaseConnection conn) {
        super.setConnection(conn);
        dateTimeType = findDateTypeType();
        if (currentDateTimeFunction == null) {
            currentDateTimeFunction = findCurrentDateTimeFunction();
        }
    }

    /**
     * Always returns null or DATABASECHANGELOG table may not be found.
     */
    @Override
    public String getDefaultCatalogName() throws DatabaseException {
        return null;
    }

    /**
     * Always returns null or DATABASECHANGELOG table may not be found.
     */
    @Override
    protected String getDefaultDatabaseSchemaName() throws DatabaseException {
        return null;
    }

    public DataType getBooleanType() {
        return BOOLEAN_TYPE;
    }


    @Override
    public String getFalseBooleanValue() {
        return "0";
    }

    @Override
    public String getTrueBooleanValue() {
        return "1";
    }

    public DataType getCurrencyType() {
        return CURRENCY_TYPE;
    }

    public DataType getUUIDType() {
        return UUID_TYPE;
    }

    public DataType getClobType() {
        return CLOB_TYPE;
    }

    public DataType getBlobType() {
        return BLOB_TYPE;
    }

    public DataType getDateTimeType() {
        return new DataType(dateTimeType, false);
    }

    private String findDateTypeType() {
//todo: reintroduce        ResultSet typeInfo = null;
//        try {
//            typeInfo = getConnection().getMetaData().getTypeInfo();
//            while (typeInfo.next()) {
//                if (typeInfo.getInt("DATA_TYPE") == Types.TIMESTAMP) {
//                    return typeInfo.getString("TYPE_NAME");
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            if (typeInfo != null) {
//                try {
//                    typeInfo.close();
//                } catch (SQLException e) {
//                    ;
//                }
//            }
//        }
        return "DATETIME";
    }

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return false;
    }

    public String getDefaultDriver(String url) {
        return null;
    }    

    public String getTypeName() {
        return "unsupported";
    }

    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    public String getCurrentDateTimeFunction() {
        return currentDateTimeFunction;
    }

    private String findCurrentDateTimeFunction() {
//todo: reintroduce        try {
//            String nowFunction = null;
//            String dateFunction = null;
//            String dateTimeFunction = null;
//            String timeStampFunction = null;
//
//            String[] timeDateFunctions = getConnection().getMetaData().getTimeDateFunctions().split(",");
//            for (String functionName : timeDateFunctions) {
//                String function = functionName.trim().toUpperCase();
//                if (function.endsWith("TIMESTAMP")) {
//                    timeStampFunction = functionName.trim();
//                }
//                if (function.endsWith("DATETIME")) {
//                    dateTimeFunction = functionName.trim();
//                }
//                if (function.endsWith("DATE")) {
//                    dateFunction = functionName.trim();
//                }
//                if ("NOW".equals(function)) {
//                    nowFunction = functionName.trim();
//                }
//            }
//
//            if (nowFunction != null) {
//                return "{fn "+nowFunction+"()"+"}";
//            } else if (timeStampFunction != null) {
//                return "{fn "+timeStampFunction+"()"+"}";
//            } else if (dateTimeFunction != null) {
//                return "{fn "+dateTimeFunction+"()"+"}";
//            } else if (dateFunction != null) {
//                return "{fn "+dateFunction+"()"+"}";
//            } else {
//                return "CURRENT_TIMESTAMP";
//            }
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
        return "CURRENT_TIMESTAMP";
    }


//todo: reintroduce?    @Override
//    protected boolean canCreateChangeLogTable() throws DatabaseException {
//        //check index size.  Many drivers just return 0, so it's not a great test
//        int maxIndexLength;
//        try {
//            maxIndexLength = getConnection().getMetaData().getMaxIndexLength();
//
//            return maxIndexLength == 0
//                    || maxIndexLength >= 150 + 150 + 255 //id + author + filename length
//                    && super.canCreateChangeLogTable();
//        } catch (SQLException e) {
//            throw new DatabaseException(e);
//        }
//    }

    public boolean supportsTablespaces() {
        return false;
    }
}
