#!/usr/bin/env bash
# Smoke test of core functional cases against a running instance.
# Usage: ./scripts/smoke-test.sh http://localhost:8080
BASE=${1:-http://localhost:8080}
pass=0; fail=0
check() { # name expected_code actual_code
  if [ "$2" == "$3" ]; then echo "PASS  $1"; pass=$((pass+1)); else echo "FAIL  $1 (expected $2, got $3)"; fail=$((fail+1)); fi
}
post() { curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/validate" -H 'Content-Type: application/json' -d "$1"; }

check "GET /health"            200 "$(curl -s -o /dev/null -w '%{http_code}' $BASE/health)"
check "valid transaction"      200 "$(post '{"transactionId":"T1","fromAccount":"ACC123456","toAccount":"ACC654321","amount":500,"currency":"INR"}')"
check "negative amount"        400 "$(post '{"transactionId":"T2","fromAccount":"ACC123456","toAccount":"ACC654321","amount":-5,"currency":"INR"}')"
check "same accounts"          400 "$(post '{"transactionId":"T3","fromAccount":"ACC123456","toAccount":"ACC123456","amount":5,"currency":"INR"}')"
check "bad currency"           400 "$(post '{"transactionId":"T4","fromAccount":"ACC123456","toAccount":"ACC654321","amount":5,"currency":"JPY"}')"
check "over limit"             400 "$(post '{"transactionId":"T5","fromAccount":"ACC123456","toAccount":"ACC654321","amount":999999,"currency":"USD"}')"
echo "Passed: $pass  Failed: $fail"
[ $fail -eq 0 ]
