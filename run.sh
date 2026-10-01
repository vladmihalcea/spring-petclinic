#!/usr/bin/env bash
# Builds and starts the Spring Petclinic application on http://localhost:8080/.
# The Hypersistence Optimizer web app starts on http://localhost:8088/.
# Press Ctrl+C to stop it gracefully, so the Optimizer flushes the pending runtime events.
# Any arguments are passed to the application, e.g. ./run.sh --server.port=9090
set -e
cd "$(dirname "$0")"

JVM_OPTS=""
# JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Clean first, as switching branches leaves stale classes behind. The tests are not compiled,
# since not every demo branch compiles them.
./mvnw clean package -Dmaven.test.skip=true -Dcheckstyle.skip -Dspring-javaformat.skip=true -Djacoco.skip=true

exec java $JVM_OPTS -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar "$@"
