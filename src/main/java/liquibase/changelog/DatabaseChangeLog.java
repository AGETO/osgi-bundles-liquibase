package liquibase.changelog;

import liquibase.changelog.filter.DbmsChangeSetFilter;
import liquibase.changelog.visitor.ValidatingVisitor;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.exception.ValidationFailedException;
import liquibase.precondition.Conditional;
import liquibase.precondition.core.PreconditionContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the information stored in the change log XML file.
 */
public class DatabaseChangeLog implements Comparable<DatabaseChangeLog>, Conditional {
    private PreconditionContainer preconditionContainer = new PreconditionContainer();
    private String physicalFilePath;
    private String logicalFilePath;

    private List<ChangeSet> changeSets = new ArrayList<ChangeSet>();

    public DatabaseChangeLog(String physicalFilePath) {
        this.physicalFilePath = physicalFilePath;
    }

    public PreconditionContainer getPreconditions() {
        return preconditionContainer;
    }

    public void setPreconditions(PreconditionContainer precond) {
        preconditionContainer = precond;
    }

    public String getPhysicalFilePath() {
        return physicalFilePath;
    }

    public void setPhysicalFilePath(String physicalFilePath) {
        this.physicalFilePath = physicalFilePath;
    }

    public String getLogicalFilePath() {
        if (logicalFilePath == null) {
            return physicalFilePath;
        }
        return logicalFilePath;
    }

    public void setLogicalFilePath(String logicalFilePath) {
        this.logicalFilePath = logicalFilePath;
    }

    public String getFilePath() {
        if (logicalFilePath == null) {
            return physicalFilePath;
        } else {
            return logicalFilePath;
        }
    }

    @Override
    public String toString() {
        return getFilePath();
    }

    public int compareTo(DatabaseChangeLog o) {
        return getFilePath().compareTo(o.getFilePath());
    }


    public ChangeSet getChangeSet(String path, String author, String id) {
        for (ChangeSet changeSet : changeSets) {
            if (changeSet.getFilePath().equals(path)
                    && changeSet.getAuthor().equals(author)
                    && changeSet.getId().equals(id)) {
                return changeSet;
            }
        }

        return null;
    }

    public List<ChangeSet> getChangeSets() {
        return changeSets;
    }

    public void addChangeSet(ChangeSet changeSet) {
        this.changeSets.add(changeSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DatabaseChangeLog that = (DatabaseChangeLog) o;

        return getFilePath().equals(that.getFilePath());

    }

    @Override
    public int hashCode() {
        return getFilePath().hashCode();
    }

    /**
     * Checks changelogs for bad MD5Sums and preconditions before attempting a migration
     */
    public void validate(Database database) throws LiquibaseException {

        ChangeLogIterator logIterator = new ChangeLogIterator(this, new DbmsChangeSetFilter(database));

        ValidatingVisitor validatingVisitor = new ValidatingVisitor(database.getRanChangeSetList());
        validatingVisitor.validate(database, this);
        logIterator.run(validatingVisitor, database);

        if (!validatingVisitor.validationPassed()) {
            throw new ValidationFailedException(validatingVisitor);
        }
    }

}
