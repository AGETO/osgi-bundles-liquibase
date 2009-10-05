package liquibase.changelog.visitor;

import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import static org.easymock.classextension.EasyMock.*;
import org.junit.Test;

public class UpdateVisitorTest {

    @Test
    public void visit_unrun() throws Exception {
        Database database = createMock(Database.class);

        ChangeSet changeSet = createMock(ChangeSet.class);
        expect(changeSet.execute(database)).andReturn(ChangeSet.ExecType.EXECUTED);


        expect(database.getRunStatus(changeSet)).andReturn(ChangeSet.RunStatus.NOT_RAN);


        database.markChangeSetExecStatus(changeSet, ChangeSet.ExecType.EXECUTED);
        expectLastCall();

        database.commit();
        expectLastCall();


        replay(changeSet);
        replay(database);

        UpdateVisitor visitor = new UpdateVisitor(database);
        visitor.visit(changeSet, database);

        verify(database);
        verify(changeSet);
    }

}
