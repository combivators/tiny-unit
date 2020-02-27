package net.tiny.unit.db;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import net.tiny.config.Reflections;

public class DatabaseExtension implements BeforeAllCallback, AfterAllCallback, BeforeTestExecutionCallback {

    static {
        DriverManager.getDrivers();
    }

    protected static enum StoreKeyType {
        H2_CLASS,
        DS_CLASS,
        JPA_CLASS
    }

    protected static final Namespace NAMESPACE = Namespace.create("net", "tiny", "unit", "DatabaseExtension");

    private static String lastLogging = "";
    private static boolean logged = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Database database = getDatabaseAnnotation(context);
        if(null == database)
            return;
        logging(database.logging());
        H2Engine engine = new H2Engine.Builder()
                .port(database.port())
                .name(database.db())
                .clear(database.clear())
                .before(database.before())
                .after(database.after())
                .build();
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.H2_CLASS), engine);
        engine.start();
        EmbeddedDataSource datasource = new EmbeddedDataSource(engine);
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.DS_CLASS), datasource);

        if (!database.createScript().isEmpty()) {
            runScript(new File(database.createScript()), datasource.getConnection(), database.trace());
        }

        JpaHelper helper = new JpaHelper(database.persistence(),
                database.unit(), database.port(), database.db(), database.trace());
        if (!database.persistence().isEmpty()) {
            context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.JPA_CLASS), helper);
            helper.start();
            if (!database.imports().isEmpty()) {
                helper.importCsv(new File(database.imports()));
            }
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        Object testCase = context.getRequiredTestInstance();
        // Find a field with @Resource
        List<Field> withResouceAnnotatedFields = findAnnotatedFields(testCase.getClass(), Resource.class);
        EmbeddedDataSource datasource = context.getStore(NAMESPACE)
                .get(getStoreKey(context, StoreKeyType.DS_CLASS), EmbeddedDataSource.class);
        for(Field field : withResouceAnnotatedFields) {
            final Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
            if (DataSource.class.equals(resourceType)) {
                field.setAccessible(true);
                // Set DataSource instance
                field.set(testCase, datasource);
            }
        }

        JpaHelper helper = context.getStore(NAMESPACE)
                .get(getStoreKey(context, StoreKeyType.JPA_CLASS), JpaHelper.class);
        if (null == helper)
            return;
        // Find a field with @PersistenceContext
        List<Field> withPersistenceAnnotatedFields = findAnnotatedFields(testCase.getClass(), PersistenceContext.class);
        for(Field field : withPersistenceAnnotatedFields) {
            final String unitName = field.getAnnotation(PersistenceContext.class).unitName();
            final Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
            if (EntityManager.class.equals(resourceType) && (unitName.isEmpty() || helper.getUnitName().equals(unitName))) {
                field.setAccessible(true);
                // Set EntityManager instance
                field.set(testCase, helper.getEntityManager());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Database database = getDatabaseAnnotation(context);
        if(null == database)
            return;
        if (!database.persistence().isEmpty()) {
            JpaHelper helper = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.JPA_CLASS), JpaHelper.class);
            helper.stop();
        }
        EmbeddedDataSource datasource = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.DS_CLASS), EmbeddedDataSource.class);
        if (!database.dropScript().isEmpty()) {
            runScript(new File(database.dropScript()), datasource.getConnection(), database.trace());
        }
        datasource.close();
        H2Engine engine = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.H2_CLASS), H2Engine.class);
        engine.stop();
        if (database.report()) {
            report("Test Database", context, engine);
        }
    }

    static void logging(String file) {
        if (logged && lastLogging.equals(file))
            return;
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL url = loader.getResource(file);
        if (null != url) {
            try {
                LogManager.getLogManager().readConfiguration(url.openStream());
                logged = true;
                lastLogging = file;
            } catch (Exception ignore) {
            }
        }
    }

    protected static String getStoreKey(ExtensionContext context, StoreKeyType type) {
        String storedKey = type.name();
        switch(type) {
        case H2_CLASS:
            storedKey = context.getRequiredTestClass().getName();
            break;
        case DS_CLASS:
            storedKey = context.getRequiredTestClass().getName();
            storedKey = storedKey.concat(".ds");
            break;
        case JPA_CLASS:
            storedKey = context.getRequiredTestClass().getName();
            storedKey = storedKey.concat(".jpa");
            break;
        }
        return storedKey;
    }

    protected static Database getDatabaseAnnotation(ExtensionContext context) {
        try {
            return context.getElement()
                    .filter(el -> el.isAnnotationPresent(Database.class))
                    .get()
                    .getAnnotation(Database.class);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private static void report(String unit, ExtensionContext context, H2Engine engine) {
        String message = "H2Engine report";
        context.publishReportEntry(unit, message);
    }

    static List<Field> findAnnotatedFields(Class<?> type, Class<? extends Annotation> annotation) {
        return Reflections.getFieldStream(type)
                .filter(f -> f.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
    }

    static void runScript(File file, Connection conn, boolean trace) throws IOException, SQLException {
        // Give the input file to Reader
        Reader reader = new BufferedReader(new FileReader(file));
        try {
            // Initialize object for ScripRunner
            ScriptRunner scriptRunner = new ScriptRunner(conn, true, true);
            if (trace) {
                scriptRunner.setLogWriter(new PrintWriter(System.out));
            }
            // Exctute script
            scriptRunner.runScript(reader);
        } finally {
            reader.close();
            conn.close();
        }
    }

    final static String driver = "org.h2.Driver";
    protected static class EmbeddedDataSource implements DataSource, Closeable {
        final String url;
        //Properties connectionProperties = new Properties();
        List<Connection> cache = new ArrayList<>();

        EmbeddedDataSource(H2Engine engine) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(String.format("Not found jdbc driver class '%s'.", driver));
            }
            url = engine.getURL();
            //connectionProperties.setProperty("user", "sa");
            //connectionProperties.setProperty("password", "sa");
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            throw new UnsupportedOperationException("Not impleted");

        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            throw new UnsupportedOperationException("Not impleted");
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public void close() throws IOException {
            for (Connection conn : cache) {
                colseConnection(conn);
            }
        }

        private void colseConnection(Connection conn) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {}
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = DriverManager.getConnection(url, "sa", "");
            cache.add(conn);
            return conn;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new UnsupportedOperationException("Not impleted");
        }

        @Override
        protected void finalize() throws Throwable {
            close();
        }
    }

}
