package com.stockpro.wallet.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockpro.wallet.config.DatabaseSetup;

@RestController
@RequestMapping("/api/demo")
public class DemoAdminController {

	private final DatabaseSetup databaseSetup;

	public DemoAdminController(DatabaseSetup databaseSetup) {
		this.databaseSetup = databaseSetup;
	}

	@PostMapping("/reset")
	public Map<String, Object> resetDemoData() {
		databaseSetup.resetAllData();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", "Demo data reset completed");
		return response;
	}
}
