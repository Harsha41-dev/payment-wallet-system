delete from wallet_ledger;
delete from wallet_holds;
delete from wallet_accounts;

insert into wallet_accounts (wallet_id, owner_name, wallet_type, currency, balance, locked_amount, status, updated_at)
values
('PAYTM_USER_1', 'Aman Customer', 'CUSTOMER', 'INR', 5000.00, 0.00, 'ACTIVE', current_timestamp),
('PAYTM_USER_2', 'Neha Customer', 'CUSTOMER', 'INR', 3200.00, 0.00, 'ACTIVE', current_timestamp);
