#!/usr/bin/env bash

# location the fat jar
BIN_JAR=`ls /app/bin/*.jar | head`

exec java $JVM_ARGS -jar ${BIN_JAR} $@