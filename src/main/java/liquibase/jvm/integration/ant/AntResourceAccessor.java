package liquibase.jvm.integration.ant;

import liquibase.resource.ResourceAccessor;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

/**
 * An implementation of FileOpener that is specific to how Ant works.
 */
public class AntResourceAccessor implements ResourceAccessor {
    private AntClassLoader loader;

    public AntResourceAccessor(final Project project, final Path classpath) {
        loader = AccessController.doPrivileged(new PrivilegedAction<AntClassLoader>() {
            public AntClassLoader run() {
                return new AntClassLoader(project, classpath);
            }
        });
    }

    public InputStream getResourceAsStream(String file) throws IOException {
        URL resource = loader.getResource(file);
        if (resource == null) {
            return null;
        }
        return resource.openStream();
    }

    public Enumeration<URL> getResources(String packageName) throws IOException {
        return loader.getResources(packageName);
    }

    public ClassLoader toClassLoader() {
        return loader;
    }

    @Override
    public String toString() {
        return getClass().getName()+"("+loader.getClasspath()+")";
    }
}
