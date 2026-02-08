#!/bin/bash

# Convenience script to start the frontend from root directory

cd "$(dirname "$0")/frontend" || exit 1
npm run dev

