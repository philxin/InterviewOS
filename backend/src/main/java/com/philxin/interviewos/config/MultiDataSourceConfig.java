package com.philxin.interviewos.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 多数据源配置：
 * 1) mysql 与 postgresql 数据源同时注册；
 * 2) primaryDataSource 作为 JPA 默认数据源；
 * 3) 同时提供两个 JdbcTemplate 供跨库读写场景使用。
 */
@Configuration
@EnableConfigurationProperties(MultiDatabaseProperties.class)
public class MultiDataSourceConfig {

    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource(MultiDatabaseProperties properties) {
        return buildDataSource("mysql-pool", properties.getMysql(), properties.getPool());
    }

    @Bean(name = "postgresqlDataSource")
    public DataSource postgresqlDataSource(MultiDatabaseProperties properties) {
        return buildDataSource("postgresql-pool", properties.getPostgresql(), properties.getPool());
    }

    @Primary
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource(
        @Qualifier("mysqlDataSource") DataSource mysqlDataSource,
        @Qualifier("postgresqlDataSource") DataSource postgresqlDataSource,
        MultiDatabaseProperties properties
    ) {
        return properties.getPrimary() == MultiDatabaseProperties.DatabaseType.MYSQL
            ? mysqlDataSource
            : postgresqlDataSource;
    }

    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        return new JdbcTemplate(mysqlDataSource);
    }

    @Bean(name = "postgresqlJdbcTemplate")
    public JdbcTemplate postgresqlJdbcTemplate(
        @Qualifier("postgresqlDataSource") DataSource postgresqlDataSource
    ) {
        return new JdbcTemplate(postgresqlDataSource);
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
        MultiDatabaseProperties properties
    ) {
        return hibernateProperties -> {
            String dialect = properties.getPrimary() == MultiDatabaseProperties.DatabaseType.MYSQL
                ? properties.getMysql().getDialect()
                : properties.getPostgresql().getDialect();
            if (dialect != null && !dialect.isBlank()) {
                hibernateProperties.put("hibernate.dialect", dialect);
            }
        };
    }

    private DataSource buildDataSource(
        String poolName,
        MultiDatabaseProperties.DatabaseConfig databaseConfig,
        MultiDatabaseProperties.PoolConfig poolConfig
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName(poolName);
        dataSource.setJdbcUrl(databaseConfig.getUrl());
        dataSource.setUsername(databaseConfig.getUsername());
        dataSource.setPassword(databaseConfig.getPassword());
        dataSource.setDriverClassName(databaseConfig.getDriverClassName());
        dataSource.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
        dataSource.setMinimumIdle(poolConfig.getMinimumIdle());
        dataSource.setConnectionTimeout(poolConfig.getConnectionTimeout());
        dataSource.setValidationTimeout(poolConfig.getValidationTimeout());
        return dataSource;
    }
}
