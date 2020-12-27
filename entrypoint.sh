#!/bin/bash

echo "Running entrypoint.sh file!"

echo "ls"
ls

echo "exec java -jar ./target/scala-2.12/sp-connector-manager.jar"
java -jar ./target/scala-2.12/sp-connector-manager.jar