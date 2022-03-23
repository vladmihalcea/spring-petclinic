@echo off

FOR /F "tokens=*" %%g IN ('mvn "help:evaluate" "-Dexpression=project.version" "-q" "-DforceStdout"') DO (SET "APP_VERSION=%%g")

java -agentpath:%USER_HOME%/agent/lightrun_agent.dll -jar target/spring-petclinic-%APP_VERSION%.jar