#!/usr/bin/env bash

echo ":27|h"   | nc -w 1 -u localhost 8125
sleep 2
echo "junk:|h" | nc -w 1 -u localhost 8125
sleep 1
echo "bonkers" | nc -w 1 -u localhost 8125