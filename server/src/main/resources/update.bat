@echo off
setlocal
goto cmdline
:help
echo Syntax: %0 [--help] --update
echo Where:
echo   --update  - update the server
echo   --help    - show help
goto :eof

:docmd
echo %*
%*
goto :eof

:cmdline

setlocal
cd %~dp0

if ".%~1" == "." goto help
if ".%~1" == "--help" goto help

rem Never use the doituclkedgj argument from the command line.
if ".%~1" == ".doituclkedgj" goto doit

rem need to copy this file and run from the copy,
rem because the update process may change this file
cd %~dp0
md save
copy update.bat save\tempupdate.bat

rem the line after the save\tempupdate.bat execution is not executed
rem since it is invoking a bat file without a call statement.
rem the goto :eof after is just put in for documentation.
save\tempupdate.bat doituclkedgj
goto :eof

:doit
cd %~dp0..

set xxxoldjar=
for %%f in (bridgescorer-server-assembly-*.jar bridgescorekeeper-*.jar) do (
  if not ".%xxxoldjar%" == "." (
    echo Found multiple %xxxoldjar% %%f
    pause
    goto :eof
  ) 
  set xxxoldjar=%%f
)
if ".%xxxoldjar%" == "." (
  echo Did not find bridgescorer-server-assembly-*.jar or bridgescorekeeper-*.jar
  pause
  goto :eof
) 

call :docmd java.exe -jar %xxxoldjar% update
if ERRORLEVEL 1 (
  pause
  goto :eof
)

for %%f in (bridgescorer-server-assembly-*.jar bridgescorekeeper-*.jar) do (
  if not "%%f" == "%xxxoldjar%" set xxxnewjar=%%f
)
if ".%xxxnewjar%" == "." (
  echo Did not find new bridgescorer-server-assembly-*.jar or bridgescorekeeper-*.jar
  pause
  goto :eof
) 

md save
move %xxxoldjar% save\
move %xxxoldjar%.sha256 save\

call :docmd java.exe -jar %xxxnewjar% install
goto :eof
