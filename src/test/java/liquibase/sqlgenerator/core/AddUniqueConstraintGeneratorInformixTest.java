package liquibase.sqlgenerator.core;

import liquibase.database.Database;
import liquibase.database.core.InformixDatabase;

public class AddUniqueConstraintGeneratorInformixTest extends
		AddUniqueConstraintGeneratorTest {

	public AddUniqueConstraintGeneratorInformixTest() throws Exception {
		this(new AddUniqueConstraintGeneratorInformix());
	}

	public AddUniqueConstraintGeneratorInformixTest(AddUniqueConstraintGeneratorInformix generatorUnderTest) throws Exception {
		super(generatorUnderTest);
	}

	@Override
	protected boolean shouldBeImplementation(Database database) {
        return (database instanceof InformixDatabase);
	}
}
