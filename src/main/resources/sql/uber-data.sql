delete from wallet_ledger;
delete from wallet_holds;
delete from wallet_accounts;

insert into wallet_accounts (wallet_id, owner_name, wallet_type, currency, balance, locked_amount, status, updated_at)
values
('UBER_DRIVER_1', 'Ravi Driver', 'DRIVER', 'INR', 700.00, 0.00, 'ACTIVE', current_timestamp),
('UBER_DRIVER_2', 'Pooja Driver', 'DRIVER', 'INR', 950.00, 0.00, 'ACTIVE', current_timestamp);
