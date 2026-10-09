@echo off
setlocal
title SmartCampus - 启动后端 API

echo ========================================
echo   SmartCampus 后端 API 启动脚本
echo ========================================
echo.
echo   后端 API:  http://localhost:8080
echo   接口文档:  http://localhost:8080/doc.html
echo.
echo   [前提] MySQL(3306) 与 Redis(6379) 必须已启动
echo ========================================
echo.

cd /d "%~dp0backend"

REM ---- 自动定位 JDK 21（项目要求 Java 21，系统默认 java 可能是 1.8） ----
set "JDK21="
if exist "D:\code\get_jobs\jdk-21.0.10+7\bin\java.exe" set "JDK21=D:\code\get_jobs\jdk-21.0.10+7"
if not defined JDK21 for /d %%i in ("D:\JAVA\jdk-21*") do if exist "%%i\bin\java.exe" set "JDK21=%%i"
if not defined JDK21 for /d %%i in ("C:\Program Files\Java\jdk-21*") do if exist "%%i\bin\java.exe" set "JDK21=%%i"
if not defined JDK21 for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do if exist "%%i\bin\java.exe" set "JDK21=%%i"

if not defined JDK21 (
    echo [错误] 未找到 JDK 21，请修改本脚本中的 JDK21 路径。
    echo        项目要求 Java 21，用 JDK 1.8 会编译失败。
    pause
    exit /b 1
)

set "JAVA_HOME=%JDK21%"
echo [检查] 使用 JDK: %JDK21%
"%JDK21%\bin\java.exe" -version 2>&1 | findstr /i "version"
echo.

if exist "target\smart-campus-1.0.0.jar" (
    echo [启动] 使用已构建的 jar...
    echo.
    "%JDK21%\bin\java.exe" -jar "target\smart-campus-1.0.0.jar"
) else (
    echo [启动] 未找到 jar，改用 Maven spring-boot:run ...
    echo.
    set "MVN=D:\JAVA\ideaIC-2024.1.4.win\plugins\maven\lib\maven3\bin\mvn.cmd"
    if not exist "%MVN%" (
        echo [错误] 未找到 Maven: %MVN%
        echo        请先在 IDEA 中构建，或手动执行 mvn package。
        pause
        exit /b 1
    )
    call "%MVN%" spring-boot:run
)

echo.
echo [提示] 后端已退出。
pause
