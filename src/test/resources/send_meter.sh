#!/usr/bin/env bash

echo "users.active_sessions:7|m" | nc -w 1 -u localhost 8125
sleep 2
echo "users.active_sessions:9|m" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:2|m" | nc -w 1 -u localhost 8125