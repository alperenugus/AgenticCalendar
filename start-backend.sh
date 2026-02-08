#!/bin/bash

# Convenience script to start the backend from root directory

cd "$(dirname "$0")/backend" || exit 1
mvn spring-boot:run

