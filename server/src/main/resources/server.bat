@echo off
goto cmdline
:help
echo Syntax: %0 [--help] [--shutdown]
echo Where:
echo   --shutdown  - shuts down a server that was started
echo                 with this command
echo   --help      - show help
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
if not ".%1" == "." goto help
goto startserver

:startserver
set xxxargs=--logfile server.%%u.log start --store ./store --browser
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
