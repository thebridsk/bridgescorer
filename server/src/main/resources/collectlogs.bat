@echo off
goto cmdline
:help
echo Syntax: %0 [--help] 
echo Where:
echo   --help      - show help
echo Notes:
echo   Collects the logs and store
goto :eof

:cmdline

setlocal
cd %~dp0

for %%f in (bridgescorer-server-assembly-*.jar) do set xxxjar=%%f
if ".%xxxjar%" == "." (
  echo Did not find bridgescorer-server-assembly-*.jar
  pause
) else (
  echo on
  java.exe -jar %xxxjar% collectlogs --store ./store --diagnostics ./logs --zip logs.zip
  @echo off
)
