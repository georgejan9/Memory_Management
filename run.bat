@echo off
set JAVAFX_PATH=%~dp0javafx-sdk-23.0.1\lib

echo Starting Memory Allocation Simulator...
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp out MemoryAllocatorApp
