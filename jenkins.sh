#!/usr/bin/env bash

set -eux

NAME="xcontest2db"

mvn clean package

docker build -t ${NAME} .
