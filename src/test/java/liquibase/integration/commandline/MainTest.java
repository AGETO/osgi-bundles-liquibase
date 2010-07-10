package liquibase.integration.commandline;

import liquibase.exception.CommandLineParsingException;
import liquibase.integration.commandline.Main;
import liquibase.util.StringUtils;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Properties;


/**
 * Tests for {@link Main}
 */
public class MainTest {

    @Test
    public void migrateWithAllParameters() throws Exception {
        String[] args = new String[]{
                "--driver=DRIVER",
                "--username=USERNAME",
                "--password=PASSWORD",
                "--url=URL",
                "--changeLogFile=FILE",
                "--classpath=CLASSPATH;CLASSPATH2",
                "--contexts=CONTEXT1,CONTEXT2",
                "--promptForNonLocalDatabase=true",
                "update",
        };

        Main cli = new Main();
        cli.parseOptions(args);

        assertEquals("DRIVER", cli.driver);
        assertEquals("USERNAME", cli.username);
        assertEquals("PASSWORD", cli.password);
        assertEquals("URL", cli.url);
        assertEquals("FILE", cli.changeLogFile);
        assertEquals("CLASSPATH;CLASSPATH2", cli.classpath);
        assertEquals("CONTEXT1,CONTEXT2", cli.contexts);
        assertEquals(Boolean.TRUE, cli.promptForNonLocalDatabase);
        assertEquals("update", cli.command);
    }

    @Test
    public void falseBooleanParameters() throws Exception {
        String[] args = new String[]{
                "--promptForNonLocalDatabase=false",
                "update",
        };

        Main cli = new Main();
        cli.parseOptions(args);

        assertEquals(Boolean.FALSE, cli.promptForNonLocalDatabase);
        assertEquals("update", cli.command);

    }

    @Test
    public void convertMigrateToUpdate() throws Exception {
        String[] args = new String[]{
                "--promptForNonLocalDatabase=false",
                "migrate",
        };

        Main cli = new Main();
        cli.parseOptions(args);

        assertEquals("update", cli.command);

    }

    @Test
    public void trueBooleanParameters() throws Exception {
        String[] args = new String[]{
                "--promptForNonLocalDatabase=true",
                "update",
        };

        Main cli = new Main();
        cli.parseOptions(args);

        assertEquals(Boolean.TRUE, cli.promptForNonLocalDatabase);
        assertEquals("update", cli.command);

    }

    @Test(expected = CommandLineParsingException.class)
    public void parameterWithoutDash() throws Exception {
        String[] args = new String[]{
                "promptForNonLocalDatabase=true",
                "update",
        };

        Main cli = new Main();
        cli.parseOptions(args);
    }

    @Test(expected = CommandLineParsingException.class)
    public void unknownParameter() throws Exception {
        String[] args = new String[]{
                "--promptForNonLocalDatabase=true",
                "--badParam=here",
                "migrate",
        };

        Main cli = new Main();
        cli.parseOptions(args);
    }

    @Test(expected = CommandLineParsingException.class)
    public void configureNonExistantClassloaderLocation() throws Exception {
        Main cli = new Main();
        cli.classpath = "badClasspathLocation";
        cli.configureClassLoader();
    }

    @Test
    public void windowsConfigureClassLoaderLocation() throws Exception {
        Main cli = new Main();

        if (cli.isWindows())
        {
          System.setProperty("os.name", "Windows XP");
          cli.classpath = "c:\\;c:\\windows\\";
          cli.applyDefaults();
          cli.configureClassLoader();

          URL[] classloaderURLs = ((URLClassLoader) cli.classLoader).getURLs();
          assertEquals(2, classloaderURLs.length);
          assertEquals("file:/c:/", classloaderURLs[0].toExternalForm());
          assertEquals("file:/c:/windows/", classloaderURLs[1].toExternalForm());
        }
    }

    @Test
    public void unixConfigureClassLoaderLocation() throws Exception {
        Main cli = new Main();

        if (!cli.isWindows())
        {
          System.setProperty("os.name", "Linux");
          cli.classpath = "/tmp:/";
          cli.applyDefaults();

          cli.configureClassLoader();

          URL[] classloaderURLs = ((URLClassLoader) cli.classLoader).getURLs();
          assertEquals(2, classloaderURLs.length);
          assertEquals("file:/tmp/", classloaderURLs[0].toExternalForm());
          assertEquals("file:/", classloaderURLs[1].toExternalForm());
        }
    }

    @Test
    public void propertiesFileWithNoOtherArgs() throws Exception {
        Main cli = new Main();

        Properties props = new Properties();
        props.setProperty("driver", "DRIVER");
        props.setProperty("username", "USERNAME");
        props.setProperty("password", "PASSWD");
        props.setProperty("url", "URL");
        props.setProperty("changeLogFile", "FILE");
        props.setProperty("classpath", "CLASSPAHT");
        props.setProperty("contexts", "CONTEXTS");
        props.setProperty("promptForNonLocalDatabase", "TRUE");

        ByteArrayOutputStream propFile = new ByteArrayOutputStream();
        props.store(propFile, "");

        cli.parsePropertiesFile(new ByteArrayInputStream(propFile.toByteArray()));

        assertEquals("DRIVER", cli.driver);
        assertEquals("USERNAME", cli.username);
        assertEquals("PASSWD", cli.password);
        assertEquals("URL", cli.url);
        assertEquals("FILE", cli.changeLogFile);
        assertEquals("CLASSPAHT", cli.classpath);
        assertEquals("CONTEXTS", cli.contexts);
        assertEquals(Boolean.TRUE, cli.promptForNonLocalDatabase);

    }

    @Test
    public void propertiesFileWithOtherArgs() throws Exception {
        Main cli = new Main();
        cli.username = "PASSED USERNAME";
        cli.password = "PASSED PASSWD";


        Properties props = new Properties();
        props.setProperty("driver", "DRIVER");
        props.setProperty("username", "USERNAME");
        props.setProperty("password", "PASSWD");
        props.setProperty("url", "URL");
        props.setProperty("changeLogFile", "FILE");
        props.setProperty("classpath", "CLASSPAHT");
        props.setProperty("contexts", "CONTEXTS");
        props.setProperty("promptForNonLocalDatabase", "TRUE");

        ByteArrayOutputStream propFile = new ByteArrayOutputStream();
        props.store(propFile, "");

        cli.parsePropertiesFile(new ByteArrayInputStream(propFile.toByteArray()));

        assertEquals("DRIVER", cli.driver);
        assertEquals("PASSED USERNAME", cli.username);
        assertEquals("PASSED PASSWD", cli.password);
        assertEquals("URL", cli.url);
        assertEquals("FILE", cli.changeLogFile);
        assertEquals("CLASSPAHT", cli.classpath);
        assertEquals("CONTEXTS", cli.contexts);
        assertEquals(Boolean.TRUE, cli.promptForNonLocalDatabase);

    }

    @Test
    public void applyDefaults() {
        Main cli = new Main();

        cli.promptForNonLocalDatabase = Boolean.TRUE;
        cli.applyDefaults();
        assertEquals(Boolean.TRUE, cli.promptForNonLocalDatabase);

        cli.promptForNonLocalDatabase = Boolean.FALSE;
        cli.applyDefaults();
        assertEquals(Boolean.FALSE, cli.promptForNonLocalDatabase);

        cli.promptForNonLocalDatabase = null;
        cli.applyDefaults();
        assertEquals(Boolean.FALSE, cli.promptForNonLocalDatabase);

    }

    @Test(expected = CommandLineParsingException.class)
    public void propertiesFileWithBadArgs() throws Exception {
        Main cli = new Main();

        Properties props = new Properties();
        props.setProperty("driver", "DRIVER");
        props.setProperty("username", "USERNAME");
        props.setProperty("badArg", "ARG");

        ByteArrayOutputStream propFile = new ByteArrayOutputStream();
        props.store(propFile, "");

        cli.parsePropertiesFile(new ByteArrayInputStream(propFile.toByteArray()));
    }

    @Test
    public void checkSetup() {
        Main cli = new Main();
        assertTrue(cli.checkSetup().size() > 0);

        cli.driver = "driver";
        cli.username = "username";
        cli.password = "pwd";
        cli.url = "url";
        cli.changeLogFile = "file";
        cli.classpath = "classpath";

        assertTrue(cli.checkSetup().size() > 0);

        cli.command = "BadCommand";
        assertTrue(cli.checkSetup().size() > 0);

        cli.command = "migrate";
        assertEquals(0, cli.checkSetup().size());
    }

    @Test
    public void printHelp() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Main cli = new Main();
        cli.printHelp(new PrintStream(stream));

        BufferedReader reader = new BufferedReader(new StringReader(new String(stream.toByteArray())));
        String line;
        while ((line = reader.readLine()) != null) {
            //noinspection MagicNumber
            if (line.length() > 80) {
                fail("'" + line + "' is longer than 80 chars");
            }
        }
    }

    @Test
    public void tag() throws Exception {
        String[] args = new String[]{
                "--driver=DRIVER",
                "--username=USERNAME",
                "--password=PASSWORD",
                "--url=URL",
                "--changeLogFile=FILE",
                "--classpath=CLASSPATH;CLASSPATH2",
                "--contexts=CONTEXT1,CONTEXT2",
                "tag", "TagHere"
        };

        Main cli = new Main();
        cli.parseOptions(args);

        assertEquals("DRIVER", cli.driver);
        assertEquals("USERNAME", cli.username);
        assertEquals("PASSWORD", cli.password);
        assertEquals("URL", cli.url);
        assertEquals("FILE", cli.changeLogFile);
        assertEquals("CLASSPATH;CLASSPATH2", cli.classpath);
        assertEquals("CONTEXT1,CONTEXT2", cli.contexts);
        assertEquals("tag", cli.command);
        assertEquals("TagHere", cli.commandParams.iterator().next());
    }

    @Test
    public void migrateWithEqualsInParams() throws Exception {
        String url = "dbc:sqlserver://127.0.0.1;DatabaseName=dev_nn;user=ffdatabase;password=p!88worD";
        String[] args = new String[]{
                "--url=" + url,
                "migrate",
        };

        Main cli = new Main();
        cli.parseOptions(args);

        assertEquals(url, cli.url);
    }
    
    @Test
    public void fixArgs() {
        Main liquibase = new Main();
        String[] fixedArgs = liquibase.fixupArgs(new String[] {"--defaultsFile","liquibase.properties", "migrate"});
        assertEquals("--defaultsFile=liquibase.properties migrate", StringUtils.join(Arrays.asList(fixedArgs), " "));

        fixedArgs = liquibase.fixupArgs(new String[] {"--defaultsFile=liquibase.properties", "migrate"});
        assertEquals("--defaultsFile=liquibase.properties migrate", StringUtils.join(Arrays.asList(fixedArgs), " "));

        fixedArgs = liquibase.fixupArgs(new String[] {"--driver=DRIVER",
                "--username=USERNAME",
                "--password=PASSWORD",
                "--url=URL",
                "--changeLogFile=FILE",
                "--classpath=CLASSPATH;CLASSPATH2",
                "--contexts=CONTEXT1,CONTEXT2",
                "--promptForNonLocalDatabase=true",
                "migrate"
        });
        assertEquals("--driver=DRIVER --username=USERNAME --password=PASSWORD --url=URL --changeLogFile=FILE --classpath=CLASSPATH;CLASSPATH2 --contexts=CONTEXT1,CONTEXT2 --promptForNonLocalDatabase=true migrate", StringUtils.join(Arrays.asList(fixedArgs), " "));
    }

    @Test
    public void testVersionArg() throws IOException, CommandLineParsingException {
        Main.main(new String[] {"--version"});

    }
}
