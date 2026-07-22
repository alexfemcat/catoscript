@echo off
rem cato: launcher for the catoscript CLI.
rem Usage: cato [run|compile] <file.cato>
rem        cato <file.cato>          (defaults to run)

setlocal

if "%~1"=="" goto usage
if "%~1"=="run" goto have_mode
if "%~1"=="compile" goto have_mode
rem First arg is a bare path; default mode = run.
set "MODE=run"
set "SCRIPT_FILE=%~1"
goto check_file

:have_mode
if "%~2"=="" goto usage
if not "%~3"=="" goto usage
set "MODE=%~1"
set "SCRIPT_FILE=%~2"

:check_file
if not exist "%SCRIPT_FILE%" (
    echo cato: file not found: %SCRIPT_FILE%
    exit /b 1
)

set "JAR=%USERPROFILE%\.m2\repository\com\catoscript\catoscript\1.0-LOCAL\catoscript-1.0-LOCAL.jar"
if not exist "%JAR%" (
    echo cato: artifact not found at %JAR%
    echo cato: run 'gradlew publishToMavenLocal' first
    exit /b 1
)

set "DEPS_DIR=%USERPROFILE%\.m2\repository"
set "CP=%JAR%"
for /r "%DEPS_DIR%\org\jetbrains\kotlinx" %%f in (*.jar) do call set "CP=%%CP%%;%%f"
for /r "%DEPS_DIR%\org\jetbrains\kotlin" %%f in (kotlin-stdlib-*.jar kotlin-stdlib-jdk8-*.jar kotlin-stdlib-jdk7-*.jar kotlin-stdlib-common-*.jar) do call set "CP=%%CP%%;%%f"

java -cp "%CP%" com.catoscript.cli.RunScriptKt "%MODE%" "%SCRIPT_FILE%"
exit /b %ERRORLEVEL%

:usage
echo usage: cato [run^|compile] ^<file.cato^>
exit /b 2