#Requires -Version 5.1
<#
.SYNOPSIS
    Starts kubectl port-forward tunnels for all payment platform services.

.DESCRIPTION
    After a system restart, run this script to re-expose every service
    on localhost via background port-forward jobs.

    Services exposed:
      - gateway-service     -> localhost:8090
      - payment-service     -> localhost:8080
      - ledger-service      -> localhost:8081
      - settlement-service  -> localhost:8082
      - postgres-payment    -> localhost:5432
      - postgres-ledger     -> localhost:5433
      - postgres-settlement -> localhost:5434
      - prometheus          -> localhost:9090
      - grafana             -> localhost:3000

    IMPORTANT: This script assumes the 'payment' kind cluster exists.
    If the cluster was lost after a restart, run the commands shown below
    to recreate it first.
#>

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# 1. Verify the kind cluster exists
# ---------------------------------------------------------------------------
$clusterInfo = kind get clusters 2>$null | Select-String -Pattern "^payment$"

if (-not $clusterInfo) {
    Write-Host "ERROR: The 'payment' kind cluster was not found." -ForegroundColor Red
    Write-Host ""
    Write-Host "This usually happens after a Windows restart because Docker Desktop"
    Write-Host "stops and the ephemeral kind cluster is destroyed." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To recreate the cluster and redeploy everything, run:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  kind create cluster --name payment --config kind-payment.yaml" -ForegroundColor Green
    Write-Host "  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml"
    Write-Host "  kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s"
    Write-Host "  kubectl create namespace payment"
    Write-Host "  docker-compose build"
    Write-Host "  kind load docker-image payment-payment-service:latest --name payment"
    Write-Host "  kind load docker-image payment-ledger-service:latest --name payment"
    Write-Host "  kind load docker-image payment-settlement-service:latest --name payment"
    Write-Host "  kind load docker-image payment-gateway-service:latest --name payment"
    Write-Host "  kubectl apply -f k8s/"
    Write-Host ""
    Write-Host "Then re-run this script."
    exit 1
}

# ---------------------------------------------------------------------------
# 2. Verify kubectl can reach the cluster
# ---------------------------------------------------------------------------
try {
    kubectl cluster-info | Out-Null
} catch {
    Write-Host "ERROR: kubectl cannot connect to the cluster." -ForegroundColor Red
    Write-Host "Make sure Docker Desktop is running and the 'payment' kind cluster exists." -ForegroundColor Yellow
    exit 1
}

# ---------------------------------------------------------------------------
# 3. Define services to forward
# ---------------------------------------------------------------------------
$forwards = @(
    # App services
    @{ Name = "gateway-service";     Service = "gateway-service";     LocalPort = 8090; RemotePort = 8090 },
    @{ Name = "payment-service";     Service = "payment-service";     LocalPort = 8080; RemotePort = 8080 },
    @{ Name = "ledger-service";      Service = "ledger-service";      LocalPort = 8081; RemotePort = 8081 },
    @{ Name = "settlement-service";  Service = "settlement-service";  LocalPort = 8082; RemotePort = 8082 },
    # Databases
    @{ Name = "postgres-payment";    Service = "postgres-payment";    LocalPort = 5432; RemotePort = 5432 },
    @{ Name = "postgres-ledger";     Service = "postgres-ledger";     LocalPort = 5433; RemotePort = 5432 },
    @{ Name = "postgres-settlement"; Service = "postgres-settlement"; LocalPort = 5434; RemotePort = 5432 },
    # Observability
    @{ Name = "prometheus";          Service = "prometheus";          LocalPort = 9090; RemotePort = 9090 },
    @{ Name = "grafana";             Service = "grafana";             LocalPort = 3000; RemotePort = 3000 }
)

# ---------------------------------------------------------------------------
# 4. Stop any existing port-forward jobs for these services first
# ---------------------------------------------------------------------------
$forwards | ForEach-Object {
    $jobName = "portfwd-$($_.Name)"
    $existing = Get-Job -Name $jobName -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "Stopping existing port-forward job: $jobName" -ForegroundColor DarkYellow
        Stop-Job -Name $jobName -ErrorAction SilentlyContinue
        Remove-Job -Name $jobName -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------------------
# 5. Start new background port-forward jobs
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "Starting kubectl port-forward jobs..." -ForegroundColor Cyan
Write-Host ""

$forwards | ForEach-Object {
    $jobName = "portfwd-$($_.Name)"
    $cmd = "kubectl port-forward service/$($_.Service) $($_.LocalPort):$($_.RemotePort) -n payment"

    try {
        Start-Job -Name $jobName -ScriptBlock {
            param($Command)
            Invoke-Expression $Command
        } -ArgumentList $cmd | Out-Null

        Write-Host "  [+] $($_.Name) -> localhost:$($_.LocalPort)   ($cmd)" -ForegroundColor Green
    } catch {
        Write-Host "  [x] $($_.Name) FAILED: $_" -ForegroundColor Red
    }
}

# ---------------------------------------------------------------------------
# 6. Wait a moment and report status
# ---------------------------------------------------------------------------
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "-----------------------------------------------" -ForegroundColor Cyan
Write-Host "Port-forward status:" -ForegroundColor Cyan
Write-Host "-----------------------------------------------" -ForegroundColor Cyan

$forwards | ForEach-Object {
    $jobName = "portfwd-$($_.Name)"
    $job = Get-Job -Name $jobName -ErrorAction SilentlyContinue
    if ($job -and $job.State -eq "Running") {
        Write-Host "  RUNNING   $($_.Name) -> http://localhost:$($_.LocalPort)" -ForegroundColor Green
    } else {
        Write-Host "  FAILED    $($_.Name)" -ForegroundColor Red
        if ($job) {
            Receive-Job -Name $jobName | Out-String | Write-Host -ForegroundColor DarkGray
        }
    }
}

Write-Host ""
Write-Host "To stop all port-forwards, run: .\scripts\stop-port-forward.ps1" -ForegroundColor Cyan
