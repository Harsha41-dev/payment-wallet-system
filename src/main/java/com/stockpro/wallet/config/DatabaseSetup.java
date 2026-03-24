package com.stockpro.wallet.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseSetup {

	private final DataSource controlDataSource;
	private final DataSource paytmDataSource;
	private final DataSource uberDataSource;

	public DatabaseSetup(@Qualifier("controlDataSource") DataSource controlDataSource,
			@Qualifier("paytmDataSource") DataSource paytmDataSource,
			@Qualifier("uberDataSource") DataSource uberDataSource) {
		this.controlDataSource = controlDataSource;
		this.paytmDataSource = paytmDataSource;
		this.uberDataSource = uberDataSource;
	}

	@PostConstruct
	public void initialize() {
		resetAllData();
	}

	public void resetAllData() {
		runScripts(controlDataSource, "sql/control-schema.sql", "sql/control-data.sql");
		runScripts(paytmDataSource, "sql/paytm-schema.sql", "sql/paytm-data.sql");
		runScripts(uberDataSource, "sql/uber-schema.sql", "sql/uber-data.sql");
	}

	private void runScripts(DataSource dataSource, String... paths) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

		for (String path : paths) {
			populator.addScript(new ClassPathResource(path));
		}

		DatabasePopulatorUtils.execute(populator, dataSource);
	}
}
