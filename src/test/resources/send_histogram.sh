#!/usr/bin/env bash

echo "rendertime.landingpage:27|h" | nc -w 1 -u localhost 8125
sleep 2
echo "rendertime.landingpage:18|h" | nc -w 1 -u localhost 8125
sleep 1
echo "rendertime.landingpage:22|h" | nc -w 1 -u localhost 8125