@echo off
rem Runs the whole PlaceWise project. Double-click it, or type one of these in a terminal:
rem
rem   run-placewise.bat            builds the program if needed, starts the website and opens it in the browser
rem   run-placewise.bat terminal   the same program as a text menu in this window (the backup for a live demo)
rem   run-placewise.bat test       builds everything and runs ALL tests first (takes a minute or two), then starts the website
rem
rem Needs a JDK (Java 25 or newer) on the PATH. Data is kept in memory (--no-db), so nothing needs to be installed.

cd /d "%~dp0"

rem Step 1: build. "test" runs every test; otherwise the program is only built when target\placewise.jar is missing.
if /i "%1"=="test" (
    echo Building and running all tests...
    call .\mvnw.cmd clean verify
    if errorlevel 1 goto failed
) else if not exist target\placewise.jar (
    echo Building PlaceWise for the first time...
    call .\mvnw.cmd -q package -DskipTests
    if errorlevel 1 goto failed
)

rem Step 2: start the program.
if /i "%1"=="terminal" (
    java -jar target\placewise.jar --no-db
    goto end
)

echo.
echo Starting the website on http://localhost:8080/  -  press Enter in this window to stop it.
rem Open the browser after 3 seconds, so the server is already up when the page loads.
start "" cmd /c "timeout /t 3 /nobreak >nul & start http://localhost:8080/"
java -jar target\placewise.jar --no-db --web
goto end

:failed
echo.
echo The build failed. Read the messages above. Is a JDK (Java 25 or newer) installed?

:end
pause
