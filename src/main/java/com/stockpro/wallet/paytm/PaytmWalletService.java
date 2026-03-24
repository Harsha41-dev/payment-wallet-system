package com.stockpro.wallet.paytm;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.stockpro.wallet.wallets.WalletSummary;

@Service
public class PaytmWalletService {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	public PaytmWalletService(@Qualifier("paytmJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
			@Qualifier("paytmTransactionManager") PlatformTransactionManager transactionManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public List<WalletSummary> getWallets() {
		return jdbcTemplate.query(
				"select wallet_id, owner_name, wallet_type, currency, balance, locked_amount, status, updated_at "
						+ "from wallet_accounts order by wallet_id",
				walletRowMapper());
	}

	public WalletSummary getWallet(String walletId) {
		List<WalletSummary> wallets = jdbcTemplate.query(
				"select wallet_id, owner_name, wallet_type, currency, balance, locked_amount, status, updated_at "
						+ "from wallet_accounts where wallet_id = :walletId",
				new MapSqlParameterSource("walletId", walletId), walletRowMapper());

		if (wallets.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paytm wallet not found: " + walletId);
		}

		return wallets.get(0);
	}

	public void prepareDebit(String transferId, String walletId, BigDecimal amount, String note) {
		transactionTemplate.executeWithoutResult(status -> {
			WalletSummary wallet = getWallet(walletId);
			BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getLockedAmount());

			if (availableBalance.compareTo(amount) < 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Paytm wallet does not have enough balance for transfer");
			}

			LocalDateTime now = LocalDateTime.now();
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("walletId", walletId);
			params.addValue("amount", amount);
			params.addValue("now", now);

			jdbcTemplate.update(
					"update wallet_accounts set locked_amount = locked_amount + :amount, updated_at = :now where wallet_id = :walletId",
					params);

			MapSqlParameterSource holdParams = new MapSqlParameterSource();
			holdParams.addValue("transferId", transferId);
			holdParams.addValue("walletId", walletId);
			holdParams.addValue("amount", amount);
			holdParams.addValue("note", note);
			holdParams.addValue("now", now);

			jdbcTemplate.update(
					"insert into wallet_holds (transfer_id, wallet_id, amount, hold_type, status, note, created_at, updated_at) "
							+ "values (:transferId, :walletId, :amount, 'DEBIT', 'PREPARED', :note, :now, :now)",
					holdParams);
		});
	}

	public void commitPreparedDebit(String transferId, String note) {
		transactionTemplate.executeWithoutResult(status -> {
			HoldRecord hold = getHold(transferId);

			if (hold == null || !"PREPARED".equals(hold.status)) {
				return;
			}

			LocalDateTime now = LocalDateTime.now();
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("walletId", hold.walletId);
			params.addValue("transferId", transferId);
			params.addValue("amount", hold.amount);
			params.addValue("now", now);

			jdbcTemplate.update(
					"update wallet_accounts set balance = balance - :amount, locked_amount = locked_amount - :amount, updated_at = :now "
							+ "where wallet_id = :walletId",
					params);

			jdbcTemplate.update("update wallet_holds set status = 'COMMITTED', updated_at = :now where transfer_id = :transferId",
					params);

			WalletSummary wallet = getWallet(hold.walletId);
			addLedgerEntry(transferId, hold.walletId, "2PC_DEBIT_COMMIT", hold.amount, wallet.getBalance(), note, now);
		});
	}

	public void rollbackPreparedDebit(String transferId, String note) {
		transactionTemplate.executeWithoutResult(status -> {
			HoldRecord hold = getHold(transferId);

			if (hold == null || !"PREPARED".equals(hold.status)) {
				return;
			}

			LocalDateTime now = LocalDateTime.now();
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("walletId", hold.walletId);
			params.addValue("transferId", transferId);
			params.addValue("amount", hold.amount);
			params.addValue("now", now);

			jdbcTemplate.update(
					"update wallet_accounts set locked_amount = locked_amount - :amount, updated_at = :now where wallet_id = :walletId",
					params);

			jdbcTemplate.update(
					"update wallet_holds set status = 'ROLLED_BACK', updated_at = :now where transfer_id = :transferId", params);

			WalletSummary wallet = getWallet(hold.walletId);
			addLedgerEntry(transferId, hold.walletId, "2PC_DEBIT_ROLLBACK", hold.amount, wallet.getBalance(), note, now);
		});
	}

	public void debitForSaga(String transferId, String walletId, BigDecimal amount, String note) {
		transactionTemplate.executeWithoutResult(status -> {
			if (ledgerEntryExists(transferId, "SAGA_DEBIT")) {
				return;
			}

			WalletSummary wallet = getWallet(walletId);

			if (wallet.getBalance().compareTo(amount) < 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Paytm wallet does not have enough balance for saga transfer");
			}

			LocalDateTime now = LocalDateTime.now();
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("walletId", walletId);
			params.addValue("amount", amount);
			params.addValue("now", now);

			jdbcTemplate.update("update wallet_accounts set balance = balance - :amount, updated_at = :now where wallet_id = :walletId",
					params);

			WalletSummary updatedWallet = getWallet(walletId);
			addLedgerEntry(transferId, walletId, "SAGA_DEBIT", amount, updatedWallet.getBalance(), note, now);
		});
	}

	public void refundSagaDebit(String transferId, String walletId, BigDecimal amount, String note) {
		transactionTemplate.executeWithoutResult(status -> {
			if (ledgerEntryExists(transferId, "SAGA_REFUND")) {
				return;
			}

			LocalDateTime now = LocalDateTime.now();
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("walletId", walletId);
			params.addValue("amount", amount);
			params.addValue("now", now);

			jdbcTemplate.update("update wallet_accounts set balance = balance + :amount, updated_at = :now where wallet_id = :walletId",
					params);

			WalletSummary updatedWallet = getWallet(walletId);
			addLedgerEntry(transferId, walletId, "SAGA_REFUND", amount, updatedWallet.getBalance(), note, now);
		});
	}

	private HoldRecord getHold(String transferId) {
		List<HoldRecord> holds = jdbcTemplate.query(
				"select transfer_id, wallet_id, amount, status from wallet_holds where transfer_id = :transferId",
				new MapSqlParameterSource("transferId", transferId), (rs, rowNum) -> {
					HoldRecord hold = new HoldRecord();
					hold.transferId = rs.getString("transfer_id");
					hold.walletId = rs.getString("wallet_id");
					hold.amount = rs.getBigDecimal("amount");
					hold.status = rs.getString("status");
					return hold;
				});

		if (holds.isEmpty()) {
			return null;
		}

		return holds.get(0);
	}

	private boolean ledgerEntryExists(String transferId, String entryType) {
		Integer count = jdbcTemplate.queryForObject(
				"select count(*) from wallet_ledger where transfer_id = :transferId and entry_type = :entryType",
				new MapSqlParameterSource().addValue("transferId", transferId).addValue("entryType", entryType),
				Integer.class);

		return count != null && count > 0;
	}

	private void addLedgerEntry(String transferId, String walletId, String entryType, BigDecimal amount,
			BigDecimal balanceAfter, String note, LocalDateTime now) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("transferId", transferId);
		params.addValue("walletId", walletId);
		params.addValue("entryType", entryType);
		params.addValue("amount", amount);
		params.addValue("balanceAfter", balanceAfter);
		params.addValue("note", note);
		params.addValue("now", now);

		jdbcTemplate.update(
				"insert into wallet_ledger (transfer_id, wallet_id, entry_type, amount, balance_after, note, created_at) "
						+ "values (:transferId, :walletId, :entryType, :amount, :balanceAfter, :note, :now)",
				params);
	}

	private RowMapper<WalletSummary> walletRowMapper() {
		return new RowMapper<>() {
			@Override
			public WalletSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new WalletSummary(rs.getString("wallet_id"), rs.getString("owner_name"),
						rs.getString("wallet_type"), rs.getString("currency"), rs.getBigDecimal("balance"),
						rs.getBigDecimal("locked_amount"), rs.getString("status"),
						rs.getTimestamp("updated_at").toLocalDateTime());
			}
		};
	}

	private static class HoldRecord {
		private String transferId;
		private String walletId;
		private BigDecimal amount;
		private String status;
	}
}
