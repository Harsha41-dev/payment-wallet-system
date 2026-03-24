package com.stockpro.wallet.transfer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransferStore {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	public TransferStore(@Qualifier("controlJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
			@Qualifier("controlTransactionManager") PlatformTransactionManager transactionManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public String startTransfer(TransferMode mode, TransferRequest request) {
		String transferId = UUID.randomUUID().toString();
		LocalDateTime now = LocalDateTime.now();

		transactionTemplate.executeWithoutResult(status -> {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("transferId", transferId);
			params.addValue("mode", mode.name());
			params.addValue("fromWallet", request.getFromWallet());
			params.addValue("toWallet", request.getToWallet());
			params.addValue("amount", request.getAmount());
			params.addValue("currency", request.getCurrency());
			params.addValue("note", request.getNote());
			params.addValue("status", TransferStatus.STARTED.name());
			params.addValue("failureReason", null);
			params.addValue("simulateCreditFailure", request.isSimulateCreditFailure());
			params.addValue("now", now);

			jdbcTemplate.update(
					"insert into transfer_requests (transfer_id, mode, from_wallet, to_wallet, amount, currency, note, status, failure_reason, simulate_credit_failure, created_at, updated_at) "
							+ "values (:transferId, :mode, :fromWallet, :toWallet, :amount, :currency, :note, :status, :failureReason, :simulateCreditFailure, :now, :now)",
					params);
		});

		return transferId;
	}

	public void updateStatus(String transferId, TransferStatus status, String failureReason) {
		transactionTemplate.executeWithoutResult(transactionStatus -> {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("transferId", transferId);
			params.addValue("status", status.name());
			params.addValue("failureReason", failureReason);
			params.addValue("now", LocalDateTime.now());

			jdbcTemplate.update(
					"update transfer_requests set status = :status, failure_reason = :failureReason, updated_at = :now where transfer_id = :transferId",
					params);
		});
	}

	public void addStep(String transferId, String stepName, String stepStatus, String message) {
		transactionTemplate.executeWithoutResult(transactionStatus -> {
			Integer maxStepOrder = jdbcTemplate.queryForObject(
					"select coalesce(max(step_order), 0) from transfer_steps where transfer_id = :transferId",
					new MapSqlParameterSource("transferId", transferId), Integer.class);
			int nextStepOrder = maxStepOrder != null ? maxStepOrder + 1 : 1;

			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("transferId", transferId);
			params.addValue("stepOrder", nextStepOrder);
			params.addValue("stepName", stepName);
			params.addValue("stepStatus", stepStatus);
			params.addValue("message", message);
			params.addValue("now", LocalDateTime.now());

			jdbcTemplate.update(
					"insert into transfer_steps (transfer_id, step_order, step_name, step_status, message, created_at) "
							+ "values (:transferId, :stepOrder, :stepName, :stepStatus, :message, :now)",
					params);
		});
	}

	public TransferDetailsResponse getTransferDetails(String transferId) {
		return new TransferDetailsResponse(getTransfer(transferId), getTransferSteps(transferId));
	}

	public TransferRecord getTransfer(String transferId) {
		List<TransferRecord> transfers = jdbcTemplate.query(
				"select transfer_id, mode, from_wallet, to_wallet, amount, currency, note, status, failure_reason, simulate_credit_failure, created_at, updated_at "
						+ "from transfer_requests where transfer_id = :transferId",
				new MapSqlParameterSource("transferId", transferId), transferRowMapper());

		if (transfers.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found: " + transferId);
		}

		return transfers.get(0);
	}

	public List<TransferRecord> getTransfers() {
		return jdbcTemplate.query(
				"select transfer_id, mode, from_wallet, to_wallet, amount, currency, note, status, failure_reason, simulate_credit_failure, created_at, updated_at "
						+ "from transfer_requests order by created_at desc",
				transferRowMapper());
	}

	public List<TransferStepRecord> getTransferSteps(String transferId) {
		return jdbcTemplate.query(
				"select id, step_order, step_name, step_status, message, created_at from transfer_steps where transfer_id = :transferId order by step_order",
				new MapSqlParameterSource("transferId", transferId), stepRowMapper());
	}

	private RowMapper<TransferRecord> transferRowMapper() {
		return new RowMapper<>() {
			@Override
			public TransferRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
				TransferRecord record = new TransferRecord();
				record.setTransferId(rs.getString("transfer_id"));
				record.setMode(TransferMode.valueOf(rs.getString("mode")));
				record.setFromWallet(rs.getString("from_wallet"));
				record.setToWallet(rs.getString("to_wallet"));
				record.setAmount(rs.getBigDecimal("amount"));
				record.setCurrency(rs.getString("currency"));
				record.setNote(rs.getString("note"));
				record.setStatus(TransferStatus.valueOf(rs.getString("status")));
				record.setFailureReason(rs.getString("failure_reason"));
				record.setSimulateCreditFailure(rs.getBoolean("simulate_credit_failure"));
				record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
				record.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
				return record;
			}
		};
	}

	private RowMapper<TransferStepRecord> stepRowMapper() {
		return new RowMapper<>() {
			@Override
			public TransferStepRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
				TransferStepRecord record = new TransferStepRecord();
				record.setId(rs.getLong("id"));
				record.setStepOrder(rs.getInt("step_order"));
				record.setStepName(rs.getString("step_name"));
				record.setStepStatus(rs.getString("step_status"));
				record.setMessage(rs.getString("message"));
				record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
				return record;
			}
		};
	}
}
