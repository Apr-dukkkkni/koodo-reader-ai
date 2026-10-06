package com.koodoagent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class PgVectorDataSourceConfig {

    @Value("${pgvector.url}")
    private String url;

    @Value("${pgvector.username}")
    private String username;

    @Value("${pgvector.password}")
    private String password;

    /**
     * 独立的 pgvector 数据源，不干扰主 SQLite 数据源。
     * 名字必须明确，避免和 Spring Boot 自动配置的 primary datasource 冲突。
     */
    @Bean(name = "pgVectorDataSource")
    public DataSource pgVectorDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        ds.setPoolName("pgvector-pool");
        return ds;
    }

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate() {
        return new JdbcTemplate(pgVectorDataSource());
    }
}