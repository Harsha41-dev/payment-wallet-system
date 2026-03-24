package com.stockpro.wallet.transfer;

import java.util.List;

public class TransferDetailsResponse {

	private TransferRecord transfer;
	private List<TransferStepRecord> steps;

	public TransferDetailsResponse() {
	}

	public TransferDetailsResponse(TransferRecord transfer, List<TransferStepRecord> steps) {
		this.transfer = transfer;
		this.steps = steps;
	}

	public TransferRecord getTransfer() {
		return transfer;
	}

	public void setTransfer(TransferRecord transfer) {
		this.transfer = transfer;
	}

	public List<TransferStepRecord> getSteps() {
		return steps;
	}

	public void setSteps(List<TransferStepRecord> steps) {
		this.steps = steps;
	}
}
