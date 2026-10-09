@echo off
title Smart Campus - Stop Nginx

echo ========================================
echo   Stop Nginx
echo ========================================
echo.

cd /d "%~dp0nginx-1.24.0"

echo Stopping Nginx...
nginx.exe -s stop 2>nul

timeout /t 2 /nobreak >nul

tasklist | findstr "nginx.exe" >nul
if %errorlevel%==0 (
    echo Force stopping Nginx...
    taskkill /f /im nginx.exe >nul 2>&1
    echo Stopped.
) else (
    echo Nginx stopped.
)

echo.
pause