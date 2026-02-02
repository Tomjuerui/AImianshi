$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendPort = 8080

if ($IsWindows) {
    $mvnw = Join-Path $repoRoot "mvnw.cmd"
} else {
    $mvnw = Join-Path $repoRoot "mvnw"
}

Write-Host "[dev] Starting backend..."
$backendProcess = Start-Process -FilePath $mvnw `
    -ArgumentList "spring-boot:run", "-Dspring-boot.run.profiles=dev" `
    -WorkingDirectory $repoRoot `
    -PassThru

try {
    Write-Host "[dev] Waiting for backend on port $backendPort..."
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        $check = Test-NetConnection -ComputerName "localhost" -Port $backendPort -WarningAction SilentlyContinue
        if ($check.TcpTestSucceeded) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 1
    }

    if (-not $ready) {
        Write-Warning "Backend did not become ready in time."
    } else {
        Write-Host "[dev] Backend is up."
    }

    Push-Location (Join-Path $repoRoot "frontend")
    if (-not (Test-Path "node_modules")) {
        npm install
    }
    npm run dev
} finally {
    Pop-Location -ErrorAction SilentlyContinue
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Stop-Process -Id $backendProcess.Id
    }
}
