package com.stockpro.wallet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.stockpro.wallet.config.DatabaseSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentWalletSystemApplicationTests {

	@Autowired
	private DatabaseSetup databaseSetup;

	@Value("${local.server.port}")
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@BeforeEach
	void setUp() {
		databaseSetup.resetAllData();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void twoPhaseTransferShouldCompleteAndMoveMoney() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "PAYTM_USER_1",
				  "toWallet": "UBER_DRIVER_1",
				  "amount": 250.00,
				  "currency": "INR",
				  "note": "Ride payment"
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/2pc", requestBody);
		assertTrue(transferResponse.statusCode() == 200);
		assertTrue(transferResponse.body().contains("\"status\":\"COMPLETED\""));
		assertTrue(transferResponse.body().contains("\"mode\":\"TWO_PHASE_COMMIT\""));

		HttpResponse<String> walletResponse = sendGet("/api/wallets/summary");
		assertTrue(walletResponse.statusCode() == 200);
		assertTrue(walletResponse.body().contains("\"walletId\":\"PAYTM_USER_1\""));
		assertTrue(walletResponse.body().contains("\"balance\":4750.00"));
		assertTrue(walletResponse.body().contains("\"walletId\":\"UBER_DRIVER_1\""));
		assertTrue(walletResponse.body().contains("\"balance\":950.00"));
	}

	@Test
	void orchestratedSagaShouldCompensateWhenUberCreditFails() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "PAYTM_USER_1",
				  "toWallet": "UBER_DRIVER_1",
				  "amount": 300.00,
				  "currency": "INR",
				  "note": "Failed ride payout",
				  "simulateCreditFailure": true
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/saga/orchestrated", requestBody);
		assertTrue(transferResponse.statusCode() == 200);
		assertTrue(transferResponse.body().contains("\"status\":\"COMPENSATED\""));
		assertTrue(transferResponse.body().contains("\"mode\":\"SAGA_ORCHESTRATION\""));

		HttpResponse<String> walletResponse = sendGet("/api/wallets/summary");
		assertTrue(walletResponse.statusCode() == 200);
		assertTrue(walletResponse.body().contains("\"walletId\":\"PAYTM_USER_1\""));
		assertTrue(walletResponse.body().contains("\"balance\":5000.00"));
		assertTrue(walletResponse.body().contains("\"walletId\":\"UBER_DRIVER_1\""));
		assertTrue(walletResponse.body().contains("\"balance\":700.00"));
	}

	@Test
	void choreographySagaShouldCompensateWhenUberCreditFails() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "PAYTM_USER_2",
				  "toWallet": "UBER_DRIVER_2",
				  "amount": 200.00,
				  "currency": "INR",
				  "note": "Choreography payout",
				  "simulateCreditFailure": true
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/saga/choreography", requestBody);
		assertTrue(transferResponse.statusCode() == 200);
		assertTrue(transferResponse.body().contains("\"status\":\"COMPENSATED\""));
		assertTrue(transferResponse.body().contains("\"mode\":\"SAGA_CHOREOGRAPHY\""));

		HttpResponse<String> walletResponse = sendGet("/api/wallets/summary");
		assertTrue(walletResponse.statusCode() == 200);
		assertTrue(walletResponse.body().contains("\"walletId\":\"PAYTM_USER_2\""));
		assertTrue(walletResponse.body().contains("\"balance\":3200.00"));
		assertTrue(walletResponse.body().contains("\"walletId\":\"UBER_DRIVER_2\""));
		assertTrue(walletResponse.body().contains("\"balance\":950.00"));
	}

	@Test
	void walletLedgerEndpointShouldShowTransferEntries() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "PAYTM_USER_1",
				  "toWallet": "UBER_DRIVER_1",
				  "amount": 250.00,
				  "currency": "INR",
				  "note": "Ride payment"
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/2pc", requestBody);
		assertTrue(transferResponse.statusCode() == 200);

		HttpResponse<String> paytmLedgerResponse = sendGet("/api/wallets/paytm/PAYTM_USER_1/ledger");
		assertTrue(paytmLedgerResponse.statusCode() == 200);
		assertTrue(paytmLedgerResponse.body().contains("\"entryType\":\"2PC_DEBIT_COMMIT\""));
		assertTrue(paytmLedgerResponse.body().contains("\"balanceAfter\":4750.00"));

		HttpResponse<String> uberLedgerResponse = sendGet("/api/wallets/uber/UBER_DRIVER_1/ledger");
		assertTrue(uberLedgerResponse.statusCode() == 200);
		assertTrue(uberLedgerResponse.body().contains("\"entryType\":\"2PC_CREDIT_COMMIT\""));
		assertTrue(uberLedgerResponse.body().contains("\"balanceAfter\":950.00"));
	}

	@Test
	void walletLedgerEndpointShouldReturnNotFoundForMissingWallet() throws Exception {
		HttpResponse<String> response = sendGet("/api/wallets/paytm/PAYTM_USER_99/ledger");
		assertTrue(response.statusCode() == 404);
		assertTrue(response.body().contains("Paytm wallet not found"));
	}

	@Test
	void twoPhaseTransferShouldRollbackWhenCommitPhaseFails() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "PAYTM_USER_1",
				  "toWallet": "UBER_DRIVER_1",
				  "amount": 200.00,
				  "currency": "INR",
				  "note": "Commit failure check",
				  "simulateCreditFailure": true
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/2pc", requestBody);
		assertTrue(transferResponse.statusCode() == 200);
		assertTrue(transferResponse.body().contains("\"status\":\"ROLLED_BACK\""));

		String transferId = readTransferId(transferResponse.body());
		HttpResponse<String> stepsResponse = sendGet("/api/transfers/" + transferId + "/steps");
		assertTrue(stepsResponse.statusCode() == 200);
		assertTrue(stepsResponse.body().contains("\"stepName\":\"ROLLBACK_PAYTM_DEBIT\""));
		assertTrue(stepsResponse.body().contains("\"stepName\":\"ROLLBACK_UBER_CREDIT\""));
	}

	@Test
	void transferShouldFailWhenBalanceIsLow() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "PAYTM_USER_1",
				  "toWallet": "UBER_DRIVER_1",
				  "amount": 9000.00,
				  "currency": "INR",
				  "note": "Large payout"
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/saga/orchestrated", requestBody);
		assertTrue(transferResponse.statusCode() == 200);
		assertTrue(transferResponse.body().contains("\"status\":\"FAILED\""));
		assertTrue(transferResponse.body().contains("does not have enough balance"));
	}

	@Test
	void transferShouldRejectWrongWalletPrefix() throws Exception {
		String requestBody = """
				{
				  "fromWallet": "USER_1",
				  "toWallet": "UBER_DRIVER_1",
				  "amount": 150.00,
				  "currency": "INR",
				  "note": "Wrong wallet"
				}
				""";

		HttpResponse<String> transferResponse = sendPost("/api/transfers/2pc", requestBody);
		assertTrue(transferResponse.statusCode() == 400);
		assertTrue(transferResponse.body().contains("fromWallet should be a Paytm wallet"));
	}

	private HttpResponse<String> sendPost(String path, String body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl() + path))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();

		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl() + path)).GET().build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String baseUrl() {
		return "http://localhost:" + port;
	}

	private String readTransferId(String responseBody) {
		String marker = "\"transferId\":\"";
		int start = responseBody.indexOf(marker);

		if (start < 0) {
			return "";
		}

		start = start + marker.length();
		int end = responseBody.indexOf("\"", start);

		if (end < 0) {
			return "";
		}

		return responseBody.substring(start, end);
	}
}
