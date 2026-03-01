@echo off
echo ========================================
echo Cellpose Backend Environment Setup
echo ========================================
echo.

REM Create and setup v3 environment (Cellpose 3.1)
echo [1/4] Creating venv_v3...
if exist venv_v3 (
    echo venv_v3 already exists, skipping creation
) else (
    python -m venv venv_v3
    echo Created venv_v3
)

echo.
echo [2/4] Installing v3 dependencies...
call venv_v3\Scripts\activate.bat
pip install --upgrade pip
pip install -r requirements_v3.txt
call venv_v3\Scripts\deactivate.bat
echo v3 environment ready!

echo.
echo [3/4] Creating venv_v4...
if exist venv_v4 (
    echo venv_v4 already exists, skipping creation
) else (
    python -m venv venv_v4
    echo Created venv_v4
)

echo.
echo [4/4] Installing v4 dependencies...
call venv_v4\Scripts\activate.bat
pip install --upgrade pip
pip install -r requirements_v4.txt
call venv_v4\Scripts\deactivate.bat
echo v4 environment ready!

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo You can now run:
echo   java -jar ..\..\target\ripple.jar
echo.
pause
