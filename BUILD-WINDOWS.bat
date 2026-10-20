@echo off
setlocal
cd /d "%~dp0"
title FaithfulBlocks 1.6.0-rc5 Build

echo.
echo Verwendete Java-Version:
java -version
echo.

call gradlew.bat --stop
call gradlew.bat clean build

if errorlevel 1 (
  echo.
  echo ==========================================
  echo BUILD FEHLGESCHLAGEN
  echo ==========================================
  pause
  exit /b 1
)

echo.
echo ==========================================
echo BUILD ERFOLGREICH
echo ==========================================
echo.
echo Die Mod-JAR liegt in:
echo build\libs\faithfulblocks-1.6.0-rc5.jar
echo.
pause
