@echo off
REM ============================================================
REM  智能水电能耗管理平台 — 停止脚本
REM  优雅关闭 Spring Boot 应用 (kill 8080 + 18080 端口进程)
REM ============================================================

echo ============================================================
echo  智能水电能耗管理平台 — 停止服务
echo ============================================================

set "FOUND=0"

REM ── 停止 8080 ──
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING" 2^>nul') do (
    echo [停止] 关闭端口 8080 进程 PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    set "FOUND=1"
)

REM ── 停止 18080 ──
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":18080 " ^| findstr "LISTENING" 2^>nul') do (
    echo [停止] 关闭端口 18080 进程 PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    set "FOUND=1"
)

if "%FOUND%"=="0" (
    echo [INFO] 未找到正在运行的应用进程。
)

del logs\app.pid 2>nul
echo [DONE] 停止完成。
pause
