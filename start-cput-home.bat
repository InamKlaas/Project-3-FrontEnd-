@echo off
setlocal EnableExtensions DisableDelayedExpansion
title CPUT Home launcher
cd /d "%~dp0"

REM Run with --check to check prerequisites and ports without starting servers.
where java.exe >nul 2>nul
if errorlevel 1 (
    echo [cput-home] Java 21 is required. Install it and add java to PATH.
    goto failed
)
for /f "tokens=3" %%V in ('java.exe -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~V"
if not defined JAVA_VERSION (
    echo [cput-home] Could not determine the Java version.
    goto failed
)
for /f "tokens=1 delims=." %%V in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%V"
if %JAVA_MAJOR% LSS 21 (
    echo [cput-home] Upgrade Java to version 21 or newer.
    goto failed
)

where node.exe >nul 2>nul
if errorlevel 1 (
    echo [cput-home] Node.js 20 or newer is required. Install it first.
    goto failed
)
node -e "if (Number(process.versions.node.split('.')[0]) < 20) process.exit(1)"
if errorlevel 1 (
    echo [cput-home] Upgrade Node.js to version 20 or newer.
    goto failed
)

where npm.cmd >nul 2>nul
if errorlevel 1 (
    echo [cput-home] npm is required. Install it with Node.js.
    goto failed
)

REM Use the full path: calling a quoted bare mvn.cmd breaks Maven's home lookup.
set "MAVEN_CMD="
for %%M in (mvn.cmd) do set "MAVEN_CMD=%%~$PATH:M"
if not defined MAVEN_CMD (
    if not exist "backend\mvnw.cmd" (
        echo [cput-home] Install Maven or restore backend\mvnw.cmd.
        goto failed
    )
    set "MAVEN_CMD=%~dp0backend\mvnw.cmd"
)
if not exist "backend\pom.xml" (
    echo [cput-home] Keep this launcher in the project root beside backend.
    goto failed
)

powershell.exe -NoProfile -Command "$busy = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 8080,5173 }; if ($busy) { $busy | Format-Table LocalAddress,LocalPort,OwningProcess -AutoSize; exit 1 }"
if errorlevel 1 (
    echo [cput-home] Ports 8080 and 5173 must be free. Close the existing servers and try again.
    goto failed
)

REM Load local backend settings without replacing exported environment values.
if exist "backend\.env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("backend\.env") do (
        if not defined %%A set "%%A=%%B"
    )
)

if /I "%~1"=="--check" (
    echo [cput-home] Prerequisite checks passed. Ports 8080 and 5173 are free.
    exit /b 0
)

if not defined DB_USERNAME set "DB_USERNAME=root"
if not defined DB_PASSWORD (
    echo [cput-home] MySQL must already be running. Enter the password for %DB_USERNAME%.
    set /p "DB_PASSWORD=MySQL password (leave blank if none): "
)

REM Pin local development to localhost, regardless of the current Wi-Fi IP.
set "VITE_API_URL=http://localhost:8080/api"
set "CORS_ALLOWED_ORIGINS=http://localhost:5173"

if not exist "node_modules\.bin\vite.cmd" (
    echo [cput-home] Installing frontend dependencies...
    call npm.cmd ci
    if errorlevel 1 goto failed
)

REM Rebuild every time so source and application property changes reach the JAR.
echo [cput-home] Building the backend...
pushd "backend"
call "%MAVEN_CMD%" package -DskipTests
if errorlevel 1 (
    popd
    goto failed
)
popd

echo [cput-home] Starting the backend on http://localhost:8080 ...
start "CPUT Home backend" /D "%~dp0backend" cmd.exe /k "java -jar target\cput-home-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
if errorlevel 1 goto failed

echo [cput-home] Starting the frontend on http://localhost:5173 ...
start "CPUT Home frontend" /D "%~dp0" cmd.exe /k "npm run dev -- --host localhost --port 5173 --strictPort"
if errorlevel 1 goto failed

echo.
echo Wait for the backend window to report Started, then open http://localhost:5173.
echo Backend health: http://localhost:8080/actuator/health
echo Stop each server with Ctrl+C in its window.
exit /b 0

:failed
echo.
echo [cput-home] Startup stopped. Check the error above.
if /I not "%~1"=="--check" pause
exit /b 1
