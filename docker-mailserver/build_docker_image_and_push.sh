#!/bin/bash

# build docker image

if [ $# -eq 0 ]
  then
    tag='latest'
  else
    tag=$1
fi

docker build -t flowcrypt/flowcrypt-email-server:$tag .
docker push flowcrypt/flowcrypt-email-server:$tag