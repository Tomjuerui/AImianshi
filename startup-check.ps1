# Windows PowerShell 启动验收脚本
# 使用方法: .\startup-check.ps1

Write-Host "=== 1. 环境检查 ===" -ForegroundColor Green
Write-Host "检查 Maven 版本..." -ForegroundColor Yellow
mvn -v
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Maven 未安装或不在 PATH 中" -ForegroundColor Red
    exit 1
}

Write-Host "`n检查 Java 版本..." -ForegroundColor Yellow
java -version
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Java 未安装或不在 PATH 中" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== 2. 运行测试 ===" -ForegroundColor Green
mvn test
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 测试失败" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== 3. 使用 dev profile 启动应用 ===" -ForegroundColor Green
Write-Host "启动命令: mvn spring-boot:run -Dspring-boot.run.profiles=dev" -ForegroundColor Yellow
Write-Host "请在另一个终端窗口运行启动命令，然后按任意键继续验证健康检查..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

Write-Host "`n=== 4. 验证健康检查端点 ===" -ForegroundColor Green
$response = curl.exe -s http://localhost:8080/api/health
if ($LASTEXITCODE -eq 0) {
    Write-Host "健康检查响应:" -ForegroundColor Yellow
    Write-Host $response
    Write-Host "`n✅ 所有验收步骤完成！" -ForegroundColor Green
} else {
    Write-Host "错误: 无法连接到健康检查端点，请确保应用已启动" -ForegroundColor Red
    exit 1
}
