package com.stockpro.wallet.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferRecord {

	private String transferId;
	private TransferMode mode;
	private String fromWallet;
	private String toWallet;
	private BigDecimal amount;
	private String currency;
	private String note;
	private TransferStatus status;
	private String failureReason;
	private boolean simulateCreditFailure;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public String getTransferId() {
		return transferId;
	}

	public void setTransferId(String transferId) {
		this.transferId = transferId;
	}

	public TransferMode getMode() {
		return mode;
	}

	public void setMode(TransferMode mode) {
		this.mode = mode;
	}

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

	public TransferStatus getStatus() {
		return status;
	}

	public void setStatus(TransferStatus status) {
		this.status = status;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public boolean isSimulateCreditFailure() {
		return simulateCreditFailure;
	}

	public void setSimulateCreditFailure(boolean simulateCreditFailure) {
		this.simulateCreditFailure = simulateCreditFailure;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
