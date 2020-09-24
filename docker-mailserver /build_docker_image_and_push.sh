#!/bin/bash

# build docker image

docker build -t flowcrypt/flowcrypt-email-server .
docker push flowcrypt/flowcrypt-email-server