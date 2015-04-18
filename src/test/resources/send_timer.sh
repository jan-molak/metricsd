#!/usr/bin/env bash

echo "rendertime.landingpage.time:27|ms" | nc -w 1 -u localhost 8125
sleep 2
echo "rendertime.landingpage.time:18|ms" | nc -w 1 -u localhost 8125
sleep 1
echo "rendertime.landingpage.time:22|ms" | nc -w 1 -u localhost 8125