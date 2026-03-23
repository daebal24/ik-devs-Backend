package com.server.board.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration // 이 클래스는 스프링 설정 클래스
public class DataSourceConfig {

    @Bean //스프링 컨테이너에 직접 객체를 등록
    public DataSource dataSource() {
        // 프로젝트 루트의 test.db
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:./Database.db");
        return ds;
    }

    @Bean //스프링 컨테이너에 직접 객체를 등록
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}

