#!/bin/bash
#
# Start script for ocr-api

PORT=8080

exec java -jar -Dserver.port="${PORT}" "ocr-api.jar"
