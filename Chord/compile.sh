#!/bin/bash

javac -d ./out/production -cp ./gson-2.8.5.jar ./src/*.java

if [ "$(ls -A $OUTPUT_DIR)" ]; then
  echo "Compilation successful. Try running run.sh."
else
  echo "Compilation failed."
fi

