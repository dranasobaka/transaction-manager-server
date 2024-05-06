#!/bin/bash

mvn install:install-file \
  -Dfile=org/tron/trident/core/0.7.0/core-0.7.0.jar \
  -DgroupId=org.tron.trident \
  -DartifactId=core \
  -Dversion=0.7.0 \
  -Dpackaging=jar \
  -DgeneratePom=true

mvn install:install-file \
  -Dfile=org/tron/trident/utils/0.7.0/utils-0.7.0.jar \
  -DgroupId=org.tron.trident \
  -DartifactId=utils \
  -Dversion=0.7.0 \
  -Dpackaging=jar \
  -DgeneratePom=true

mvn install:install-file \
  -Dfile=org/tron/trident/abi/0.7.0/abi-0.7.0.jar \
  -DgroupId=org.tron.trident \
  -DartifactId=abi \
  -Dversion=0.7.0 \
  -Dpackaging=jar \
  -DgeneratePom=true
