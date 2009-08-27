package liquibase.database.structure.type;

public class CurrencyType  extends DataType {
    public CurrencyType() {
        super("DECIMAL",0,0);
    }

    public CurrencyType(String dataTypeName) {
        super(dataTypeName,0,0);
    }

}
