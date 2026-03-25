# Payment Wallet System

Simple Spring Boot backend demo for a Paytm to Uber wallet flow.

This project shows:

- separate wallet data stores for Paytm and Uber
- a coordinator store for transfer state and step history
- a 2PC-style transfer flow using prepare and commit steps
- a saga orchestration flow with compensation
- a saga choreography flow using events inside the app

This is a demo project, so the 2PC flow is implemented as an application-level prepare/commit process, not XA/JTA.

## Tech

- Java 21 target
- Spring Boot 4
- Spring Web MVC
- Spring JDBC
- H2 file databases
- Maven Wrapper

## Project shape

`src/main/java/com/stockpro/wallet/config`

- datasource setup
- demo database reset

`src/main/java/com/stockpro/wallet/paytm`

- Paytm wallet debit, hold, rollback, refund logic

`src/main/java/com/stockpro/wallet/uber`

- Uber wallet credit, hold, rollback logic

`src/main/java/com/stockpro/wallet/transfer`

- transfer APIs
- transfer state store
- 2PC flow
- orchestrated saga flow
- choreography saga flow

## Seed wallets

Paytm:

- `PAYTM_USER_1` balance `5000.00`
- `PAYTM_USER_2` balance `3200.00`

Uber:

- `UBER_DRIVER_1` balance `700.00`
- `UBER_DRIVER_2` balance `950.00`

## Run

From the project folder:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
.\mvnw.cmd test
```

## Important endpoints

Get wallet summary:

```http
GET /api/wallets/summary
```

Get Paytm wallet ledger:

```http
GET /api/wallets/paytm/PAYTM_USER_1/ledger
```

Get Uber wallet ledger:

```http
GET /api/wallets/uber/UBER_DRIVER_1/ledger
```

Reset demo data:

```http
POST /api/demo/reset
```

2PC transfer:

```http
POST /api/transfers/2pc
Content-Type: application/json

{
  "fromWallet": "PAYTM_USER_1",
  "toWallet": "UBER_DRIVER_1",
  "amount": 250.00,
  "currency": "INR",
  "note": "Ride payment"
}
```

Saga orchestration:

```http
POST /api/transfers/saga/orchestrated
Content-Type: application/json

{
  "fromWallet": "PAYTM_USER_1",
  "toWallet": "UBER_DRIVER_1",
  "amount": 300.00,
  "currency": "INR",
  "note": "Ride payout",
  "simulateCreditFailure": true
}
```

Saga choreography:

```http
POST /api/transfers/saga/choreography
Content-Type: application/json

{
  "fromWallet": "PAYTM_USER_2",
  "toWallet": "UBER_DRIVER_2",
  "amount": 200.00,
  "currency": "INR",
  "note": "Choreography payout",
  "simulateCreditFailure": true
}
```

Get one transfer:

```http
GET /api/transfers/{transferId}
```

Get only transfer steps:

```http
GET /api/transfers/{transferId}/steps
```
