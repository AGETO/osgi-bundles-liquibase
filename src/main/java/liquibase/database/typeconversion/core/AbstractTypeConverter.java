package liquibase.database.typeconversion.core;

import liquibase.change.ColumnConfig;
import liquibase.database.Database;
import liquibase.database.core.*;
import liquibase.database.structure.Column;
import liquibase.database.structure.type.*;
import liquibase.database.typeconversion.TypeConverter;
import liquibase.exception.DateParseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.logging.LogFactory;
import liquibase.statement.DatabaseFunction;
import liquibase.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractTypeConverter implements TypeConverter {

    public Object convertDatabaseValueToObject(Object value, int databaseDataType, int firstParameter, int secondParameter, Database database) throws ParseException {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return convertToCorrectObjectType(((String) value).replaceFirst("^'", "").replaceFirst("'$", ""), databaseDataType, firstParameter, secondParameter, database);
        } else {
            return value;
        }
    }

    public DataType getDataType(Object object) {
        if (object instanceof BigInteger) {
            return getBigIntType();
        } else if (object instanceof Boolean) {
            return getBooleanType();
        } else if (object instanceof String) {
            return getVarcharType();
        } else if (object instanceof java.sql.Date) {
            return getDateType();
        } else if (object instanceof java.sql.Timestamp) {
            return getDateTimeType();
        } else if (object instanceof java.sql.Time) {
            return getTimeType();
        } else if (object instanceof java.util.Date) {
            return getDateTimeType();
        } else if (object instanceof Double) {
            return getDoubleType();
        } else if (object instanceof Float) {
            return getFloatType();
        } else if (object instanceof Integer) {
            return getIntType();
        } else if (object instanceof Long) {
            return getBigIntType();
        } else if (object instanceof DatabaseFunction) {
            return new DatabaseFunctionType();
        } else {
            throw new UnexpectedLiquibaseException("Unknown object type "+object.getClass().getName());
        }
    }

    protected Object convertToCorrectObjectType(String value, int dataType, int columnSize, int decimalDigits, Database database) throws ParseException {
        if (value == null) {
            return null;
        }
        if (dataType == Types.CLOB || dataType == Types.VARCHAR || dataType == Types.CHAR || dataType == Types.LONGVARCHAR) {
            if (value.equalsIgnoreCase("NULL")) {
                return null;
            } else {
                return value;
            }
        }

        value = StringUtils.trimToNull(value);
        if (value == null) {
            return null;
        }

        try {
            if (dataType == Types.DATE) {
                return new java.sql.Date(database.parseDate(value).getTime());
            } else if (dataType == Types.TIMESTAMP) {
                return new java.sql.Timestamp(database.parseDate(value).getTime());
            } else if (dataType == Types.TIME) {
                return new java.sql.Time(database.parseDate(value).getTime());
            } else if (dataType == Types.BIGINT) {
                return new BigInteger(value);
            } else if (dataType == Types.BIT) {
                value = value.replaceFirst("b'", ""); //mysql puts wierd chars in bit field
                if (value.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                } else if (value.equalsIgnoreCase("false")) {
                    return Boolean.FALSE;
                } else if (value.equals("1")) {
                    return Boolean.TRUE;
                } else if (value.equals("0")) {
                    return Boolean.FALSE;
                } else if (value.equals("(1)")) {
                    return Boolean.TRUE;
                } else if (value.equals("(0)")) {
                    return Boolean.FALSE;
                }
                throw new ParseException("Unknown bit value: " + value, 0);
            } else if (dataType == Types.BOOLEAN) {
                return Boolean.valueOf(value);
            } else if (dataType == Types.DECIMAL) {
                if (decimalDigits == 0) {
                    return new Integer(value);
                }
                return new BigDecimal(value);
            } else if (dataType == Types.DOUBLE || dataType == Types.NUMERIC) {
                return new BigDecimal(value);
            } else if (dataType == Types.FLOAT) {
                return new Float(value);
            } else if (dataType == Types.INTEGER) {
                return new Integer(value);
            } else if (dataType == Types.NULL) {
                return null;
            } else if (dataType == Types.REAL) {
                return new Float(value);
            } else if (dataType == Types.SMALLINT) {
                return new Integer(value);
            } else if (dataType == Types.TINYINT) {
                return new Integer(value);
            } else if (dataType == Types.BLOB) {
                return "!!!!!! LIQUIBASE CANNOT OUTPUT BLOB VALUES !!!!!!";
            } else {
                LogFactory.getLogger().warning("Do not know how to convert type " + dataType);
                return value;
            }
        } catch (DateParseException e) {
            return new DatabaseFunction(value);
        } catch (NumberFormatException e) {
            return new DatabaseFunction(value);
        }
    }

    /**
     * Returns the database-specific datatype for the given column configuration.
     * This method will convert some generic column types (e.g. boolean, currency) to the correct type
     * for the current database.
     */
    public DataType getDataType(String columnTypeString, Boolean autoIncrement) {
        // Parse out data type and precision
        // Example cases: "CLOB", "java.sql.Types.CLOB", "CLOB(10000)", "java.sql.Types.CLOB(10000)
        String dataTypeName = null;
        String precision = null;
        String additionalInformation = null;
        if (columnTypeString.startsWith("java.sql.Types") && columnTypeString.contains("(")) {
            precision = columnTypeString.substring(columnTypeString.indexOf("(") + 1, columnTypeString.indexOf(")"));
            dataTypeName = columnTypeString.substring(columnTypeString.lastIndexOf(".") + 1, columnTypeString.indexOf("("));
        } else if (columnTypeString.startsWith("java.sql.Types")) {
            dataTypeName = columnTypeString.substring(columnTypeString.lastIndexOf(".") + 1);
        } else if (columnTypeString.contains("(")) {
            precision = columnTypeString.substring(columnTypeString.indexOf("(") + 1, columnTypeString.indexOf(")"));
            dataTypeName = columnTypeString.substring(0, columnTypeString.indexOf("("));
        } else {
            dataTypeName = columnTypeString;
        }
        if (columnTypeString.contains(")")) {
            additionalInformation = StringUtils.trimToNull(columnTypeString.replaceFirst(".*\\)", ""));
        }

        return getDataType(columnTypeString, autoIncrement, dataTypeName, precision, additionalInformation);
    }

    protected DataType getDataType(String columnTypeString, Boolean autoIncrement, String dataTypeName, String precision, String additionalInformation) {
        // Translate type to database-specific type, if possible
        DataType returnTypeName = null;
        if (dataTypeName.equalsIgnoreCase("BIGINT")) {
            returnTypeName = getBigIntType();
        } else if (dataTypeName.equalsIgnoreCase("NUMBER") || dataTypeName.equalsIgnoreCase("NUMERIC")) {
            returnTypeName = getNumberType();
        } else if (dataTypeName.equalsIgnoreCase("BLOB")) {
            returnTypeName = getBlobType();
        } else if (dataTypeName.equalsIgnoreCase("BOOLEAN")) {
            returnTypeName = getBooleanType();
        } else if (dataTypeName.equalsIgnoreCase("CHAR")) {
            returnTypeName = getCharType();
        } else if (dataTypeName.equalsIgnoreCase("CLOB")) {
            returnTypeName = getClobType();
        } else if (dataTypeName.equalsIgnoreCase("CURRENCY")) {
            returnTypeName = getCurrencyType();
        } else if (dataTypeName.equalsIgnoreCase("DATE") || dataTypeName.equalsIgnoreCase(getDateType().getDataTypeName())) {
            returnTypeName = getDateType();
        } else if (dataTypeName.equalsIgnoreCase("DATETIME") || dataTypeName.equalsIgnoreCase(getDateTimeType().getDataTypeName())) {
            returnTypeName = getDateTimeType();
        } else if (dataTypeName.equalsIgnoreCase("DOUBLE")) {
            returnTypeName = getDoubleType();
        } else if (dataTypeName.equalsIgnoreCase("FLOAT")) {
            returnTypeName = getFloatType();
        } else if (dataTypeName.equalsIgnoreCase("INT")) {
            returnTypeName = getIntType();
        } else if (dataTypeName.equalsIgnoreCase("INTEGER")) {
            returnTypeName = getIntType();
        } else if (dataTypeName.equalsIgnoreCase("LONGBLOB")) {
            returnTypeName = getLongBlobType();
        } else if (dataTypeName.equalsIgnoreCase("LONGVARBINARY")) {
            returnTypeName = getBlobType();
        } else if (dataTypeName.equalsIgnoreCase("LONGVARCHAR")) {
            returnTypeName = getClobType();
        } else if (dataTypeName.equalsIgnoreCase("SMALLINT")) {
            returnTypeName = getSmallIntType();
        } else if (dataTypeName.equalsIgnoreCase("TEXT")) {
            returnTypeName = getClobType();
        } else if (dataTypeName.equalsIgnoreCase("TIME") || dataTypeName.equalsIgnoreCase(getTimeType().getDataTypeName())) {
            returnTypeName = getTimeType();
        } else if (dataTypeName.toUpperCase().contains("TIMESTAMP")) {
            returnTypeName = getDateTimeType();
        } else if (dataTypeName.equalsIgnoreCase("TINYINT")) {
            returnTypeName = getTinyIntType();
        } else if (dataTypeName.equalsIgnoreCase("UUID")) {
            returnTypeName = getUUIDType();
        } else if (dataTypeName.equalsIgnoreCase("VARCHAR")) {
            returnTypeName = getVarcharType();
        } else if (dataTypeName.equalsIgnoreCase("NVARCHAR")) {
            returnTypeName = getNVarcharType();
        } else {
            return new CustomType(columnTypeString,0,2);
        }

        if (returnTypeName == null) {
            throw new UnexpectedLiquibaseException("Could not determine " + dataTypeName + " for " + this.getClass().getName());
        }
        addPrecisionToType(precision, returnTypeName);
        returnTypeName.setAdditionalInformation(additionalInformation);

         return returnTypeName;
    }

    protected void addPrecisionToType(String precision, DataType returnTypeName) throws NumberFormatException {
        if (precision != null) {
            String[] params = precision.split(",");
            returnTypeName.setFirstParameter(params[0].trim());
            if (params.length > 1) {
                returnTypeName.setSecondParameter(params[1].trim());
            }
        }
    }


    public DataType getDataType(ColumnConfig columnConfig) {
        return getDataType(columnConfig.getType(), columnConfig.isAutoIncrement());
    }

    /**
     * Returns the actual database-specific data type to use a "date" (no time information) column.
     */
    public DateType getDateType() {
        return new DateType();
    }

    /**
     * Returns the actual database-specific data type to use a "time" column.
     */
    public TimeType getTimeType() {
        return new TimeType();
    }

    public DateTimeType getDateTimeType() {
        return new DateTimeType();
    }

    public BigIntType getBigIntType() {
        return new BigIntType();
    }

    /**
     * Returns the actual database-specific data type to use for a "char" column.
     */
    public CharType getCharType() {
        return new CharType();
    }

    /**
     * Returns the actual database-specific data type to use for a "varchar" column.
     */
    public VarcharType getVarcharType() {
        return new VarcharType();
    }

    /**
     * Returns the actual database-specific data type to use for a "varchar" column.
     */
    public NVarcharType getNVarcharType() {
        return new NVarcharType();
    }

    /**
     * Returns the actual database-specific data type to use for a "float" column.
     *
     * @return database-specific type for float
     */
    public FloatType getFloatType() {
        return new FloatType();
    }

    /**
     * Returns the actual database-specific data type to use for a "double" column.
     *
     * @return database-specific type for double
     */
    public DoubleType getDoubleType() {
        return new DoubleType();
    }

    /**
     * Returns the actual database-specific data type to use for a "int" column.
     *
     * @return database-specific type for int
     */
    public IntType getIntType() {
        return new IntType();
    }

    /**
     * Returns the actual database-specific data type to use for a "tinyint" column.
     *
     * @return database-specific type for tinyint
     */
    public TinyIntType getTinyIntType() {
        return new TinyIntType();
    }
    public SmallIntType getSmallIntType() {
    	return new SmallIntType();
    }

    public BooleanType getBooleanType() {
        return new BooleanType();
    }

    public NumberType getNumberType() {
        return new NumberType();
    }

    public CurrencyType getCurrencyType() {
        return new CurrencyType();
    }

    public UUIDType getUUIDType() {
        return new UUIDType();
    }

    public TextType getTextType() {
        return getClobType();
    }

    public ClobType getClobType() {
        return new ClobType();
    }

    public BlobType getBlobType() {
        return new BlobType();
    }

    public BlobType getLongBlobType() {
    	return getBlobType();
    }

    public String convertToDatabaseTypeString(Column referenceColumn, Database database) {

        List<Integer> noParens = Arrays.asList(
                Types.ARRAY,
                Types.BIGINT,
                Types.BINARY,
                Types.BIT,
                Types.BLOB,
                Types.BOOLEAN,
                Types.CLOB,
                Types.DATALINK,
                Types.DATE,
                Types.DISTINCT,
                Types.INTEGER,
                Types.JAVA_OBJECT,
                Types.LONGVARBINARY,
                Types.NULL,
                Types.OTHER,
                Types.REF,
                Types.SMALLINT,
                Types.STRUCT,
                Types.TIME,
                Types.TIMESTAMP,
                Types.TINYINT,
                Types.LONGVARCHAR);

        List<Integer> oneParam = Arrays.asList(
                Types.CHAR,
                -15, // Types.NCHAR in java 1.6,
                Types.VARCHAR,
                -9, //Types.NVARCHAR in java 1.6,
                Types.VARBINARY,
                Types.DOUBLE,
                Types.FLOAT
        );

        List<Integer> twoParams = Arrays.asList(
                Types.DECIMAL,
                Types.NUMERIC,
                Types.REAL
        );

        String translatedTypeName = referenceColumn.getTypeName();
        if (database instanceof PostgresDatabase) {
            if ("bpchar".equals(translatedTypeName)) {
                translatedTypeName = "char";
            }
        }

        if (database instanceof HsqlDatabase || database instanceof H2Database || database instanceof DerbyDatabase || database instanceof DB2Database) {
            if (referenceColumn.getDataType() == Types.FLOAT || referenceColumn.getDataType() == Types.DOUBLE) {
                return "float";
            }
        }

        if (database instanceof InformixDatabase) {
            /*
             * rs.getInt("DATA_TYPE") returns 1 (Types.CHAR) for
             * interval types (bug in JDBC driver?)
             * So if you comment this out, the the columnsize will be appended
             * and the type becomes: "INTERVAL HOUR TO FRACTION(3)(2413)"
             */
        	if (translatedTypeName.toUpperCase().startsWith("INTERVAL")) {
        		return translatedTypeName;
        	}
        	if (referenceColumn.getDataType() == Types.REAL) {
        		return "SMALLFLOAT";
        	}
        }

        String dataType;
        if (noParens.contains(referenceColumn.getDataType())) {
	        dataType = translatedTypeName;
        } else if (oneParam.contains(referenceColumn.getDataType())) {
            if (database instanceof PostgresDatabase && translatedTypeName.equalsIgnoreCase("TEXT")) {
                return translatedTypeName;
            } else if (database instanceof MSSQLDatabase && translatedTypeName.equals("uniqueidentifier")) {
                return translatedTypeName;
            } else if (database instanceof MySQLDatabase && (translatedTypeName.startsWith("enum(") || translatedTypeName.startsWith("set("))                   ) {
              return translatedTypeName;
            } else if (database instanceof OracleDatabase && (translatedTypeName.equals("VARCHAR2"))                   ) {
              return translatedTypeName+"("+referenceColumn.getColumnSize()+" "+referenceColumn.getLengthSemantics()+")";
            } else if (database instanceof MySQLDatabase && translatedTypeName.equalsIgnoreCase("DOUBLE")) {
              return translatedTypeName;
            } else if (database instanceof MySQLDatabase && translatedTypeName.equalsIgnoreCase("DOUBLE PRECISION")) {
                return translatedTypeName;
            }
            dataType = translatedTypeName+"("+referenceColumn.getColumnSize()+")";
        } else if (twoParams.contains(referenceColumn.getDataType())) {
            if (database instanceof PostgresDatabase && referenceColumn.getColumnSize() == 131089 ) {
                dataType = "DECIMAL";
            } else if (database instanceof MSSQLDatabase && translatedTypeName.toLowerCase().contains("money")) {
                dataType = translatedTypeName.toUpperCase();
            } else {
	            dataType = translatedTypeName;
	            if (referenceColumn.isInitPrecision()) {
		            dataType += "(" + referenceColumn.getColumnSize() + "," + referenceColumn.getDecimalDigits() + ")";
	            }
            }
        } else {
            LogFactory.getLogger().warning("Unknown Data Type: "+referenceColumn.getDataType()+" ("+referenceColumn.getTypeName()+").  Assuming it does not take parameters");
            dataType = referenceColumn.getTypeName();
        }
        return dataType;

    }
}