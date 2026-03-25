package com.stockpro.wallet.wallets;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@GetMapping("/paytm/{walletId}/ledger")
	public List<WalletLedgerEntry> getPaytmLedger(@PathVariable String walletId) {
		return paytmWalletService.getLedgerEntries(walletId);
	}

	@GetMapping("/uber")
	public List<WalletSummary> getUberWallets() {
		return uberWalletService.getWallets();
	}

	@GetMapping("/uber/{walletId}/ledger")
	public List<WalletLedgerEntry> getUberLedger(@PathVariable String walletId) {
		return uberWalletService.getLedgerEntries(walletId);
	}

	@GetMapping("/summary")
	public Map<String, Object> getWalletSummary() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("paytmWallets", paytmWalletService.getWallets());
		response.put("uberWallets", uberWalletService.getWallets());
		return response;
	}
}
