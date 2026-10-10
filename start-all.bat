@echo off
chcp 936 >nul
title SmartCampus 一键启动
echo ============================================
echo   SmartCampus 演示环境一键启动
echo ============================================
echo.
echo [1/4] 启动 Redis...
netstat -ano | findstr :6379 | findstr LISTENING >nul
if %errorlevel%==0 (
    echo   Redis 已在运行，跳过
) else (
    start "Redis" /D "D:\Self_Project\Demo\SmartCampus\Redis" redis-server.exe redis-secure.conf
    timeout /t 3 /nobreak >nul
    echo   Redis 已启动（带密码配置）
)
echo.
echo [2/4] 检查 MySQL...
netstat -ano | findstr :3306 | findstr LISTENING >nul
if %errorlevel%==0 (
    echo   MySQL 运行中
) else (
    echo   [警告] MySQL 未启动！请手动执行: net start mysql
)
echo.
echo [3/4] 启动后端 (8080)...
netstat -ano | findstr :8080 | findstr LISTENING >nul
if %errorlevel%==0 (
    echo   后端已在运行，跳过
) else (
    start "SmartCampus-Backend" /D "D:\Self_Project\Demo\SmartCampus" "D:\code\get_jobs\jdk-21.0.10+7\bin\java.exe" -jar backend\target\smart-campus-1.0.0.jar
    echo   后端启动中（约 30 秒后就绪）...
    timeout /t 30 /nobreak >nul
)
echo.
echo [4/4] 启动前端 (5173)...
netstat -ano | findstr :5173 | findstr LISTENING >nul
if %errorlevel%==0 (
    echo   前端已在运行，跳过
) else (
    start "SmartCampus-Frontend" /D "D:\Self_Project\Demo\SmartCampus\frontend" cmd /c "npm run dev:h5"
    echo   前端启动中（约 15 秒）...
    timeout /t 15 /nobreak >nul
)
echo.
echo ============================================
echo   启动完成！
echo.
echo   本地访问:  http://localhost:5173
echo   cpolar 隧道（需要时另开两个 cmd 执行）:
echo     D:\cpolar\cpolar.exe http 8080 --region cn_top
echo     D:\cpolar\cpolar.exe http 5173 --region cn_top
echo ============================================
pause
