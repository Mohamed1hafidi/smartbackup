#!/usr/bin/env pwsh
# ==============================================================
# SmartBackup — Script de vérification post-déploiement
# Usage : .\scripts\verify-deployment.ps1 -BackendUrl "https://..." -FrontendUrl "https://..."
# ==============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$BackendUrl,

    [Parameter(Mandatory=$false)]
    [string]$FrontendUrl = "",

    [Parameter(Mandatory=$false)]
    [string]$SchedulerSecret = ""
)

$ErrorActionPreference = "Continue"
$results = @()

function Test-Endpoint {
    param([string]$Name, [string]$Url, [string]$Method = "GET", [hashtable]$Headers = @{}, [string]$Body = "", [int]$ExpectedStatus = 200)

    try {
        $params = @{
            Uri     = $Url
            Method  = $Method
            Headers = $Headers
            TimeoutSec = 15
            ErrorAction = "Stop"
        }
        if ($Body) { $params.Body = $Body; $params.ContentType = "application/json" }

        $resp = Invoke-WebRequest @params
        $ok   = $resp.StatusCode -eq $ExpectedStatus

        $status = if ($ok) { "PASS" } else { "FAIL" }
        $script:results += [PSCustomObject]@{ Test=$Name; Status=$status; Code=$resp.StatusCode; Detail="" }
        Write-Host "[$status] $Name (HTTP $($resp.StatusCode))" -ForegroundColor $(if($ok){"Green"}else{"Red"})
        return $ok
    }
    catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        $script:results += [PSCustomObject]@{ Test=$Name; Status="FAIL"; Code=$code; Detail=$_.Exception.Message.Split("`n")[0] }
        Write-Host "[FAIL] $Name (Erreur: $($_.Exception.Message.Split('`n')[0]))" -ForegroundColor Red
        return $false
    }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " SmartBackup — Vérification de déploiement" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Backend  : $BackendUrl"
Write-Host " Frontend : $(if($FrontendUrl){"$FrontendUrl"}else{"(non vérifié)"})"
Write-Host ""

# ── 1. Health Check ──────────────────────────────────────────
Write-Host "--- 1. Backend Health ---" -ForegroundColor Yellow
Test-Endpoint "Actuator Health" "$BackendUrl/actuator/health" -ExpectedStatus 200

# ── 2. API Auth ───────────────────────────────────────────────
Write-Host ""
Write-Host "--- 2. API Authentification ---" -ForegroundColor Yellow

$testEmail    = "test-verify-$(Get-Random -Max 9999)@smartbackup.test"
$testPassword = "TestPass123!@"
$registerBody = "{`"email`":`"$testEmail`",`"password`":`"$testPassword`",`"fullName`":`"Test Verify`"}"

$regOk = Test-Endpoint "POST /api/auth/register" `
    "$BackendUrl/api/auth/register" `
    -Method POST `
    -Body $registerBody `
    -ExpectedStatus 200

$token = ""
if ($regOk) {
    $loginBody = "{`"email`":`"$testEmail`",`"password`":`"$testPassword`"}"
    try {
        $loginResp = Invoke-WebRequest -Uri "$BackendUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 15
        $loginJson = $loginResp.Content | ConvertFrom-Json
        $token     = $loginJson.data.accessToken
        $script:results += [PSCustomObject]@{ Test="POST /api/auth/login"; Status="PASS"; Code=200; Detail="Token obtenu" }
        Write-Host "[PASS] POST /api/auth/login (Token: $($token.Substring(0,[Math]::Min(20,$token.Length)))...)" -ForegroundColor Green
    }
    catch {
        $script:results += [PSCustomObject]@{ Test="POST /api/auth/login"; Status="FAIL"; Code=0; Detail=$_.Exception.Message }
        Write-Host "[FAIL] POST /api/auth/login" -ForegroundColor Red
    }
}

# ── 3. API avec authentification ─────────────────────────────
if ($token) {
    Write-Host ""
    Write-Host "--- 3. API Authentifiée ---" -ForegroundColor Yellow
    $authHeaders = @{ "Authorization" = "Bearer $token" }

    Test-Endpoint "GET /api/users/me"           "$BackendUrl/api/users/me"           -Headers $authHeaders
    Test-Endpoint "GET /api/files"              "$BackendUrl/api/files"              -Headers $authHeaders
    Test-Endpoint "GET /api/backups"            "$BackendUrl/api/backups"            -Headers $authHeaders
    Test-Endpoint "GET /api/schedules"          "$BackendUrl/api/schedules"          -Headers $authHeaders
    Test-Endpoint "GET /api/notifications"      "$BackendUrl/api/notifications"      -Headers $authHeaders
    Test-Endpoint "GET /api/dashboard/stats"    "$BackendUrl/api/dashboard/stats"    -Headers $authHeaders
}

# ── 4. Scheduler interne ──────────────────────────────────────
if ($SchedulerSecret) {
    Write-Host ""
    Write-Host "--- 4. Trigger Scheduler (Cloudflare Worker) ---" -ForegroundColor Yellow
    Test-Endpoint "POST /api/internal/scheduler/trigger" `
        "$BackendUrl/api/internal/scheduler/trigger" `
        -Method POST `
        -Headers @{ "X-Scheduler-Secret" = $SchedulerSecret; "Content-Type" = "application/json" } `
        -ExpectedStatus 200
}

# ── 5. Frontend ───────────────────────────────────────────────
if ($FrontendUrl) {
    Write-Host ""
    Write-Host "--- 5. Frontend Vercel ---" -ForegroundColor Yellow
    Test-Endpoint "Frontend index.html" "$FrontendUrl" -ExpectedStatus 200
}

# ── Résumé ────────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$total  = $results.Count

Write-Host " Résultats : $passed/$total réussis" -ForegroundColor $(if($failed -eq 0){"Green"}else{"Yellow"})
if ($failed -gt 0) {
    Write-Host ""
    Write-Host " Tests échoués :" -ForegroundColor Red
    $results | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "   - $($_.Test) (HTTP $($_.Code)) : $($_.Detail)" -ForegroundColor Red
    }
}
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
