@echo off
title Dental Calendar Web Portal
color 0B
echo ========================================================
echo        Dental Calendar - نظام مواعيد عيادة الاسنان
echo             تشغيل برنامج الويب للكمبيوتر واللابتوب
echo ========================================================
echo.
cd /d "%~dp0web"
echo جاري تشغيل خادم الويب وفتح المتصفح...
timeout /t 2 /nobreak >nul
start http://localhost:5173
npm.cmd run dev
pause
