@echo off
title Smart Campus - Start Nginx

echo ========================================
echo   Smart Campus - Start Nginx
echo ========================================
echo.

cd /d "%~dp0nginx-1.24.0"

echo [1/2] Starting Nginx...
start /B nginx.exe

timeout /t 2 /nobreak >nul

echo [2/2] Checking status...
tasklist | findstr "nginx.exe" >nul
if %errorlevel%==0 (
    echo.
    echo ========================================
    echo   Nginx started successfully!
    echo   URL: http://localhost/
    echo   API Docs: http://localhost/doc.html
    echo ========================================
    echo.
    echo Tips:
    echo   - Closing this window does NOT stop Nginx
    echo   - Run stop-nginx.bat to stop Nginx
) else (
    echo.
    echo [ERROR] Nginx failed to start!
    echo Check if port 80 is in use:
    echo   netstat -ano ^| findstr ":80"
)

echo.
pause