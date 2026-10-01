@echo off
rem Builds and starts the Spring Petclinic application on http://localhost:8080/.
rem The Hypersistence Optimizer web app starts on http://localhost:8088/.
rem Press Ctrl+C to stop it gracefully, so the Optimizer flushes the pending runtime events.
rem Any arguments are passed to the application, e.g. run.bat --server.port=9090

cd /d "%~dp0"

set JVM_OPTS=
rem set JVM_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005

rem Clean first, as switching branches leaves stale classes behind. The tests are not compiled,
rem since not every demo branch compiles them.
call "%~dp0mvnw.cmd" clean package -Dmaven.test.skip=true -Dcheckstyle.skip -Dspring-javaformat.skip=true -Djacoco.skip=true
if errorlevel 1 exit /b 1

java %JVM_OPTS% -jar .\target\spring-petclinic-4.0.0-SNAPSHOT.jar %*
