@echo off
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 goto failed
java -cp out Main
goto end
:failed
echo Compilation failed.
pause
:end
