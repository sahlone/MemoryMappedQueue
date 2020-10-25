#!/bin/bash

source build.sh
run () {
  build
  java -jar -Dlogback.configurationFile=logback.xml build/libs/MemoryMappedQueue-1.0.1-all.jar @
}
run
