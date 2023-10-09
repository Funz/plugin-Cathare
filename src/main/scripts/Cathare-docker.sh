#!/bin/bash

CWD=$(dirname `readlink -f $0`)
#"$(dirname "$(readlink -f "$0")")"
docker run -v ${PWD}:/workdir --mount type=bind,source=${CWD}/Cathare.sh,target=/opt/Cathare.sh,readonly irsn/cathare2:v25_3_mod931 /bin/sh /opt/Cathare.sh $1