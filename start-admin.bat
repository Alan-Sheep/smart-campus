@echo off
setlocal
title SmartCampus - 启动管理后台

echo ========================================
echo   SmartCampus 管理后台 启动脚本
echo ========================================
echo.
echo   管理后台:  http://localhost:8081
echo   默认账号:  admin / admin123
echo.
echo   [前提] 后端必须已启动在 8080 端口
echo          否则页面接口全部请求失败
echo ========================================
echo.

cd /d "%~dp0admin"

where npm >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 npm，请先安装 Node.js 并加入 PATH。
    pause
    exit /b 1
)

if not exist "node_modules" (
    echo [1/2] 首次运行，正在安装依赖...
    call npm install --no-audit --no-fund
    if errorlevel 1 (
        echo.
        echo [错误] 依赖安装失败，请检查网络。
        pause
        exit /b 1
    )
) else (
    echo [1/2] 依赖已存在，跳过安装
)

echo.
echo [2/2] 启动管理后台开发服务器...
echo.
call npm run dev

echo.
echo [提示] 开发服务器已退出。
pause
