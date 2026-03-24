package com.stockpro.wallet.wallets;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletSummary {

	private String walletId;
	private String ownerName;
	private String walletType;
	private String currency;
	private BigDecimal balance;
	private BigDecimal lockedAmount;
	private String status;
	private LocalDateTime updatedAt;

	public WalletSummary() {
	}

	public WalletSummary(String walletId, String ownerName, String walletType, String currency, BigDecimal balance,
			BigDecimal lockedAmount, String status, LocalDateTime updatedAt) {
		this.walletId = walletId;
		this.ownerName = ownerName;
		this.walletType = walletType;
		this.currency = currency;
		this.balance = balance;
		this.lockedAmount = lockedAmount;
		this.status = status;
		this.updatedAt = updatedAt;
	}

	public String getWalletId() {
		return walletId;
	}

	public void setWalletId(String walletId) {
		this.walletId = walletId;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getWalletType() {
		return walletType;
	}

	public void setWalletType(String walletType) {
		this.walletType = walletType;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getLockedAmount() {
		return lockedAmount;
	}

	public void setLockedAmount(BigDecimal lockedAmount) {
		this.lockedAmount = lockedAmount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
