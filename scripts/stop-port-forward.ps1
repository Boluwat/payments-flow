#Requires -Version 5.1
<#
.SYNOPSIS
    Stops all kubectl port-forward background jobs started by start-port-forward.ps1.
#>

$ErrorActionPreference = "SilentlyContinue"

$jobs = Get-Job -Name "portfwd-*"

if (-not $jobs) {
    Write-Host "No port-forward jobs found." -ForegroundColor Yellow
    exit 0
}

Write-Host "Stopping $($jobs.Count) port-forward job(s)..." -ForegroundColor Cyan

$jobs | ForEach-Object {
    Stop-Job -Name $_.Name -ErrorAction SilentlyContinue
    Remove-Job -Name $_.Name -ErrorAction SilentlyContinue
    Write-Host "  [-] $($_.Name) stopped" -ForegroundColor Green
}

Write-Host ""
Write-Host "All port-forward jobs have been cleaned up." -ForegroundColor Cyan
