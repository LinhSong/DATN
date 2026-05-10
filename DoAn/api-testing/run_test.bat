@echo off

set FILE=%1
set BASE_URL=%2

if "%FILE%"=="" (
    echo  Thiếu file testcase
    exit /b
)

if "%BASE_URL%"=="" (
    echo  Thiếu baseUrl
    exit /b
)

for /f %%i in ('powershell -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TIME=%%i
for %%f in (%FILE%) do set NAME=%%~nf

set RESULT_DIR=allure-results\run-%TIME%-%NAME%

echo.
echo  Running test: %FILE%
echo  Base URL: %BASE_URL%
echo  Result folder: %RESULT_DIR%

mkdir "%RESULT_DIR%"

mvn clean test ^
-q ^
-Dsurefire.printSummary=false ^
-Dsurefire.useFile=false ^
-Dfile="%FILE%" ^
-DbaseUrl="%BASE_URL%" ^
-Dallure.results.directory="%RESULT_DIR%"

echo.
echo  DONE!
echo  allure serve %RESULT_DIR%