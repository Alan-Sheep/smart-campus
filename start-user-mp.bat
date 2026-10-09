@echo off
setlocal
title SmartCampus - 编译用户端微信小程序

echo ========================================
echo   SmartCampus 用户端 微信小程序
echo ========================================
echo.
echo   本脚本会编译小程序代码（监听文件变更）
echo   编译完成后，用微信开发者工具导入：
echo     frontend\dist\build\mp-weixin
echo.
echo   [重要] 微信开发者工具中必须勾选：
echo     详情 - 本地设置 - 不校验合法域名
echo   （因为本地接口是 http://localhost:8080）
echo.
echo   [前提] 后端必须已启动在 8080 端口
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
        echo [错误] 依赖安装失败。
        pause
        exit /b 1
    )
) else (
    echo [1/2] 依赖已存在，跳过安装
)

echo.
echo [2/2] 编译微信小程序（监听变更，Ctrl+C 退出）...
echo.
call npm run dev:mp-weixin

echo.
echo ========================================
echo   编译产物: frontend\dist\build\mp-weixin
echo   请用微信开发者工具「导入项目」选择该目录
echo ========================================
pause
