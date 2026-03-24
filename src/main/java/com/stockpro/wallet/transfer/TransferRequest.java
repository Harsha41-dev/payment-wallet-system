package com.stockpro.wallet.transfer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransferRequest {

	@NotBlank(message = "fromWallet is required")
	private String fromWallet;

	@NotBlank(message = "toWallet is required")
	private String toWallet;

	@NotNull(message = "amount is required")
	@DecimalMin(value = "1.00", message = "amount should be at least 1.00")
	private BigDecimal amount;

	@NotBlank(message = "currency is required")
	private String currency;

	private String note;

	private boolean simulateCreditFailure;

	public String getFromWallet() {
		return fromWallet;
	}

	public void setFromWallet(String fromWallet) {
		this.fromWallet = fromWallet;
	}

	public String getToWallet() {
		return toWallet;
	}

	public void setToWallet(String toWallet) {
		this.toWallet = toWallet;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public boolean isSimulateCreditFailure() {
		return simulateCreditFailure;
	}

	public void setSimulateCreditFailure(boolean simulateCreditFailure) {
		this.simulateCreditFailure = simulateCreditFailure;
	}
}
