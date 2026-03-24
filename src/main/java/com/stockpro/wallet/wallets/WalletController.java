package com.stockpro.wallet.wallets;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockpro.wallet.paytm.PaytmWalletService;
import com.stockpro.wallet.uber.UberWalletService;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

	private final PaytmWalletService paytmWalletService;
	private final UberWalletService uberWalletService;

	public WalletController(PaytmWalletService paytmWalletService, UberWalletService uberWalletService) {
		this.paytmWalletService = paytmWalletService;
		this.uberWalletService = uberWalletService;
	}

	@GetMapping("/paytm")
	public List<WalletSummary> getPaytmWallets() {
		return paytmWalletService.getWallets();
	}

	@GetMapping("/uber")
	public List<WalletSummary> getUberWallets() {
		return uberWalletService.getWallets();
	}

	@GetMapping("/summary")
	public Map<String, Object> getWalletSummary() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("paytmWallets", paytmWalletService.getWallets());
		response.put("uberWallets", uberWalletService.getWallets());
		return response;
	}
}
