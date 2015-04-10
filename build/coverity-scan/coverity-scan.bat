@echo off
cd..
del jhv-coverity.zip
rd /S /Q cov-int > NUL
start /WAIT cmd /C ant clean
"c:\Program Files\Coverity\bin\cov-build.exe" --dir cov-int --instrument ant
zip -r jhv-coverity.zip cov-int
rd /S /Q cov-int > NUL
pause