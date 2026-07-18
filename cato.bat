@echo off
rem cato: launcher for the catoscript CLI.
rem Usage: cato run <file.cato>

setlocal

if "%~1"=="" goto usage
if not "%~1"=="run" goto usage
if "%~2"=="" goto usage
if not "%~3"=="" goto usage

set "SCRIPT_FILE=%~2"
if not exist "%SCRIPT_FILE%" (
    echo cato: file not found: %SCRIPT_FILE%
    exit /b 1
)

set "JAR=%USERPROFILE%\.m2\repository\com\catoscript\catoscript\0.3.1-LOCAL\catoscript-0.3.1-LOCAL.jar"
if not exist "%JAR%" (
    echo cato: artifact not found at %JAR%
    echo cato: run 'gradlew publishToMavenLocal' first
    exit /b 1
)

set "DEPS_DIR=%USERPROFILE%\.m2\repository"
set "CP=%JAR%"
for /r "%DEPS_DIR%\org\jetbrains\kotlinx" %%f in (*.jar) do call set "CP=%%CP%%;%%f"
for /r "%DEPS_DIR%\org\jetbrains\kotlin" %%f in (kotlin-stdlib-*.jar kotlin-stdlib-jdk8-*.jar kotlin-stdlib-jdk7-*.jar kotlin-stdlib-common-*.jar) do call set "CP=%%CP%%;%%f"

java -cp "%CP%" com.catoscript.cli.RunScriptKt "%SCRIPT_FILE%"
exit /b %ERRORLEVEL%

:usage
echo usage: cato run ^<file.cato^>
exit /b 2