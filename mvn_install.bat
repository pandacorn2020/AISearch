@echo off
setlocal EnableExtensions

REM Always run from the script directory.
cd /d "%~dp0"

REM Default JDK for this project. You can pass another JDK path as arg1.
set "TARGET_JAVA_HOME=D:\v4_dev_env\jdk-17.0.6"
if not "%~1"=="" set "TARGET_JAVA_HOME=%~1"
set "JAVA_CMD=java"

if exist "%TARGET_JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=%TARGET_JAVA_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    echo [WARN] JDK path not found: %TARGET_JAVA_HOME%
    echo [WARN] Continue with current JAVA_HOME: %JAVA_HOME%
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

echo [INFO] JAVA_HOME: %JAVA_HOME%
echo [INFO] JAVA_CMD : %JAVA_CMD%

echo [INFO] Java runtime:
"%JAVA_CMD%" -version
if errorlevel 1 goto :fail

echo [INFO] Maven runtime:
call mvn -v
if errorlevel 1 goto :fail

echo [INFO] Running build: mvn install -DskipTests
call mvn install -DskipTests
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" goto :fail_with_rc

echo [INFO] Build finished successfully.
exit /b 0

:fail
echo [ERROR] Java or Maven is not available in PATH.
exit /b 1

:fail_with_rc
echo [ERROR] Build failed with exit code %RC%.
exit /b %RC%
