#!/bin/bash
#
# Start script for ocr-api

PORT=8080

exec java -jar -Djava.security.egd=file:/dev/./urandom -Dserver.port="${PORT}" "ocr-api.jar"
