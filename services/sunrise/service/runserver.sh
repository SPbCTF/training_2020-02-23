#!/bin/bash


socat -d TCP4-LISTEN:3771,reuseaddr,fork,keepalive exec:./run.sh
