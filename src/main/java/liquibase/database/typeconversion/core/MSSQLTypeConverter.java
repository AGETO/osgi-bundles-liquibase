package liquibase.database.typeconversion.core;

import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.structure.type.*;

import java.text.ParseException;

public class MSSQLTypeConverter extends AbstractTypeConverter {

    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    public boolean supports(Database database) {
        return database instanceof MSSQLDatabase;
    }


    @Override
    public Object convertDatabaseValueToObject(Object defaultValue, int dataType, int columnSize, int decimalDigits, Database database) throws ParseException {
        if (defaultValue == null) {
            return null;
        }

        if (defaultValue instanceof String) {
            if (((String) defaultValue).startsWith("('")) {
                defaultValue = ((String) defaultValue).replaceFirst("^\\('", "").replaceFirst("'\\)$", "");
            } else if (((String) defaultValue).startsWith("((")) {
                defaultValue = ((String) defaultValue).replaceFirst("^\\(\\(", "").replaceFirst("\\)\\)$", "");
            }
        }

        defaultValue = super.convertDatabaseValueToObject(defaultValue, dataType, columnSize, decimalDigits, database);

        return defaultValue;
    }


    @Override
    public DataType getDataType(String columnTypeString, Boolean autoIncrement) {
        DataType type = super.getDataType(columnTypeString, autoIncrement);
        if (autoIncrement != null && autoIncrement) {
            type.setDataTypeName(type.getDataTypeName().replaceFirst(" identity$", ""));
        }
        return type;
    }

    /**
     * Extension of super.getDataType(String columnTypeString, Boolean autoIncrement, String dataTypeName, String precision)<br>
     * Contains definition of Oracle's data-types
     * */
    @Override
    protected DataType getDataType(String columnTypeString, Boolean autoIncrement, String dataTypeName, String precision) {
        // Try to define data type by searching of common standard types
        DataType returnTypeName = super.getDataType(columnTypeString, autoIncrement, dataTypeName, precision);
        // If we found CustomType (it means - nothing compatible) then search for oracle types
        if (returnTypeName instanceof CustomType) {
            boolean returnTypeChanged=false;
            if (columnTypeString.toUpperCase().startsWith("NVARCHAR")) {
                returnTypeName = new VarcharType("NVARCHAR");
                returnTypeChanged=true;
            } else if(columnTypeString.toUpperCase().startsWith("NCHAR")) {
                returnTypeName= new CharType("NCHAR");
                returnTypeChanged=true;
            }

            if(returnTypeChanged)
                addPrecisionToType(precision, returnTypeName);
        }
        
        return returnTypeName;
    }

    @Override
    public DateType getDateType() {
        return new DateType("SMALLDATETIME");
    }

    @Override
    public TimeType getTimeType() {
        return new TimeType("DATETIME");
    }

    @Override
    public BooleanType getBooleanType() {
        return new BooleanType.NumericBooleanType("BIT");
    }

    @Override
    public CurrencyType getCurrencyType() {
        return new CurrencyType("MONEY");
    }

    @Override
    public UUIDType getUUIDType() {
        return new UUIDType("UNIQUEIDENTIFIER");
    }

    @Override
    public ClobType getClobType() {
        return new ClobType("TEXT");
    }

    @Override
    public BlobType getBlobType() {
        return new BlobType("IMAGE");
    }


}
