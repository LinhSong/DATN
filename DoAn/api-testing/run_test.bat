@echo off

set FILE=%1

if "%FILE%"=="" (
    echo ❌ Vui lòng nhập file testcase
    exit /b
)

REM ===== TIME =====
for /f %%i in ('powershell -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TIME=%%i

REM ===== FILE NAME =====
for %%f in (%FILE%) do set NAME=%%~nf

REM ===== RESULT DIR =====
set RESULT_DIR=allure-results\run-%TIME%-%NAME%

echo.
echo 🚀 Running test: %FILE%
echo 📁 Result folder: %RESULT_DIR%

REM ===== TẠO FOLDER TRƯỚC =====
mkdir "%RESULT_DIR%"

REM ===== CHẠY TEST + ÉP ALLURE GHI THẲNG =====
mvn clean test -Dfile=%FILE% -Dallure.results.directory="%RESULT_DIR%" -q

REM ===== COPY categories (nếu có) =====
if exist categories.json (
    copy categories.json "%RESULT_DIR%" > nul
)

echo.
echo ✅ DONE!
echo 👉 allure serve %RESULT_DIR%