@echo off
setlocal
set DIR=%~dp0
set WRAPPER=%DIR%gradle\wrapper\gradle-wrapper.jar
set WRAPPER_URL=https://raw.githubusercontent.com/gradle/gradle/v9.7.1/gradle/wrapper/gradle-wrapper.jar
set WRAPPER_SHA=7A9CE74CFF467CA1BF60A4FCD9F05185ACCEDA4D0F382434D393E17864262C5D

where java >nul 2>nul
if errorlevel 1 (
  echo Java wurde nicht gefunden. Bitte JDK 25 installieren und java zum PATH hinzufuegen.
  exit /b 1
)

if not exist "%WRAPPER%" (
  echo Lade offiziellen Gradle Wrapper herunter...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%WRAPPER_URL%' -OutFile '%WRAPPER%'"
  if errorlevel 1 exit /b 1
)

for /f %%H in ('powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 '%WRAPPER%').Hash"') do set ACTUAL_SHA=%%H
if /I not "%ACTUAL_SHA%"=="%WRAPPER_SHA%" (
  echo Fehler: Gradle Wrapper Pruefsumme stimmt nicht.
  del /q "%WRAPPER%" >nul 2>nul
  exit /b 1
)

java -classpath "%WRAPPER%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
