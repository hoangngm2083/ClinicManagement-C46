@echo off
echo Building API Gateway...
call mvnw.cmd clean package -DskipTests

echo API Gateway built successfully!
pause
