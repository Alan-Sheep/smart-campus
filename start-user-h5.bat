@echo off
setlocal
title SmartCampus - 启动用户端 H5

echo ========================================
echo   SmartCampus 用户端 H5 启动脚本
echo ========================================
echo.
echo   用户端 H5:  http://localhost:5173
echo.
echo   [前提] 后端必须已启动在 8080 端口
echo          Vite 已配置 /api 代理到 8080
echo ========================================
echo.

cd /d "%~dp0frontend"

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
echo [2/2] 启动 H5 开发服务器...
echo.
call npm run dev:h5

echo.
echo [提示] 开发服务器已退出。
pause
