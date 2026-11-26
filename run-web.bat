@echo off
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 goto failed
start "Placement Web Server" java -cp out WebApp
ping -n 3 127.0.0.1 >nul
start "" http://localhost:8080
goto end
:failed
echo Compilation failed.
pause
:end
