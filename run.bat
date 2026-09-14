@echo off
cd /d "%~dp0"
echo Compiling ExpenseWise.java ...
javac ExpenseWise.java
if errorlevel 1 (
  echo.
  echo Compile failed. Fix the errors above and run this file again.
  pause
  exit /b 1
)
java -cp "." ExpenseWise
if errorlevel 1 pause
