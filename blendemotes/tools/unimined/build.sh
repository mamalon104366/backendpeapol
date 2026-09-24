#!/bin/bash
# Builds the Unimined Gradle plugin from a pinned commit of its lts/1.4 branch into the local
# Maven repository (~/.m2). The last release (1.4.1) predates the unobfuscated Minecraft 26.x
# jars, Java 25 bytecode and NeoForge's new binary patcher (1.21.9+), all fixed on that branch.
#
#   bash tools/unimined/build.sh        (from the blendemotes folder; needs git and JDK 17+)
set -e
cd "$(dirname "$0")/../.."
COMMIT=dabcdf34a0c64305f98443a150528bcf6dd930de
VERSION=$(grep '^unimined_version=' gradle.properties | cut -d= -f2)
MARKER="$HOME/.m2/repository/xyz/wagyourtail/unimined/xyz.wagyourtail.unimined.gradle.plugin/$VERSION"
if [ -d "$MARKER" ]; then
    echo "Unimined $VERSION is already in the local Maven repository"
    exit 0
fi
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
git clone -q https://github.com/unimined/unimined "$WORK/unimined"
cd "$WORK/unimined"
git checkout -q "$COMMIT"
./gradlew publishToMavenLocal -Pversion="$VERSION" -x test --no-daemon --console=plain -q
echo "Unimined $VERSION published to ~/.m2"
