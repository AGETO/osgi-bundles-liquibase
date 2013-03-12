package liquibase.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.structure.DatabaseObject;
import liquibase.exception.RollbackImpossibleException;
import liquibase.exception.SetupException;
import liquibase.exception.UnsupportedChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.logging.LogFactory;
import liquibase.resource.ResourceAccessor;
import liquibase.serializer.core.string.StringChangeLogSerializer;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

/**
 * Standard superclass for Changes to implement. This is a <i>skeletal implementation</i>,
 * as defined in Effective Java#16.
 *
 * @see Change
 */
public abstract class AbstractChange implements Change {

    @ChangeProperty(includeInSerialization = false)
    private ChangeMetaData changeMetaData;

    @ChangeProperty(includeInSerialization = false)
    private ResourceAccessor resourceAccessor;

    @ChangeProperty(includeInSerialization = false)
    private ChangeSet changeSet;

    @ChangeProperty(includeInSerialization = false)
    private ChangeLogParameters changeLogParameters;


    /**
     * Constructor with tag name and name
     *
     * @param changeName        the tag name for this change
     * @param changeDescription the name for this change
     */
    protected AbstractChange(String changeName, String changeDescription, int priority) {
        this.changeMetaData = new ChangeMetaData(changeName, changeDescription, priority);
    }

    public ChangeMetaData getChangeMetaData() {
        return changeMetaData;
    }

    protected void setPriority(int newPriority) {
        this.changeMetaData.setPriority(newPriority);
    }

    public ChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    public boolean requiresUpdatedDatabaseMetadata(Database database) {
        for (SqlStatement statement : generateStatements(database)) {
            if (SqlGeneratorFactory.getInstance().requiresCurrentDatabaseMetadata(statement, database)) {
                return true;
            }
        }
        return false;
    }

    public boolean supports(Database database) {
        for (SqlStatement statement : generateStatements(database)) {
            if (!SqlGeneratorFactory.getInstance().supports(statement, database)) {
                return false;
            }
        }
        return true;
    }

    public Warnings warn(Database database) {
        Warnings warnings = new Warnings();
        for (SqlStatement statement : generateStatements(database)) {
            if (SqlGeneratorFactory.getInstance().supports(statement, database)) {
                warnings.addAll(SqlGeneratorFactory.getInstance().warn(statement, database));
            }
        }

        return warnings;
    }

    public ValidationErrors validate(Database database) {
        ValidationErrors changeValidationErrors = new ValidationErrors();
        for (SqlStatement statement : generateStatements(database)) {
            boolean supported = SqlGeneratorFactory.getInstance().supports(statement, database);
            if (!supported) {
                if (statement.skipOnUnsupported()) {
                    LogFactory.getLogger().info(getChangeMetaData().getName()+" is not supported on "+database.getTypeName()+" but will continue");
                } else {
                    changeValidationErrors.addError(getChangeMetaData().getName()+" is not supported on "+database.getTypeName());
                }
            } else {
                changeValidationErrors.addAll(SqlGeneratorFactory.getInstance().validate(statement, database));
            }
        }

        return changeValidationErrors;
    }

    /*
    * Skipped by this skeletal implementation
    *
    * @see liquibase.change.Change#generateStatements(liquibase.database.Database)
    */

    /**
     * @see liquibase.change.Change#generateRollbackStatements(liquibase.database.Database)
     */
    public SqlStatement[] generateRollbackStatements(Database database) throws UnsupportedChangeException, RollbackImpossibleException {
        return generateRollbackStatementsFromInverse(database);
    }

    /**
     * @see Change#supportsRollback(liquibase.database.Database)
     * @param database
     */
    public boolean supportsRollback(Database database) {
        return createInverses() != null;
    }

    /**
     * @see liquibase.change.Change#generateCheckSum()
     */
    public CheckSum generateCheckSum() {
        return CheckSum.compute(new StringChangeLogSerializer().serialize(this));
    }

    //~ ------------------------------------------------------------------------------- private methods
    /*
     * Generates rollback statements from the inverse changes returned by createInverses()
     *
     * @param database the target {@link Database} associated to this change's rollback statements
     * @return an array of {@link String}s containing the rollback statements from the inverse changes
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     * @throws RollbackImpossibleException if rollback is not supported for this change
     */
    private SqlStatement[] generateRollbackStatementsFromInverse(Database database) throws UnsupportedChangeException, RollbackImpossibleException {
        Change[] inverses = createInverses();
        if (inverses == null) {
            throw new RollbackImpossibleException("No inverse to " + getClass().getName() + " created");
        }

        List<SqlStatement> statements = new ArrayList<SqlStatement>();

        for (Change inverse : inverses) {
            statements.addAll(Arrays.asList(inverse.generateStatements(database)));
        }

        return statements.toArray(new SqlStatement[statements.size()]);
    }

    /*
     * Create inverse changes that can roll back this change. This method is intended
     * to be overriden by the subclasses that can create inverses.
     *
     * @return an array of {@link Change}s containing the inverse
     *         changes that can roll back this change
     */
    protected Change[] createInverses() {
        return null;
    }

    /**
     * Default implementation that stores the file opener provided when the
     * Change was created.
     */
    public void setResourceAccessor(ResourceAccessor resourceAccessor) {
        this.resourceAccessor = resourceAccessor;
    }

    /**
     * Returns the FileOpen as provided by the creating ChangeLog.
     *
     * @return The file opener
     */
    public ResourceAccessor getResourceAccessor() {
        return resourceAccessor;
    }

    /**
     * Most Changes don't need to do any setup.
     * This implements a no-op
     */
    public void init() throws SetupException {

    }

    public Set<DatabaseObject> getAffectedDatabaseObjects(Database database) {
        Set<DatabaseObject> affectedObjects = new HashSet<DatabaseObject>();
        for (SqlStatement statement : generateStatements(database)) {
            affectedObjects.addAll(SqlGeneratorFactory.getInstance().getAffectedDatabaseObjects(statement, database));
        }

        return affectedObjects;
    }

    protected ChangeLogParameters getChangeLogParameters() {
        return changeLogParameters;
    }

    public void setChangeLogParameters(ChangeLogParameters changeLogParameters) {
        this.changeLogParameters = changeLogParameters;
    }

}
