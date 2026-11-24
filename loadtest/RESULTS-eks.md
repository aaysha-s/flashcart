# EKS autoscaling run — 24 Nov 2026

**Cluster:** Amazon EKS 1.37, 2 × m7i-flex.large (ap-southeast-2), created with `infra-eks/` Terraform.
Postgres, Kafka and Redis ran as pods in the cluster. Traffic reached ingress-nginx over a NodePort
(this AWS account can't create load balancers).

**Load:** `loadtest/sustained.js`, constant arrival rate of 30 orders/s for 4 min, run from India.

| Metric | Result |
|---|---|
| Orders placed | 7,201 (29.6/s) |
| Failed requests | 0.00% (0 / 7,203) |
| Checks | 100% `202 Accepted` |
| p95 latency | 428 ms (minimum 384 ms; mostly the India ↔ Sydney round trip) |

**Autoscaling (HPA on order-service, CPU target 300% of request, 1–3 pods):**

| Event | After the load started |
|---|---|
| HPA scales 1 → 3 | ~25 s |
| New pods pass readiness and take traffic | ~75 s |
| 3 → 2 (load steady, pods warmed up) | ~2.5 min |
| Load ends | 4 min |
| 2 → 1 | ~55 s after the load ended |

No requests failed during either scale-up or scale-down.