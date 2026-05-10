@echo off
set JAVAFX_PATH=%~dp0javafx-sdk-23.0.1\lib

echo Compiling Memory Allocation Simulator (JavaFX)...
if exist out rmdir /s /q out
mkdir out

javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -d out src\*.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

copy src\main.fxml out\ >nul
copy src\style.css out\ >nul

echo Compilation successful!
pause
