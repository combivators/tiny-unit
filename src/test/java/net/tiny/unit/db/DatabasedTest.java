package net.tiny.unit.db;

import static org.junit.jupiter.api.Assertions.*;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

//Default JPA disable
@Database
public class DatabasedTest {


    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager manager;

    @Resource
    private DataSource ds;


    @Test
    public void testVaildDatabased() throws InterruptedException {
        assertNotNull(ds);
        assertNull(manager);
    }

}
