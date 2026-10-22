#!/bin/bash
# Run this ONCE before first git push
echo "Downloading gradle-wrapper.jar..."
curl -sL -o gradle/wrapper/gradle-wrapper.jar \
  "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
echo "Done. Now run: git add . && git commit -m 'add wrapper' && git push"
