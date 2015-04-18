#!/usr/bin/env bash

echo "rendertime.landingpage:1500|g" | nc -w 1 -u localhost 8125
sleep 1
echo "rendertime.landingpage:1700|g" | nc -w 1 -u localhost 8125
sleep 1
echo "rendertime.landingpage:2000|g" | nc -w 1 -u localhost 8125