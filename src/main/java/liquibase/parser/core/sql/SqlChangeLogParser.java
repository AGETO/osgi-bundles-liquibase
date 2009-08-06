package liquibase.parser.core.sql;

import liquibase.change.core.RawSQLChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.ChangeLogParseException;
import liquibase.parser.ChangeLogParser;
import liquibase.resource.ResourceAccessor;
import liquibase.util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class SqlChangeLogParser implements ChangeLogParser {
    public String[] getValidFileExtensions() {
        return new String [] {
                "sql"
        };
    }

    public DatabaseChangeLog parse(String physicalChangeLogLocation, Map<String, Object> changeLogParameters, ResourceAccessor resourceAccessor) throws ChangeLogParseException {

        RawSQLChange change = new RawSQLChange();

        try {
            InputStream sqlStream = resourceAccessor.getResourceAsStream(physicalChangeLogLocation);
            String sql = StreamUtil.getStreamContents(sqlStream, null);
            change.setSql(sql);
        } catch (IOException e) {
            throw new ChangeLogParseException(e);
        }
        change.setFileOpener(resourceAccessor);
        change.setSplitStatements(false);
        change.setStripComments(false);

        ChangeSet changeSet = new ChangeSet("raw", "includeAll", false, false, physicalChangeLogLocation, physicalChangeLogLocation, null, null, false);
        changeSet.addChange(change);

        DatabaseChangeLog changeLog = new DatabaseChangeLog(physicalChangeLogLocation);
        changeLog.addChangeSet(changeSet);

        return changeLog;
    }
}
