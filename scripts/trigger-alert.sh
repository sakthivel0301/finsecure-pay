#!/usr/bin/env bash
# Generates traffic so Grafana panels show live data AND triggers the alerts (Task 6).
# Usage: ./scripts/trigger-alert.sh http://localhost:8080
BASE=${1:-http://localhost:8080}
echo "Sending valid + invalid transactions for 90 seconds..."
for i in $(seq 1 450); do
  curl -s -o /dev/null -X POST "$BASE/validate" -H 'Content-Type: application/json' \
    -d '{"transactionId":"X'$i'","fromAccount":"ACC123456","toAccount":"ACC654321","amount":-1,"currency":"INR"}'
  curl -s -o /dev/null -X POST "$BASE/validate" -H 'Content-Type: application/json' \
    -d '{"transactionId":"Y'$i'","fromAccount":"ACC123456","toAccount":"ACC654321","amount":100,"currency":"INR"}'
  curl -s -o /dev/null "$BASE/simulate-error"
  sleep 0.2
done
echo "Done. Check http://localhost:9090/alerts and your mailbox."
