# FinSecure Pay
Transaction validation API (Spring Boot 3.3.5, Java 17).

## Run
mvn clean package && java -jar target/finsecure-pay-1.0.0.jar

## Test
mvn test

## Endpoints
- GET /health
- POST /validate  (JSON: transactionId, fromAccount, toAccount, amount, currency)

## Evidence
See docs/evidence/