package com.stockpro.wallet.transfer;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final TransferStore transferStore;
	private final TransferService transferService;

	public TransferController(TransferStore transferStore, TransferService transferService) {
		this.transferStore = transferStore;
		this.transferService = transferService;
	}

	@PostMapping("/2pc")
	public TransferDetailsResponse createTwoPhaseTransfer(@Valid @RequestBody TransferRequest request) {
		prepareRequest(request);
		return transferService.createTwoPhaseTransfer(request);
	}

	@PostMapping("/saga/orchestrated")
	public TransferDetailsResponse createOrchestratedSagaTransfer(@Valid @RequestBody TransferRequest request) {
		prepareRequest(request);
		return transferService.createOrchestratedSagaTransfer(request);
	}

	@PostMapping("/saga/choreography")
	public TransferDetailsResponse createChoreographySagaTransfer(@Valid @RequestBody TransferRequest request) {
		prepareRequest(request);
		return transferService.createChoreographyTransfer(request);
	}

	@GetMapping
	public List<TransferRecord> getTransfers() {
		return transferStore.getTransfers();
	}

	@GetMapping("/{transferId}")
	public TransferDetailsResponse getTransfer(@PathVariable String transferId) {
		return transferStore.getTransferDetails(transferId);
	}

	private void prepareRequest(TransferRequest request) {
		request.setFromWallet(request.getFromWallet().trim());
		request.setToWallet(request.getToWallet().trim());
		request.setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT));

		if (request.getNote() != null) {
			request.setNote(request.getNote().trim());
		}

		if (!request.getFromWallet().startsWith("PAYTM_")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"fromWallet should be a Paytm wallet like PAYTM_USER_1");
		}

		if (!request.getToWallet().startsWith("UBER_")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"toWallet should be an Uber wallet like UBER_DRIVER_1");
		}

		if (request.getFromWallet().equalsIgnoreCase(request.getToWallet())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromWallet and toWallet cannot be the same");
		}

		if (!"INR".equalsIgnoreCase(request.getCurrency())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"This demo keeps a single INR flow so edge cases stay easy to understand");
		}
	}
}
