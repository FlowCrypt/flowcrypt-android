#!/bin/bash

# build docker image

if [ $# -eq 0 ]
  then
    userName='unknown'
  else
    userName=$1
fi

./setup.sh email add $userName qwerty1234
