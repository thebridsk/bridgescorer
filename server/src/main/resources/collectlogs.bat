@echo off
goto cmdline
:help
echo Syntax: %0 [--help]
echo Where:
echo   --help      - show help
echo Notes:
echo   Collects the logs and store and saves it in logs.zip
goto :eof

:cmdline

setlocal
cd %~dp0

call findServerJar.bat
if ERRORLEVEL 1 (
  pause
  exit /b 1
)

echo on
java.exe -jar %xxxserverjar% diagnostics --store ./store --diagnostics ./logs --zip logs.zip
@echo off
