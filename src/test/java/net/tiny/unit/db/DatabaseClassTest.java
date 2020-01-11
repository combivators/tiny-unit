package net.tiny.unit.db;

import static org.junit.jupiter.api.Assertions.*;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import net.tiny.unit.db.Database;

@Database(persistence="persistence-eclipselink.properties")
public class DatabaseClassTest {

    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager manager;

    @PersistenceContext(unitName = "Unknow")
    private EntityManager unknow;

    @Resource
    private DataSource dataSource;

    @Test
    public void testVaildField() throws Exception {
        assertNotNull(dataSource);
        assertNotNull(manager);
        assertNull(unknow);
    }

    @ParameterizedTest
    @ArgumentsSource(EntityManagerProvider.class)
    public void testVaildEntityManager(EntityManager em) throws Exception {
        assertNotNull(em);
    }

    @ParameterizedTest
    @ArgumentsSource(DataSourceProvider.class)
    public void testVaildEntityManager(DataSource ds) throws Exception {
        assertNotNull(ds);
    }

}
