package com.stockpro.wallet.transfer;

public enum TransferStatus {
	STARTED,
	PREPARING,
	PREPARED,
	PROCESSING,
	COMPLETED,
	FAILED,
	ROLLING_BACK,
	ROLLED_BACK,
	COMPENSATING,
	COMPENSATED
}
