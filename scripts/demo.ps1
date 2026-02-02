$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$baseUrl = $env:BASE_URL
if (-not $baseUrl) {
    $baseUrl = "http://localhost:8080"
}
$resumeFile = Join-Path $repoRoot "scripts/demo-resume.txt"

if (-not (Test-Path $resumeFile)) {
    throw "Resume file not found: $resumeFile"
}

Write-Host "[demo] Uploading resume..."
$resumeResponse = Invoke-RestMethod -Uri "$baseUrl/api/resumes/upload" `
    -Method Post `
    -Form @{ file = Get-Item $resumeFile; userId = 1 }
$resumeId = $resumeResponse.data
Write-Host "[demo] Resume ID: $resumeId"

Write-Host "[demo] Creating session..."
$sessionPayload = @{ resumeId = $resumeId; durationMinutes = 30 } | ConvertTo-Json
$sessionResponse = Invoke-RestMethod -Uri "$baseUrl/api/interview/sessions" `
    -Method Post `
    -ContentType "application/json" `
    -Body $sessionPayload
$sessionId = $sessionResponse.data
Write-Host "[demo] Session ID: $sessionId"

Write-Host "[demo] Request next question..."
$nextQuestion = Invoke-RestMethod -Uri "$baseUrl/api/interview/sessions/$sessionId/next-question" `
    -Method Post
$nextQuestion | ConvertTo-Json -Depth 5

Write-Host "[demo] Send candidate answer..."
$turnPayload = @{ role = "CANDIDATE"; content = "我在项目中负责接口设计，重点优化了查询性能与缓存策略。" } | ConvertTo-Json
$turnResponse = Invoke-RestMethod -Uri "$baseUrl/api/interview/sessions/$sessionId/turns" `
    -Method Post `
    -ContentType "application/json" `
    -Body $turnPayload
$turnResponse | ConvertTo-Json -Depth 5

Write-Host "[demo] Advance stage..."
$advanceResponse = Invoke-RestMethod -Uri "$baseUrl/api/interview/sessions/$sessionId/stage/next" `
    -Method Post
$advanceResponse | ConvertTo-Json -Depth 5

Write-Host "[demo] End session..."
$endResponse = Invoke-RestMethod -Uri "$baseUrl/api/interview/sessions/$sessionId/end" `
    -Method Post
$endResponse | ConvertTo-Json -Depth 5

Write-Host "[demo] Generate report..."
$reportResponse = Invoke-RestMethod -Uri "$baseUrl/api/interview/sessions/$sessionId/report" `
    -Method Post
$reportResponse | ConvertTo-Json -Depth 6

Write-Host "[demo] Done."
