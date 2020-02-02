package net.tiny.unit.db;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;

public class H2Engine implements Closeable {
    private final static Logger LOGGER  = Logger.getLogger(H2Engine.class.getName());

    public static final int    H2_PORT = 9092;
    public static final String H2_DIR = ".";
    public static final String H2_DB  = "h2";
    public static final String H2_USER = "sa";

    private final Builder builder;
    private final CountDownLatch serverLock = new CountDownLatch(1);
    private Server h2db;
    private Throwable lastError = null;

    private H2Engine(Builder builder) throws SQLException {
        this.builder = builder;
        this.h2db = Server.createTcpServer(createArguments());
    }

    private String[] createArguments() {
        LinkedList<String> args = new LinkedList<>();
        args.add("-tcpPort");
        args.add(Integer.toString(builder.port));
        args.add("-baseDir");
        args.add(builder.base);
        if (builder.allow) {
            args.add("-tcpAllowOthers");
        }
        if (builder.create) {
            args.add("-ifNotExists");
        }
        return args.toArray(new String[args.size()]);
    }

    public boolean start() {
        try {
            //调用H2-DB的服务入口
            h2db.start();
            LOGGER.info("[H2] Database engine started. - " + getURL());
            if (builder.changed != null) {
                executeScript(new String[] {String.format("alter user sa set password '%s'", builder.changed)});
                builder.password = builder.changed;
            }
            if (builder.before != null) {
                if (builder.batch)
                	batchScript(builder.before);
                else
                	executeScript(builder.before);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            lastError = ex;
        }
        return lastError != null;
    }

    public void stop() {
        close();
    }

    @Override
    public void close() {
    	stopH2();
    }

    private void stopH2() {
        try {
            h2db.stop();
            int i=0;
            while(i<10 && isRunning()){
                i++;
                try{
                    Thread.sleep(500);
                } catch (Exception ex){
                }
            }
            if(builder.clear) {
                DeleteDbFiles.execute(builder.base, builder.name, true);
                LOGGER.info(String.format("[H2] Database file '%s/%s.db' was deleted.", builder.base, builder.name));
            }
        } finally {
            serverLock.countDown();
            LOGGER.info("[H2] Database engine stoped.");
        }
    }
    public boolean isRunning(){
        try{
            return h2db.isRunning(true);
        } catch(RuntimeException  e){
            return false;
        }
    }

    public boolean hasError() {
        return (null != lastError);
    }

    public Throwable getLastError() {
        return lastError;
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        serverLock.await(timeout, unit);
    }

    public void awaitTermination() throws InterruptedException {
        serverLock.await();
    }

    public String getURL() {
        String url = "jdbc:h2:" + h2db.getURL() + "/" + builder.name;
        return url;
    }

    public void executeScript(String[] scripts) throws SQLException {
        if(null == scripts || scripts.length == 0)
            return;
        String url = getURL();
        int count = 0;
        long st  = System.currentTimeMillis();
        Connection conn = DriverManager.getConnection(url, builder.user, builder.password);
        for(int i=0; i<scripts.length; i++) {
        	String sql = scripts[i];
        	if (sql.startsWith("-"))
        		continue;
        	try {
        		PreparedStatement ps = conn.prepareStatement(sql);
        		ps.executeUpdate();
        		ps.close();
        		count++;
        		if (LOGGER.isLoggable(Level.FINE)) {
        			LOGGER.fine(String.format("[H2] Run script(%d) - '%s'", (i+1), sql));
        		}
        	} catch (SQLException e) {
         		LOGGER.log(Level.WARNING, String.format("[H2] Run script(%d) - '%s' error : %s", i, sql, e.getMessage()));
            }
        }
        conn.commit();
        conn.close();
        LOGGER.info(String.format("[H2] Run %d scripts - %dms", count, (System.currentTimeMillis() - st)));
    }

    public void batchScript(String[] scripts) throws SQLException {
        if(null == scripts || scripts.length == 0)
            return;
        String url = getURL();
        long st  = System.currentTimeMillis();
        Connection conn = DriverManager.getConnection(url, builder.user, builder.password);
		Statement s  = conn.createStatement();
        for(int i=0; i<scripts.length; i++) {
        	String sql = scripts[i];
        	if (sql.startsWith("-"))
        		continue;
       		s.addBatch(sql);
        }
        s.executeBatch();
        s.close();
        conn.commit();
        conn.close();
        LOGGER.info(String.format("[H2] Run batch scripts - %dms", (System.currentTimeMillis() - st)));
    }

    public static class Builder {
        String bind = "127.0.0.1";
        int port = H2_PORT;
        String name = H2_DB;
        String base = H2_DIR;
        String user = H2_USER;
        String password = "";
        String changed = null; // Change a new password
        String[] before = new String[0];
        String[] after = new String[0];
        String load;
        boolean clear = false; // Clear database file on shutdown
        boolean allow = false; // Start option --tcpAllowOthers
        boolean create = true; // Start option --ifNotExists
        boolean batch = false; // Run batch scripts

        public Builder bind(String ip) {
            bind = ip;
            return this;
        }
        public Builder port(int p) {
            port = p;
            return this;
        }
        public Builder name(String n) {
            name = n;
            return this;
        }
        public Builder base(Path p) {
            base = p.toFile().getAbsolutePath();
            return this;
        }
        public Builder user(String u) {
        	user = u;
            return this;
        }
        public Builder password(String p) {
        	password = p;
            return this;
        }
        public Builder before(String[] s) {
            before =  s;
            return this;
        }
        public Builder after(String[] s) {
        	after =  s;
            return this;
        }
        public Builder load(String p) {
            if (!Files.exists(Paths.get(p))) {
                throw new IllegalArgumentException("Not found load CSV data path : " + p);
            }
            load = p;
            return this;
        }
        public Builder clear(boolean enable) {
            clear = enable;
            return this;
        }
        public Builder allow(boolean enable) {
            allow = enable;
            return this;
        }
        public Builder create(boolean enable) {
            create = enable;
            return this;
        }
        public Builder batch(boolean enable) {
        	batch = enable;
            return this;
        }
        public Builder changed(String p) {
        	changed = p;
            return this;
        }
        @Override
        public String toString() {
            return String.format("jdbc:h2:tcp://%s:%d/%s", bind, port, name);
        }
        public H2Engine build() {
            try {
                return new H2Engine(this);
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}
