package uk.co.oliwali.HawkEye.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import uk.co.oliwali.HawkEye.HawkEye;
import uk.co.oliwali.HawkEye.util.Config;
import uk.co.oliwali.HawkEye.util.Util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controls MySQL connection pool using Hikari
 *
 * @author bob7l
 */
public class ConnectionManager implements AutoCloseable {

    private static final String[] DRIVER_CLASS_NAMES = {
            "com.mysql.cj.jdbc.Driver",
            "com.mysql.jdbc.Driver"
    };

    private HikariDataSource connectionPool;
    private URLClassLoader driverClassLoader;

    public ConnectionManager() throws Exception {
        Util.debug("Attempting to connecting to database...");

        // Ensure the MySQL driver is loaded (its static initialiser self-registers
        // it with DriverManager under its own class name - never a HawkEye class).
        ClassLoader driverLoader = loadMySqlDriver();

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(Config.PoolSize);
        config.setJdbcUrl(buildJdbcUrl());
        config.setUsername(Config.DbUser);
        config.setPassword(Config.DbPassword);
        config.setAutoCommit(false);

        // When the driver came from an external JAR we must tell HikariCP which
        // classloader to use so it can find the driver class.
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(driverLoader);
        try {
            connectionPool = new HikariDataSource(config);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /**
     * Returns the ClassLoader that was used to load (and therefore register) the
     * MySQL driver.  Tries the server/plugin classpath first; falls back to an
     * external JAR placed in the plugin's {@code /lib} data folder.
     */
    private ClassLoader loadMySqlDriver() throws Exception {
        // 1. Already on the server classpath (Paper bundles mysql-connector-j).
        for (String name : DRIVER_CLASS_NAMES) {
            try {
                Class.forName(name, true, getClass().getClassLoader());
                Util.debug("MySQL driver found on server classpath: " + name);
                return getClass().getClassLoader();
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 2. External JAR in <dataFolder>/lib/.
        File libDir = new File(HawkEye.getInstance().getDataFolder(), "lib");
        File driverJar = findDriverJar(libDir);
        if (driverJar == null) {
            throw new ClassNotFoundException(
                    "MySQL JDBC driver not found on server classpath and no driver JAR found in "
                            + libDir.getAbsolutePath()
                            + ". Place mysql-connector-j.jar there and restart.");
        }

        driverClassLoader = new URLClassLoader(
                new URL[]{driverJar.toURI().toURL()},
                getClass().getClassLoader()
        );

        for (String name : DRIVER_CLASS_NAMES) {
            try {
                Class.forName(name, true, driverClassLoader);
                Util.debug("Loaded MySQL driver from external JAR: " + driverJar.getName());
                return driverClassLoader;
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new ClassNotFoundException(
                "Unable to load MySQL JDBC driver from " + driverJar.getAbsolutePath());
    }

    @Override
    public void close() throws Exception {
        if (connectionPool != null) {
            connectionPool.close();
        }
        if (driverClassLoader != null) {
            driverClassLoader.close();
            driverClassLoader = null;
        }
    }

    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    private File findDriverJar(File libDirectory) {
        if (!libDirectory.isDirectory()) {
            return null;
        }
        for (String name : new String[]{
                "mysql-connector-java-5.1.49.jar",
                "mysql-connector-java.jar",
                "mysql-connector-j.jar"
        }) {
            File f = new File(libDirectory, name);
            if (f.isFile()) return f;
        }
        File[] jars = libDirectory.listFiles(
                (dir, n) -> n.startsWith("mysql-connector") && n.endsWith(".jar"));
        return jars != null && jars.length > 0 ? jars[0] : null;
    }

    private String buildJdbcUrl() {
        return "jdbc:mysql://" + Config.DbHostname + ":" + Config.DbPort + "/" + Config.DbDatabase
                + "?rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=275&prepStmtCacheSqlLimit=2048";
    }
}
