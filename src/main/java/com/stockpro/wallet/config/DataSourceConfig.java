package com.stockpro.wallet.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DataSourceConfig {

	@Value("${app.datasource.control.url}")
	private String controlUrl;

	@Value("${app.datasource.control.username}")
	private String controlUsername;

	@Value("${app.datasource.control.password}")
	private String controlPassword;

	@Value("${app.datasource.paytm.url}")
	private String paytmUrl;

	@Value("${app.datasource.paytm.username}")
	private String paytmUsername;

	@Value("${app.datasource.paytm.password}")
	private String paytmPassword;

	@Value("${app.datasource.uber.url}")
	private String uberUrl;

	@Value("${app.datasource.uber.username}")
	private String uberUsername;

	@Value("${app.datasource.uber.password}")
	private String uberPassword;

	@Bean
	@Primary
	DataSource controlDataSource() {
		return buildDataSource(controlUrl, controlUsername, controlPassword);
	}

	@Bean
	DataSource paytmDataSource() {
		return buildDataSource(paytmUrl, paytmUsername, paytmPassword);
	}

	@Bean
	DataSource uberDataSource() {
		return buildDataSource(uberUrl, uberUsername, uberPassword);
	}

	@Bean
	@Primary
	NamedParameterJdbcTemplate controlJdbcTemplate(@Qualifier("controlDataSource") DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}

	@Bean
	NamedParameterJdbcTemplate paytmJdbcTemplate(@Qualifier("paytmDataSource") DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}

	@Bean
	NamedParameterJdbcTemplate uberJdbcTemplate(@Qualifier("uberDataSource") DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}

	@Bean
	@Primary
	PlatformTransactionManager controlTransactionManager(@Qualifier("controlDataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	PlatformTransactionManager paytmTransactionManager(@Qualifier("paytmDataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	PlatformTransactionManager uberTransactionManager(@Qualifier("uberDataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	private DataSource buildDataSource(String url, String username, String password) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}
}
