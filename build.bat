@echo off
setlocal

REM ─── Vireon Wild Harvest — Windows build script ──────────────
REM Requires: Java 21 JDK + Maven 3.9+ on PATH.

echo.
echo  Building Vireon Wild Harvest...
echo.

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found on PATH.
    echo         Install from https://maven.apache.org/download.cgi and add the bin\ folder to PATH.
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found on PATH.
    echo         Install JDK 21 from https://adoptium.net/ and add it to PATH / JAVA_HOME.
    exit /b 1
)

call mvn -q clean package
if errorlevel 1 (
    echo.
    echo [FAIL] Build failed.
    exit /b 1
)

echo.
echo  ───────────────────────────────────────────────
echo   Build complete.
echo   Output: target\VireonWildHarvest-0.1.0.jar
echo  ───────────────────────────────────────────────
echo.
echo  Drop that file into your server's plugins\ folder and restart.

endlocal
