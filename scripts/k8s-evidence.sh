#!/usr/bin/env bash
# Task 3: pods, recovery, rollout, rollback evidence. Run after deploying. Take screenshots of the output.
NS=finsecure
echo "== 1. Pods (expect >=2 Running, READY 1/1) =="
kubectl -n $NS get pods -o wide
echo; echo "== 2. Service / external IP =="
kubectl -n $NS get svc finsecure-pay-svc
echo; echo "== 3. Probes + ConfigMap in Deployment =="
kubectl -n $NS describe deployment finsecure-pay | grep -E "Replicas|Liveness|Readiness|Image|Environment"
echo; echo "== 4. Delete one pod -> automatic recreation =="
POD=$(kubectl -n $NS get pods -l app=finsecure-pay -o jsonpath='{.items[0].metadata.name}')
echo "Deleting $POD"; kubectl -n $NS delete pod "$POD"
kubectl -n $NS get pods -w --request-timeout=45s || true
echo; echo "== 5. Rollout history =="
kubectl -n $NS rollout history deployment/finsecure-pay
echo; echo "== 6. Rollback to previous revision =="
echo "Run:  kubectl -n $NS rollout undo deployment/finsecure-pay && kubectl -n $NS rollout status deployment/finsecure-pay"
