@echo off

call mvn spring-javaformat:apply
call mvn clean install -DskipTests=true