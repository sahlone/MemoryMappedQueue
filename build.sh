#!/bin/bash

build () {
  ./gradlew clean build -xtest
}


build
