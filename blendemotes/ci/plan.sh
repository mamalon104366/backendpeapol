#!/bin/bash
# Turns ci/targets.txt and ci/probe.txt into the JSON matrices of the CI jobs.
set -e
cd "$(dirname "$0")"
targets=$(grep -v -E '^\s*(#|$)' targets.txt)
versions=$(echo "$targets" | awk '{print $1}' | jq -R . | jq -s -c .)
selftest=$(echo "$targets" | awk '{for (i = 2; i <= NF; i++) print $1 " " $i}' \
  | jq -R 'split(" ") | {mc: .[0], loader: .[1]}' | jq -s -c .)
probe=$( (grep -v -E '^\s*(#|$)' probe.txt 2>/dev/null || true) | cut -d: -f1 | sort -u | jq -R . | jq -s -c . )
# Production test: the release jars on the real loaders (HeadlessMC). Legacy Fabric is not
# installable there, the other loaders are.
prod=$(echo "$targets" | awk '{for (i = 2; i <= NF; i++) print $1 " " $i}' | while read -r mc loader; do
  case "$mc $loader" in "1.8.9 fabric"|"1.12.2 fabric") continue;; esac
  f="../versions/$mc/build.gradle"
  java=$(grep -o 'javaVersion = [0-9]*' "$f" | grep -o '[0-9]*$' || true)
  api=$(grep -o 'fabricApiVersion = "[^"]*"' "$f" | cut -d'"' -f2 || true)
  jq -n -c --arg mc "$mc" --arg loader "$loader" --arg java "${java:-8}" --arg api "$api" \
    '{mc: $mc, loader: $loader, java: $java, fabricApi: $api}'
done | jq -s -c .)
echo "versions=$versions"
echo "selftest=$selftest"
echo "prod=$prod"
echo "probe=${probe:-[]}"
