#!/usr/bin/env bash

echo "users.active_sessions:1|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:7|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:23|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:1112|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:300|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:-465|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:-315|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:-253|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:-215|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1
echo "users.active_sessions:-195|c|@0.1" | nc -w 1 -u localhost 8125
sleep 1