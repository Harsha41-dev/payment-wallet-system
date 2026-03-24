package com.stockpro.wallet.transfer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stockpro.wallet.paytm.PaytmWalletService;
import com.stockpro.wallet.uber.UberWalletService;

@Service
public class TransferService {

	private final TransferStore transferStore;
	private final PaytmWalletService paytmWalletService;
	private final UberWalletService uberWalletService;
	private final ApplicationEventPublisher applicationEventPublisher;

	public TransferService(TransferStore transferStore, PaytmWalletService paytmWalletService,
			UberWalletService uberWalletService, ApplicationEventPublisher applicationEventPublisher) {
		this.transferStore = transferStore;
		this.paytmWalletService = paytmWalletService;
		this.uberWalletService = uberWalletService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public TransferDetailsResponse createTwoPhaseTransfer(TransferRequest request) {
		String transferId = transferStore.startTransfer(TransferMode.TWO_PHASE_COMMIT, request);
		transferStore.addStep(transferId, "TRANSFER_CREATED", "SUCCESS", "2PC transfer request created");

		try {
			transferStore.updateStatus(transferId, TransferStatus.PREPARING, null);

			String paytmNote = request.getNote();
			if (paytmNote == null || paytmNote.isBlank()) {
				paytmNote = "Paytm wallet hold created";
			}

			paytmWalletService.prepareDebit(transferId, request.getFromWallet(), request.getAmount(), paytmNote);
			transferStore.addStep(transferId, "PREPARE_PAYTM_DEBIT", "SUCCESS",
					"Amount reserved on Paytm wallet before final commit");

			String uberNote = request.getNote();
			if (uberNote == null || uberNote.isBlank()) {
				uberNote = "Uber wallet hold created";
			}

			uberWalletService.prepareCredit(transferId, request.getToWallet(), request.getAmount(), uberNote);
			transferStore.addStep(transferId, "PREPARE_UBER_CREDIT", "SUCCESS",
					"Incoming credit prepared on Uber wallet");

			transferStore.updateStatus(transferId, TransferStatus.PREPARED, null);

			if (request.isSimulateCreditFailure()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
						"Coordinator stopped before the commit phase, starting rollback");
			}

			paytmWalletService.commitPreparedDebit(transferId, "2PC commit applied on Paytm wallet");
			transferStore.addStep(transferId, "COMMIT_PAYTM_DEBIT", "SUCCESS",
					"Reserved amount moved from Paytm wallet balance");

			uberWalletService.commitPreparedCredit(transferId, "2PC commit applied on Uber wallet");
			transferStore.addStep(transferId, "COMMIT_UBER_CREDIT", "SUCCESS",
					"Prepared credit moved into Uber wallet balance");

			transferStore.updateStatus(transferId, TransferStatus.COMPLETED, null);
		} catch (Exception ex) {
			String errorMessage = getErrorMessage(ex);
			transferStore.updateStatus(transferId, TransferStatus.ROLLING_BACK, errorMessage);
			transferStore.addStep(transferId, "ROLLBACK_STARTED", "STARTED",
					"Prepare phase did not finish cleanly, so both holds are rolling back");

			paytmWalletService.rollbackPreparedDebit(transferId, "2PC rollback on Paytm wallet");
			transferStore.addStep(transferId, "ROLLBACK_PAYTM_DEBIT", "SUCCESS",
					"Paytm hold released after prepare failure");

			uberWalletService.rollbackPreparedCredit(transferId, "2PC rollback on Uber wallet");
			transferStore.addStep(transferId, "ROLLBACK_UBER_CREDIT", "SUCCESS",
					"Uber hold released after prepare failure");

			transferStore.updateStatus(transferId, TransferStatus.ROLLED_BACK, errorMessage);
		}

		return transferStore.getTransferDetails(transferId);
	}

	public TransferDetailsResponse createOrchestratedSagaTransfer(TransferRequest request) {
		String transferId = transferStore.startTransfer(TransferMode.SAGA_ORCHESTRATION, request);
		transferStore.addStep(transferId, "TRANSFER_CREATED", "SUCCESS", "Orchestrated saga transfer request created");

		boolean sourceDebited = false;

		try {
			transferStore.updateStatus(transferId, TransferStatus.PROCESSING, null);

			String paytmNote = request.getNote();
			if (paytmNote == null || paytmNote.isBlank()) {
				paytmNote = "Saga debit from Paytm wallet";
			}

			paytmWalletService.debitForSaga(transferId, request.getFromWallet(), request.getAmount(), paytmNote);
			sourceDebited = true;
			transferStore.addStep(transferId, "PAYTM_DEBIT", "SUCCESS",
					"Source wallet debited as the first local transaction");

			String uberNote = request.getNote();
			if (uberNote == null || uberNote.isBlank()) {
				uberNote = "Saga credit to Uber wallet";
			}

			uberWalletService.creditForSaga(transferId, request.getToWallet(), request.getAmount(), uberNote,
					request.isSimulateCreditFailure());
			transferStore.addStep(transferId, "UBER_CREDIT", "SUCCESS",
					"Destination wallet credited as the second local transaction");

			transferStore.updateStatus(transferId, TransferStatus.COMPLETED, null);
		} catch (Exception ex) {
			String errorMessage = getErrorMessage(ex);

			if (!sourceDebited) {
				transferStore.addStep(transferId, "PAYTM_DEBIT", "FAILED", errorMessage);
				transferStore.updateStatus(transferId, TransferStatus.FAILED, errorMessage);
				return transferStore.getTransferDetails(transferId);
			}

			transferStore.addStep(transferId, "UBER_CREDIT", "FAILED", errorMessage);
			transferStore.updateStatus(transferId, TransferStatus.COMPENSATING, errorMessage);

			try {
				paytmWalletService.refundSagaDebit(transferId, request.getFromWallet(), request.getAmount(),
						"Saga compensation back to Paytm wallet");
				transferStore.addStep(transferId, "PAYTM_REFUND", "SUCCESS",
						"Compensation refund restored the source wallet balance");
				transferStore.updateStatus(transferId, TransferStatus.COMPENSATED, errorMessage);
			} catch (Exception compensationEx) {
				String compensationMessage = getErrorMessage(compensationEx);
				transferStore.addStep(transferId, "PAYTM_REFUND", "FAILED", compensationMessage);
				transferStore.updateStatus(transferId, TransferStatus.FAILED, compensationMessage);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Saga credit failed and compensation also failed");
			}
		}

		return transferStore.getTransferDetails(transferId);
	}

	public TransferDetailsResponse createChoreographyTransfer(TransferRequest request) {
		String transferId = transferStore.startTransfer(TransferMode.SAGA_CHOREOGRAPHY, request);
		transferStore.addStep(transferId, "TRANSFER_CREATED", "SUCCESS", "Choreography saga transfer request created");
		transferStore.updateStatus(transferId, TransferStatus.PROCESSING, null);

		applicationEventPublisher.publishEvent(
				new TransferStartedEvent(transferId, request.isSimulateCreditFailure()));

		return transferStore.getTransferDetails(transferId);
	}

	@EventListener
	public void handleTransferStarted(TransferStartedEvent event) {
		TransferRecord transfer = transferStore.getTransfer(event.getTransferId());

		try {
			String paytmNote = transfer.getNote();
			if (paytmNote == null || paytmNote.isBlank()) {
				paytmNote = "Choreography debit from Paytm wallet";
			}

			paytmWalletService.debitForSaga(transfer.getTransferId(), transfer.getFromWallet(), transfer.getAmount(),
					paytmNote);
			transferStore.addStep(transfer.getTransferId(), "PAYTM_DEBIT_EVENT", "SUCCESS",
					"Paytm wallet reacted to the transfer-started event and debited the source wallet");

			applicationEventPublisher.publishEvent(
					new SourceDebitedEvent(transfer.getTransferId(), event.isSimulateCreditFailure()));
		} catch (Exception ex) {
			String errorMessage = getErrorMessage(ex);
			transferStore.addStep(transfer.getTransferId(), "PAYTM_DEBIT_EVENT", "FAILED", errorMessage);
			transferStore.updateStatus(transfer.getTransferId(), TransferStatus.FAILED, errorMessage);
		}
	}

	@EventListener
	public void handleSourceDebited(SourceDebitedEvent event) {
		TransferRecord transfer = transferStore.getTransfer(event.getTransferId());

		try {
			String uberNote = transfer.getNote();
			if (uberNote == null || uberNote.isBlank()) {
				uberNote = "Choreography credit to Uber wallet";
			}

			uberWalletService.creditForSaga(transfer.getTransferId(), transfer.getToWallet(), transfer.getAmount(),
					uberNote, event.isSimulateCreditFailure());
			transferStore.addStep(transfer.getTransferId(), "UBER_CREDIT_EVENT", "SUCCESS",
					"Uber wallet reacted to the source-debited event and credited the destination wallet");
			transferStore.updateStatus(transfer.getTransferId(), TransferStatus.COMPLETED, null);
		} catch (Exception ex) {
			String errorMessage = getErrorMessage(ex);
			transferStore.addStep(transfer.getTransferId(), "UBER_CREDIT_EVENT", "FAILED", errorMessage);
			transferStore.updateStatus(transfer.getTransferId(), TransferStatus.COMPENSATING, errorMessage);
			applicationEventPublisher.publishEvent(new CreditFailedEvent(transfer.getTransferId(), errorMessage));
		}
	}

	@EventListener
	public void handleCreditFailed(CreditFailedEvent event) {
		TransferRecord transfer = transferStore.getTransfer(event.getTransferId());

		try {
			paytmWalletService.refundSagaDebit(transfer.getTransferId(), transfer.getFromWallet(), transfer.getAmount(),
					"Choreography compensation back to Paytm wallet");
			transferStore.addStep(transfer.getTransferId(), "PAYTM_REFUND_EVENT", "SUCCESS",
					"Paytm wallet reacted to the credit-failed event and refunded the source wallet");
			transferStore.updateStatus(transfer.getTransferId(), TransferStatus.COMPENSATED, event.getReason());
		} catch (Exception ex) {
			String errorMessage = getErrorMessage(ex);
			transferStore.addStep(transfer.getTransferId(), "PAYTM_REFUND_EVENT", "FAILED", errorMessage);
			transferStore.updateStatus(transfer.getTransferId(), TransferStatus.FAILED, errorMessage);
		}
	}

	private String getErrorMessage(Exception ex) {
		if (ex instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
			return responseStatusException.getReason();
		}

		return ex.getMessage();
	}

	private static class TransferStartedEvent {
		private final String transferId;
		private final boolean simulateCreditFailure;

		private TransferStartedEvent(String transferId, boolean simulateCreditFailure) {
			this.transferId = transferId;
			this.simulateCreditFailure = simulateCreditFailure;
		}

		private String getTransferId() {
			return transferId;
		}

		private boolean isSimulateCreditFailure() {
			return simulateCreditFailure;
		}
	}

	private static class SourceDebitedEvent {
		private final String transferId;
		private final boolean simulateCreditFailure;

		private SourceDebitedEvent(String transferId, boolean simulateCreditFailure) {
			this.transferId = transferId;
			this.simulateCreditFailure = simulateCreditFailure;
		}

		private String getTransferId() {
			return transferId;
		}

		private boolean isSimulateCreditFailure() {
			return simulateCreditFailure;
		}
	}

	private static class CreditFailedEvent {
		private final String transferId;
		private final String reason;

		private CreditFailedEvent(String transferId, String reason) {
			this.transferId = transferId;
			this.reason = reason;
		}

		private String getTransferId() {
			return transferId;
		}

		private String getReason() {
			return reason;
		}
	}
}
