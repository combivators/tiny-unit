package net.tiny.unit.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.ForwardOnlyResultSetTableFactory;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.csv.CsvDataSet;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;

public class JpaHelper {

    private final static Logger LOGGER = Logger.getLogger(JpaHelper.class.getName());
    final static String driver = "org.h2.Driver";

    final String unitName;
    final String jdbcDriver;
    final String jdbcUrl;
    final String jdbcUser;
    final String jdbcPassword;
    final boolean trace;
    final Properties properties;
    EntityManagerFactory entityManagerFactory = null;
    EntityManager entityManager = null;
    File backupFile = null;

    public JpaHelper(String persistence, String unit, int port, String db, boolean debug) throws Exception {
        this.unitName = unit;
        this.trace = debug;
        this.jdbcDriver = driver;
        this.jdbcUrl = String.format("jdbc:h2:tcp://localhost:%d/%s", port, db);
        this.jdbcUser = "sa";
        this.jdbcPassword = "";
        properties = new Properties();
        if (persistence != null && !persistence.isEmpty()) {
            URL url = JpaHelper.class.getClassLoader().getResource(persistence);
            if(null == url) {
                throw new FileNotFoundException(persistence);
            }
            if (trace) {
                LOGGER.info(String.format("[JPA-UNIT] Load '%s'", url.toString()));
            }
            properties.load(url.openStream());
            // Set JDBC Properties
            properties.setProperty("javax.persistence.jdbc.driver", jdbcDriver);
            properties.setProperty("javax.persistence.jdbc.url", jdbcUrl);
            properties.setProperty("javax.persistence.jdbc.user", jdbcUser);
            properties.setProperty("javax.persistence.jdbc.password", jdbcPassword);
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }


    public String getUnitName() {
        return unitName;
    }


    public void start() throws Exception {
        if (trace) {
            LOGGER.info("[JPA-UNIT] start");
        }
        Class.forName(jdbcDriver);
        if(backupFile == null || !backupFile.exists()) {
            File temp = new File(System.getProperty("java.io.tmpdir"));
            Calendar now = Calendar.getInstance();
            String prefix = new SimpleDateFormat("yyyyMMdd").format(now.getTime());
            backupFile = new File(temp, "backup_" + prefix + ".xml");
        }
        createEntityManager();
    }

    public void stop() throws Exception {
        closeEntityManager();
        if(backupFile != null && backupFile.exists()) {
            backupFile.delete();
        }
        if (trace) {
            LOGGER.info("[JPA-UNIT] stop");
        }
    }

    private void createEntityManager() {
        entityManagerFactory = Persistence.createEntityManagerFactory(unitName, properties);
        entityManager = entityManagerFactory.createEntityManager();
    }

    private void closeEntityManager() {
        if(null == entityManager)
            return;
        entityManager.close();
        entityManagerFactory.close();
        entityManagerFactory = null;
    }

    public Connection getJdbcConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    public void executeBatch(String[] queries)  throws SQLException {
        if (null == queries || queries.length ==0)
            return;
        Connection conn = getJdbcConnection();
        Statement stmt =conn.createStatement();
        for(String sql : queries) {
            stmt.execute(sql);
        }
        conn.commit();
        conn.close();
    }

    protected IDatabaseConnection getConnection() throws Exception {
        IDatabaseConnection connection = new DatabaseConnection(getJdbcConnection());
        //TODO
        if(jdbcDriver.contains("h2")) {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new JPADataTypeFactory());
        } else if(jdbcDriver.contains("mysql")) {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());
        } else if(jdbcDriver.contains("oracle")) {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
        } else if(jdbcDriver.contains("hsql")) {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
        } else if(jdbcDriver.contains("postgresql")) {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
        }
        connection.getConfig().setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, new ForwardOnlyResultSetTableFactory());
        return connection;
    }

    public void importCsv(File path) throws Exception {
        importCsv(path, true, 1);
    }

    public void importCsv(File path, boolean clean, int onceNumber) throws Exception {
        File ordering = new File(path, "table-ordering.txt");
        LineNumberReader reader = new LineNumberReader(new FileReader(ordering));
        List<List<String>> orderList = new ArrayList<List<String>>();
        String line = null;
        int count = 0;
        List<String> list =  new ArrayList<String>();
        while((line = reader.readLine()) != null) {
            if(count == 0) {
                list =  new ArrayList<String>();
                orderList.add(list);
            }
            list.add(line);
            count++;
            if(onceNumber > 0 && count >= onceNumber) {
                count = 0;
            }
        }
        reader.close();

        for(List<String> ts : orderList) {
            FileWriter writer = new FileWriter(ordering);
            for(String t : ts) {
                writer.write(t);
                writer.write("\r\n");
            }
            writer.close();
            try {
                ReplacementDataSet dataSet = new ReplacementDataSet(new CsvDataSet(path));
                dataSet.addReplacementObject("", null);
                IDatabaseConnection conn = getConnection();
                if(clean) {
                    DatabaseOperation.CLEAN_INSERT.execute(conn, dataSet);
                } else {
                    DatabaseOperation.INSERT.execute(conn, dataSet);
                }
                conn.close();
            } catch( Exception ex) {
                ex.printStackTrace(System.err);
            }
            if(ordering.delete()) {
                   for(String t : ts) {
                    System.out.println("'" + t + "' done.");
                }
            }
        }
        FileWriter writer = new FileWriter(ordering);
        for(List<String> ts : orderList) {
            for(String t : ts) {
                writer.write(t);
                writer.write("\r\n");
            }
        }
        writer.close();
        if (trace) {
            LOGGER.info(String.format("[JPA-UNIT] From '%s' %d csv file(s) was imported.", path.getAbsolutePath(), orderList.size()));
        }
    }

    static class JPADataTypeFactory extends H2DataTypeFactory {
        // H2Database boolean to smallint
        public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
            DataType dataType = null;
            if (sqlType == Types.BOOLEAN) {
                dataType = DataType.BOOLEAN;
            } else if("SMALLINT".equals(sqlTypeName)) {
                dataType = DataType.BOOLEAN;
            } else {
                dataType = super.createDataType(sqlType, sqlTypeName);
            }
            return dataType;
        }
    }
}
