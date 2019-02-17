@echo off
goto cmdline
:help
echo Syntax: %0 [--help] [--shutdown] [--chrome]
echo Where:
echo   --shutdown  - shuts down a server that was started
echo                 with this command
echo   --help      - show help
echo   --chrome    - start up chrome in full screen mode
echo Notes:
echo   If no arguments are given, the server is started 
echo   on port 80, http. When the server is ready, a 
echo   browser window is opened to the server home page.
goto :eof

:cmdline

setlocal
set xxxport=80
cd %~dp0

if ".%1" == ".--shutdown" goto shutdown
if ".%1" == ".-h" goto help
if ".%1" == ".--help" goto help
if ".%1" == "./h" goto help
if ".%1" == "./?" goto help
goto startserver

:startserver
md logs 2>nul
set xxxargs=--logfile logs/server.%%d.%%u.log --memoryfile logs/memory.%%d.csv start --store ./store --browser --diagnostics logs %*
goto runcmd

:shutdown
set xxxargs=--logfile shutdown.log shutdown
goto runcmd

:runcmd
for %%f in (bridgescorer-server-assembly-*.jar) do set xxxjar=%%f
if ".%xxxjar%" == "." (
  echo Did not find bridgescorer-server-assembly-*.jar
  pause
) else (
  echo on
  start "BridgeServer" /MIN java.exe -jar %xxxjar% %xxxargs% --port %xxxport%
  @echo off
)
