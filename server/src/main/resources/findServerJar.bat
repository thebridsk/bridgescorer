@echo off
goto cmdline
:help
echo Syntax: %0 [--help] [notthis]
echo Where:
echo   --help      - show help
echo   notthis     - don't return this jar file
echo Notes:
echo   looks for bridgescorer jar files in current directory
echo   If no arguments are given, the server jar is returned 
echo   in the xxxserverjar variable
echo   If server jar file is not found a message is printed to stdout
goto :eof

:cmdline

set xxxserverjar=
for %%f in (bridgescorer-server-assembly-*.jar bridgescorekeeper-*.jar) do (
    if not ".%%f" == ".%1" (
        call :loopbody %%f
        if ERRORLEVEL 1 (
          set xxxserverjar=
          exit /b 1
        )
    )
)
if ".%xxxserverjar%" == "." (
  echo Did not find bridgescorer-server-assembly-*.jar or bridgescorekeeper-*.jar
  exit /b 1
) 

exit /b 0

:loopbody
  if not ".%xxxserverjar%" == "." (
    echo Found multiple %xxxserverjar% %1
    exit /b 1
  ) 
  set xxxserverjar=%1
  exit /b 0
