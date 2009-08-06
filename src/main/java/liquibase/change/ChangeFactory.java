package liquibase.change;

import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.servicelocator.ServiceLocator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for constructing the correct liquibase.change.Change implementation based on the tag name.
 * It is currently implemented by a static array of Change implementations, although that may change in
 * later revisions.  The best way to get an instance of ChangeFactory is off the Liquibase.getChangeFactory() method.
 *
 * @see liquibase.change.Change
 */
public class ChangeFactory {

    private static ChangeFactory instance;

    private Map<String, Class<? extends Change>> registry = new ConcurrentHashMap<String, Class<? extends Change>>();

    private ChangeFactory() {
        Class<? extends Change>[] classes;
        try {
            classes = ServiceLocator.getInstance().findClasses(Change.class);

            for (Class<? extends Change> clazz : classes) {
                //noinspection unchecked
                register(clazz);
            }

        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }

    }

    /**
     * Return singleton SqlGeneratorFactory
     */
    public static ChangeFactory getInstance() {
        if (instance == null) {
             instance = new ChangeFactory();
        }
        return instance;
    }

    public static void reset() {
        instance = new ChangeFactory();
    }


    public void register(Class<? extends Change> changeClass) {
        try {
            registry.put(changeClass.newInstance().getChangeMetaData().getName(), changeClass);
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    public void unregister(String name) {
        registry.remove(name);
    }

    public Map<String, Class<? extends Change>> getRegistry() {
        return registry;
    }

    public Change create(String name) {
        Class<? extends Change> aClass = registry.get(name);

        if (aClass == null) {
            return null;
        }

        try {
            return aClass.newInstance();
        } catch (Exception e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

}
