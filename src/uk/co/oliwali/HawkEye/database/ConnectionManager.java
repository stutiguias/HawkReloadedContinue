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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Controls MySQL connection pool using Hikari
 *
 * @author bob7l
 */
public class ConnectionManager implements AutoCloseable {

    private HikariDataSource connectionPool;
    private Driver registeredDriver;
    private URLClassLoader driverClassLoader;

    public ConnectionManager() throws Exception {
        registeredDriver = registerMySqlDriver();

        Util.debug("Attempting to connecting to database...");

        HikariConfig config = new HikariConfig();

        config.setMaximumPoolSize(Config.PoolSize);
        config.setJdbcUrl(buildJdbcUrl());

        config.setUsername(Config.DbUser);
        config.setPassword(Config.DbPassword);

        config.setAutoCommit(false);

        connectionPool = new HikariDataSource(config);
    }

    @Override
    public void close() throws Exception {
        if (connectionPool != null) {
            connectionPool.close();
        }

        if (registeredDriver != null) {
            DriverManager.deregisterDriver(registeredDriver);
            registeredDriver = null;
        }

        if (driverClassLoader != null) {
            driverClassLoader.close();
            driverClassLoader = null;
        }
    }

    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    private Driver registerMySqlDriver() throws Exception {
        Driver driver = instantiateDriver(getClass().getClassLoader());
        if (driver != null) {
            return registerDriver(driver);
        }

        File libDirectory = new File(HawkEye.getInstance().getDataFolder(), "lib");
        File driverJar = findDriverJar(libDirectory);

        if (driverJar == null) {
            throw new ClassNotFoundException("MySQL JDBC driver not found. Add mysql-connector-java-5.1.49.jar or mysql-connector-j.jar to " + libDirectory.getAbsolutePath());
        }

        driverClassLoader = new URLClassLoader(new URL[]{driverJar.toURI().toURL()}, getClass().getClassLoader());
        driver = instantiateDriver(driverClassLoader);

        if (driver == null) {
            throw new ClassNotFoundException("Unable to load MySQL JDBC driver from " + driverJar.getAbsolutePath());
        }

        return registerDriver(driver);
    }

    private Driver instantiateDriver(ClassLoader classLoader) throws Exception {
        for (String driverClassName : new String[]{"com.mysql.jdbc.Driver", "com.mysql.cj.jdbc.Driver"}) {
            try {
                Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
                return (Driver) driverClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    private Driver registerDriver(Driver driver) throws SQLException {
        Driver shim = new DriverShim(driver);
        DriverManager.registerDriver(shim);
        return shim;
    }

    private File findDriverJar(File libDirectory) {
        if (!libDirectory.isDirectory()) {
            return null;
        }

        String[] driverNames = {
                "mysql-connector-java-5.1.49.jar",
                "mysql-connector-java.jar",
                "mysql-connector-j.jar"
        };

        for (String driverName : driverNames) {
            File driverJar = new File(libDirectory, driverName);
            if (driverJar.isFile()) {
                return driverJar;
            }
        }

        File[] jarFiles = libDirectory.listFiles((dir, name) -> name.startsWith("mysql-connector") && name.endsWith(".jar"));
        return jarFiles != null && jarFiles.length > 0 ? jarFiles[0] : null;
    }

    private String buildJdbcUrl() {
        return "jdbc:mysql://" + Config.DbHostname + ":" + Config.DbPort + "/" + Config.DbDatabase
                + "?rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=275&prepStmtCacheSqlLimit=2048";
    }

    private static final class DriverShim implements Driver {
        private final Driver delegate;

        private DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }

}
