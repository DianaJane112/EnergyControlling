@echo off
REM ============================================================
REM  智能水电能耗管理平台 — 生产环境启动脚本 (Windows CMD)
REM  使用方式：双击此文件，或命令行运行 start.bat
REM  启动参数：java -jar ... --spring.profiles.active=prod
REM ============================================================

setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "JAR_FILE=%PROJECT_DIR%EnergyControllerPlatform-2.3.0-RELEASE.jar"
set "LOG_DIR=%PROJECT_DIR%logs"
set "STDOUT_LOG=%LOG_DIR%\sys_stdout.log"
set "PID_FILE=%LOG_DIR%\app.pid"

echo ============================================================
echo  智能水电能耗管理平台 — 生产部署启动
echo  时间: %date% %time%
echo ============================================================

REM ── 1. 创建日志目录 ──
if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
    echo [OK] 日志目录已创建: %LOG_DIR%
)

REM ── 2. 检查 JAR 文件是否存在 ──
if not exist "%JAR_FILE%" (
    echo [ERROR] 未找到 JAR 文件: %JAR_FILE%
    echo         请确认 JAR 包与此脚本在同一目录下。
    pause
    exit /b 1
)
echo [OK] JAR 文件: %JAR_FILE%

REM ── 3. 检测端口 8080 占用 ──
set "PORT_8080_FREE=1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING" 2^>nul') do (
    set "PID_8080=%%a"
    set "PORT_8080_FREE=0"
)
if "!PORT_8080_FREE!"=="0" (
    echo [WARN] 端口 8080 已被占用 (PID: !PID_8080!)
    choice /C YN /T 10 /D N /M "是否终止该进程并继续启动? [Y/N]"
    if errorlevel 2 (
        echo [CANCEL] 用户取消启动。
        pause
        exit /b 1
    )
    taskkill /F /PID !PID_8080! >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo [OK] 端口 8080 已释放。
) else (
    echo [OK] 端口 8080 空闲。
)

REM ── 4. 检测端口 18080 占用（Netty TCP 集中器服务） ──
set "PORT_18080_FREE=1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":18080 " ^| findstr "LISTENING" 2^>nul') do (
    set "PID_18080=%%a"
    set "PORT_18080_FREE=0"
)
if "!PORT_18080_FREE!"=="0" (
    echo [WARN] 端口 18080 已被占用 (PID: !PID_18080!)
    choice /C YN /T 10 /D N /M "是否终止该进程并继续启动? [Y/N]"
    if errorlevel 2 (
        echo [CANCEL] 用户取消启动。
        pause
        exit /b 1
    )
    taskkill /F /PID !PID_18080! >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo [OK] 端口 18080 已释放。
) else (
    echo [OK] 端口 18080 空闲。
)

REM ── 5. 后台启动 Spring Boot 应用 ──
echo [启动] 正在以 prod 配置启动应用...
start /B "" javaw -jar -Dspring.profiles.active=prod "%JAR_FILE%" >> "%STDOUT_LOG%" 2>&1

REM ── 6. 等待启动 ──
echo [等待] 等待应用启动中（最多 60 秒）...
set "STARTED=0"
for /L %%i in (1,1,30) do (
    timeout /t 2 /nobreak >nul
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING" 2^>nul') do (
        set "PID=%%a"
        set "STARTED=1"
    )
    if "!STARTED!"=="1" (
        echo !PID! > "%PID_FILE%"
        echo ============================================================
        echo  启动成功!
        echo  应用端口: 8080 (HTTP)
        echo  集中器端口: 18080 (TCP/Netty)
        echo  进程 PID:  !PID!
        echo  日志文件: %STDOUT_LOG%
        echo  接口文档: http://localhost:8080/doc.html
        echo ============================================================
        goto :end
    )
)

echo [ERROR] 应用启动超时（60秒），请检查日志: %STDOUT_LOG%
pause
exit /b 1

:end
endlocal
pause
exit /b 0
