# Kubernetes Deployment — Payment Platform

Self-contained plain-YAML manifests for a `kind` cluster. No Helm, no operators, no external prerequisites beyond Docker and `kubectl`.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / Mac / Linux)
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) (`choco install kind` on Windows)
- `kubectl` (comes with Docker Desktop or install separately)

---

## Quick Start (One-Time Setup)

### 1. Create the Kind Cluster

From the repo root:

```powershell
kind create cluster --name payment --config kind-payment.yaml
```

This creates a single-node cluster with host port `8080` mapped so you can reach the nginx ingress controller via `http://localhost:8080`.

### 2. Install Nginx Ingress Controller

```powershell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
```

Wait for it to be ready (~1 minute):

```powershell
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s
```

### 3. Create Namespace

```powershell
kubectl create namespace payment
```

### 4. Build App Images

```powershell
docker-compose build
```

### 5. Load Images into Kind

```powershell
kind load docker-image payment-payment-service:latest --name payment
kind load docker-image payment-ledger-service:latest --name payment
kind load docker-image payment-settlement-service:latest --name payment
kind load docker-image payment-gateway-service:latest --name payment
```

> If your image names differ, run `docker images` to check and adjust the commands above.

### 6. Deploy Everything

```powershell
kubectl apply -f k8s/
```

### 7. Wait for Pods

```powershell
kubectl get pods -n payment -w
```

All pods should reach `Running` and `1/1` within 2–3 minutes.

---

## Access the App

| URL | Service |
|-----|---------|
| `http://localhost:8080/actuator/health` | Gateway health |
| `http://localhost:8080/api/v1/payments/` | Payment API (via gateway) |
| `http://localhost:8080/api/v1/ledger/` | Ledger API (via gateway) |
| `http://localhost:8080/api/v1/settlements/` | Settlement API (via gateway) |
| `http://localhost:30030` | Grafana (`admin` / `admin`) |
| `http://localhost:30090` | Prometheus UI |

---

## Access via Port-Forward (Alternative)

If you prefer to reach every service directly on localhost — including the internal databases — use the helper scripts in `scripts/`.

### Start all port-forwards

From the repo root:

```powershell
.\scripts\start-port-forward.ps1
```

This starts background `kubectl port-forward` jobs for every service.  
If the `payment` kind cluster was lost after a restart, the script prints the exact commands needed to recreate it.

### Ports exposed

| Service | Local URL |
|---------|-----------|
| Gateway API | `http://localhost:8090` |
| Payment service (direct) | `http://localhost:8080` |
| Ledger service (direct) | `http://localhost:8081` |
| Settlement service (direct) | `http://localhost:8082` |
| Postgres — Payment | `localhost:5432` |
| Postgres — Ledger | `localhost:5433` |
| Postgres — Settlement | `localhost:5434` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

### Stop all port-forwards

```powershell
.\scripts\stop-port-forward.ps1
```

---

## Teardown

```powershell
kind delete cluster --name payment
```

---

## Troubleshooting

### Pod stays `Pending`

Check events:
```powershell
kubectl describe pod <pod-name> -n payment
```

### Pod shows `ImagePullBackOff`

You forgot to load the image into kind. Re-run step 5:
```powershell
kind load docker-image payment-payment-service:latest --name payment
```

### Gateway returns 502

The Java app hasn't finished starting. Spring Boot apps take 30–60 seconds. Check logs:
```powershell
kubectl logs -n payment deployment/gateway-service --follow
```

### Ingress returns 404

Make sure the nginx ingress controller is fully ready (step 2).

### Cannot reach `localhost:8080`

If you're on Windows with WSL2 backend, try `http://127.0.0.1:8080` instead of `http://localhost:8080`.

---

## File Reference

| File | What It Creates |
|------|-----------------|
| `db-secret.yaml` | PostgreSQL password secret |
| `postgres-payment.yaml` | Payment DB StatefulSet + Service |
| `postgres-ledger.yaml` | Ledger DB StatefulSet + Service |
| `postgres-settlement.yaml` | Settlement DB StatefulSet + Service |
| `ledger-service.yaml` | Ledger app Deployment + Service |
| `settlement-service.yaml` | Settlement app Deployment + Service |
| `payment-service.yaml` | Payment app Deployment + Service |
| `gateway-service.yaml` | Gateway app Deployment + ClusterIP Service |
| `prometheus-configmap.yaml` | Prometheus scrape config |
| `prometheus.yaml` | Prometheus Deployment + NodePort Service |
| `grafana-configmap.yaml` | Grafana datasource provisioning |
| `grafana.yaml` | Grafana Deployment + NodePort Service |
| `ingress.yaml` | Nginx Ingress routing everything to Gateway |
