package liquibase.change.core;

import liquibase.change.AbstractChange;
import liquibase.change.ChangeMetaData;
import liquibase.database.Database;
import liquibase.sql.Sql;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RuntimeStatement;
import liquibase.util.StringUtils;

public class StopChange extends AbstractChange {

    private String message ="Stop command in changelog file";

    public StopChange() {
        super("stop", "Stop Execution", ChangeMetaData.PRIORITY_DEFAULT);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = StringUtils.trimToNull(message);
    }

    public SqlStatement[] generateStatements(Database database) {
        return new SqlStatement[] { new RuntimeStatement() {
            @Override
            public Sql[] generate(Database database) {
                throw new StopChangeException(getMessage());
            }
        }};

    }

    public String getConfirmationMessage() {
        return "Changelog Execution Stopped";
    }

    public static class StopChangeException extends RuntimeException {
        public StopChangeException(String message) {
            super(message);
        }
    }
}
