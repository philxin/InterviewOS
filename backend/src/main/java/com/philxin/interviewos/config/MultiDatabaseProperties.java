package com.philxin.interviewos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 双数据库配置模型：
 * 1) mysql / postgresql 连接信息同时存在；
 * 2) 通过 primary 指定 JPA 默认使用的数据源；
 * 3) pool 配置统一作用于两个连接池。
 */
@ConfigurationProperties(prefix = "app.datasource")
public class MultiDatabaseProperties {
    private DatabaseType primary = DatabaseType.POSTGRESQL;
    private DatabaseConfig mysql = new DatabaseConfig();
    private DatabaseConfig postgresql = new DatabaseConfig();
    private PoolConfig pool = new PoolConfig();

    public DatabaseType getPrimary() {
        return primary;
    }

    public void setPrimary(DatabaseType primary) {
        this.primary = primary;
    }

    public DatabaseConfig getMysql() {
        return mysql;
    }

    public void setMysql(DatabaseConfig mysql) {
        this.mysql = mysql;
    }

    public DatabaseConfig getPostgresql() {
        return postgresql;
    }

    public void setPostgresql(DatabaseConfig postgresql) {
        this.postgresql = postgresql;
    }

    public PoolConfig getPool() {
        return pool;
    }

    public void setPool(PoolConfig pool) {
        this.pool = pool;
    }

    public enum DatabaseType {
        MYSQL,
        POSTGRESQL
    }

    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private String dialect;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getDialect() {
            return dialect;
        }

        public void setDialect(String dialect) {
            this.dialect = dialect;
        }
    }

    public static class PoolConfig {
        private Integer maximumPoolSize = 10;
        private Integer minimumIdle = 2;
        private Long connectionTimeout = 30000L;
        private Long validationTimeout = 5000L;

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Integer getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(Integer minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public Long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Long getValidationTimeout() {
            return validationTimeout;
        }

        public void setValidationTimeout(Long validationTimeout) {
            this.validationTimeout = validationTimeout;
        }
    }
}
