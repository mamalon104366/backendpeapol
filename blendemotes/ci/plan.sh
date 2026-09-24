#!/bin/bash
# Turns ci/targets.txt and ci/probe.txt into the JSON matrices of the CI jobs.
set -e
cd "$(dirname "$0")"
targets=$(grep -v -E '^\s*(#|$)' targets.txt)
versions=$(echo "$targets" | awk '{print $1}' | jq -R . | jq -s -c .)
selftest=$(echo "$targets" | awk '{for (i = 2; i <= NF; i++) print $1 " " $i}' \
  | jq -R 'split(" ") | {mc: .[0], loader: .[1]}' | jq -s -c .)
probe=$( (grep -v -E '^\s*(#|$)' probe.txt 2>/dev/null || true) | cut -d: -f1 | sort -u | jq -R . | jq -s -c . )
echo "versions=$versions"
echo "selftest=$selftest"
echo "probe=${probe:-[]}"
