@echo off
cd..
cd..
del jhv-coverity.zip
rd /S /Q cov-int > NUL
start /WAIT cmd /C "C:\Program Files\Eclipse\plugins\org.apache.ant_1.9.2.v201404171502\bin\ant.bat" clean
"C:\Program Files\cov-analysis-win64-7.7.0\bin\cov-build.exe" --dir cov-int --instrument "C:\Program Files\Eclipse\plugins\org.apache.ant_1.9.2.v201404171502\bin\ant.bat"
"C:\Program Files\cov-analysis-win64-7.7.0\bin\zip.exe" -r jhv-coverity.zip cov-int
rd /S /Q cov-int > NUL
pause
