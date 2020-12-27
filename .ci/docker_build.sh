#!/usr/bin/env bash

set -eux

NAME="xcontest2db"

../gradlew build

docker build -t ${NAME} .
