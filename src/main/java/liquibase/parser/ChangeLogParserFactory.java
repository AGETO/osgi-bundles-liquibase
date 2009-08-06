package liquibase.parser;

import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.servicelocator.ServiceLocator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeLogParserFactory {

    private static ChangeLogParserFactory instance;

    private Map<String, ChangeLogParser> parsers = new HashMap<String, ChangeLogParser>();


    public static void reset() {
        instance = new ChangeLogParserFactory();
    }

    public static ChangeLogParserFactory getInstance() {
        if (instance == null) {
             instance = new ChangeLogParserFactory();
        }
        return instance;
    }

    private ChangeLogParserFactory() {
        Class<? extends ChangeLogParser>[] classes;
        try {
            classes = ServiceLocator.getInstance().findClasses(ChangeLogParser.class);

            for (Class<? extends ChangeLogParser> clazz : classes) {
                    register((ChangeLogParser) clazz.getConstructor().newInstance());
            }
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }

    }

    public Map<String, ChangeLogParser> getParsers() {
        return parsers;
    }

    public ChangeLogParser getParser(String fileNameOrExtension) {
        fileNameOrExtension = fileNameOrExtension.replaceAll(".*\\.", ""); //just need the extension
        return parsers.get(fileNameOrExtension);
    }

    public void register(ChangeLogParser changeLogParser) {
        for (String extension : changeLogParser.getValidFileExtensions()) {
            parsers.put(extension, changeLogParser);
        }
    }

    public void unregister(ChangeLogParser changeLogParser) {
        List<Map.Entry<String, ChangeLogParser>> entrysToRemove = new ArrayList<Map.Entry<String, ChangeLogParser>>();
        for (Map.Entry<String, ChangeLogParser> entry : parsers.entrySet()) {
            if (entry.getValue().equals(changeLogParser)) {
                entrysToRemove.add(entry);
            }
        }

        for (Map.Entry<String, ChangeLogParser> entry : entrysToRemove) {
            parsers.remove(entry.getKey());
        }

    }
}
