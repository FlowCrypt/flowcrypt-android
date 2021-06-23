#!/bin/bash

# add a new user to the local email server with the predefined password = "qwerty1234"

if [ $# -eq 0 ]
  then
    userName='unknown'
  else
    userName=$1
fi

./setup.sh email add $userName qwerty1234
