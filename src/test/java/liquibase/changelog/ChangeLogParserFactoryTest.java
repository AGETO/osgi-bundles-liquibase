package liquibase.changelog;

import liquibase.exception.ChangeLogParseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.parser.core.sql.SqlChangeLogParser;
import liquibase.resource.ResourceAccessor;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class ChangeLogParserFactoryTest {
    @Before
    public void setup() {
        ChangeLogParserFactory.reset();

    }

    @Test
    public void getInstance() {
        assertNotNull(ChangeLogParserFactory.getInstance());

        assertTrue(ChangeLogParserFactory.getInstance() == ChangeLogParserFactory.getInstance());
    }

    @Test
    public void register() {
        ChangeLogParserFactory.getInstance().getParsers().clear();

        assertEquals(0, ChangeLogParserFactory.getInstance().getParsers().size());

        ChangeLogParserFactory.getInstance().register(new MockChangeLogParser());

        assertEquals(1, ChangeLogParserFactory.getInstance().getParsers().size());
    }

    @Test
    public void unregister_instance() {
        ChangeLogParserFactory factory = ChangeLogParserFactory.getInstance();

        factory.getParsers().clear();

        assertEquals(0, factory.getParsers().size());

        ChangeLogParser mockChangeLogParser = new MockChangeLogParser();

        factory.register(new XMLChangeLogSAXParser());
        factory.register(mockChangeLogParser);
        factory.register(new SqlChangeLogParser());

        assertEquals(3, factory.getParsers().size());

        factory.unregister(mockChangeLogParser);
        assertEquals(2, factory.getParsers().size());
    }

    @Test
    public void getParser_byExtension() {
        ChangeLogParserFactory.getInstance().getParsers().clear();

        XMLChangeLogSAXParser xmlChangeLogParser = new XMLChangeLogSAXParser();
        ChangeLogParserFactory.getInstance().register(xmlChangeLogParser);
        ChangeLogParserFactory.getInstance().register(new SqlChangeLogParser());

        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser("xml");

        assertNotNull(parser);
        assertTrue(xmlChangeLogParser == parser);
    }

    @Test
    public void getParser_byFile() {
        ChangeLogParserFactory.getInstance().getParsers().clear();

        XMLChangeLogSAXParser xmlChangeLogParser = new XMLChangeLogSAXParser();
        ChangeLogParserFactory.getInstance().register(xmlChangeLogParser);
        ChangeLogParserFactory.getInstance().register(new SqlChangeLogParser());

        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser("path/to/a/file.xml");

        assertNotNull(parser);
        assertTrue(xmlChangeLogParser == parser);
    }

    @Test
    public void getParser_noneMatching() {
        ChangeLogParserFactory.getInstance().getParsers().clear();

        ChangeLogParserFactory.getInstance().getParsers().clear();

        XMLChangeLogSAXParser xmlChangeLogParser = new XMLChangeLogSAXParser();
        ChangeLogParserFactory.getInstance().register(xmlChangeLogParser);
        ChangeLogParserFactory.getInstance().register(new SqlChangeLogParser());

        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser("badextension");

        assertNull(parser);
    }

    @Test
    public void reset() {
        ChangeLogParserFactory instance1 = ChangeLogParserFactory.getInstance();
        ChangeLogParserFactory.reset();
        assertFalse(instance1 == ChangeLogParserFactory.getInstance());
    }

    @SuppressWarnings("unchecked")
	@Test
    public void builtInGeneratorsAreFound() {
        Map parsers = ChangeLogParserFactory.getInstance().getParsers();
        assertTrue(parsers.size() > 0);
    }

    private static class MockChangeLogParser implements ChangeLogParser {

        public DatabaseChangeLog parse(String physicalChangeLogLocation, Map<String, Object> changeLogParameters, ResourceAccessor resourceAccessor) throws ChangeLogParseException {
            return null;
        }

        public String[] getValidFileExtensions() {
            return new String[] {
                    "test"
            };
        }
    }
}
