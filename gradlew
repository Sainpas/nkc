#!/bin/sh
# The wrapper JAR is intentionally omitted from this source repository.
# See gradle/wrapper/insert_gradle-wrapper_jar_here.txt for setup instructions.
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
