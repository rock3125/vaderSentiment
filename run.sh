#!/bin/bash

# Vader simple "run the jar" kind of affair command line wrapper
# make sure to build it first with  "mvn clean install"
# you move it around anywhere you like - the jar is self contained
# below is just the default location after a build (target)

java -jar target/vader-0.1-SNAPSHOT.jar "$@"
