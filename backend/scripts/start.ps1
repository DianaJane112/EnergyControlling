# ============================================================
#  智能水电能耗管理平台 — 生产环境启动脚本 (PowerShell)
#  使用方式：右键 "使用 PowerShell 运行"，或：
#           powershell -ExecutionPolicy Bypass -File start.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarFile   = Join-Path $ProjectDir "EnergyControllerPlatform-2.3.0-RELEASE.jar"
$LogDir    = Join-Path $ProjectDir "logs"
$StdoutLog = Join-Path $LogDir "sys_stdout.log"
$PidFile   = Join-Path $LogDir "app.pid"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  智能水电能耗管理平台 — 生产部署启动 (PowerShell)" -ForegroundColor Cyan
Write-Host "  时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# ── 1. 创建日志目录 ──
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
    Write-Host "[OK] 日志目录已创建: $LogDir" -ForegroundColor Green
}

# ── 2. 检查 JAR 文件 ──
if (-not (Test-Path $JarFile)) {
    Write-Host "[ERROR] 未找到 JAR 文件: $JarFile" -ForegroundColor Red
    Write-Host "       请确认 JAR 包与此脚本在同一目录下。"
    Read-Host "按回车键退出"
    exit 1
}
Write-Host "[OK] JAR 文件: $JarFile" -ForegroundColor Green

# ── 3. 端口占用检查与清理 ──
function Test-AndKill-Port {
    param([int]$Port, [string]$Label)

    $conn = netstat -ano | Select-String ":$Port " | Select-String "LISTENING"
    if ($conn) {
        $pidStr = ($conn -split '\s+')[-1]
        Write-Host "[WARN] 端口 $Port ($Label) 已被占用 (PID: $pidStr)" -ForegroundColor Yellow
        $choice = Read-Host "是否终止该进程并继续启动? [Y/N]"
        if ($choice -ne 'Y' -and $choice -ne 'y') {
            Write-Host "[CANCEL] 用户取消启动。" -ForegroundColor Red
            exit 1
        }
        Stop-Process -Id $pidStr -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        Write-Host "[OK] 端口 $Port 已释放。" -ForegroundColor Green
    } else {
        Write-Host "[OK] 端口 $Port 空闲。" -ForegroundColor Green
    }
}

Test-AndKill-Port -Port 8080  -Label "HTTP 应用"
Test-AndKill-Port -Port 18080 -Label "Netty TCP 集中器"

# ── 4. 后台启动 Java 进程 ──
Write-Host "[启动] 正在以 prod 配置启动应用..." -ForegroundColor Cyan

$proc = Start-Process -FilePath "javaw" `
    -ArgumentList "-jar", "-Dspring.profiles.active=prod", "`"$JarFile`"" `
    -RedirectStandardOutput $StdoutLog `
    -RedirectStandardError (Join-Path $LogDir "sys_stderr.log") `
    -WorkingDirectory $ProjectDir `
    -NoNewWindow `
    -PassThru

$proc.Id | Out-File -FilePath $PidFile -Encoding UTF8

# ── 5. 等待端口就绪 ──
Write-Host "[等待] 等待应用启动中（最多 60 秒）..." -ForegroundColor Cyan
$maxWait = 60
for ($i = 0; $i -lt $maxWait; $i += 2) {
    Start-Sleep -Seconds 2
    $listening = netstat -ano | Select-String ":8080 " | Select-String "LISTENING"
    if ($listening) {
        Write-Host "============================================================" -ForegroundColor Green
        Write-Host "  启动成功!" -ForegroundColor Green
        Write-Host "  应用端口:   8080 (HTTP)" -ForegroundColor Green
        Write-Host "  集中器端口: 18080 (TCP/Netty)" -ForegroundColor Green
        Write-Host "  进程 PID:   $($proc.Id)" -ForegroundColor Green
        Write-Host "  日志文件:   $StdoutLog" -ForegroundColor Green
        Write-Host "  接口文档:   http://localhost:8080/doc.html" -ForegroundColor Green
        Write-Host "============================================================" -ForegroundColor Green
        Read-Host "按回车键退出"
        exit 0
    }
}

Write-Host "[ERROR] 应用启动超时（${maxWait}秒），请检查日志: $StdoutLog" -ForegroundColor Red
Read-Host "按回车键退出"
exit 1
