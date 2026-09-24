#!/bin/bash
# Prints the newest loader versions for each Minecraft version (CI helper; the mavens are not
# reachable from every development machine).
meta() { curl -s --max-time 30 "$1" | grep -o '<version>[^<]*</version>' | sed 's|</*version>||g'; }
forge=$(meta https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml)
neo=$(meta https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)
fapi=$(meta https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)
floader=$(meta https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml)
lfapi=$(meta https://maven.legacyfabric.net/net/legacyfabric/legacy-fabric-api/legacy-fabric-api/maven-metadata.xml)
uni=$(meta https://maven.wagyourtail.xyz/releases/xyz/wagyourtail/unimined/xyz.wagyourtail.unimined.gradle.plugin/maven-metadata.xml)
echo "VERSIONS unimined: $(echo "$uni" | sort -V | tail -5 | tr '\n' ' ')"
echo "VERSIONS fabric-loader: $(echo "$floader" | grep -v -E 'beta|alpha|rc|pre' | sort -V | tail -3 | tr '\n' ' ')"
echo "VERSIONS legacy-fabric-api: $(echo "$lfapi" | sort -V | tail -8 | tr '\n' ' ')"
for mc in 1.8.9 1.12.2 1.16.5 1.18.2 1.19.2 1.19.4 1.20.1 1.20.4 1.20.6 1.21.1 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11 26.1 26.1.1 26.1.2 26.2 26.3; do
  f=$(echo "$forge" | grep "^$mc-" | sort -V | tail -2 | tr '\n' ' ')
  a=$(echo "$fapi" | grep "+$mc$" | sort -V | tail -1)
  short=${mc#1.}
  case "$mc" in 1.*) n=$(echo "$neo" | grep "^$short\." | sort -V | tail -2 | tr '\n' ' ');; *) n=$(echo "$neo" | grep "^$mc\." | sort -V | tail -2 | tr '\n' ' ');; esac
  echo "VERSIONS $mc | forge: $f | neoforge: $n | fabric-api: $a"
done
# Fabric API module names (some were renamed for the Mojang names of 26.x)
for v in 0.141.6+1.21.11 0.155.3+26.1.2 0.161.0+26.3; do
  mods=$(curl -s --max-time 30 "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$v/fabric-api-$v.pom" | grep -o '<artifactId>[^<]*</artifactId>' | sed 's|</*artifactId>||g' | grep -E 'key|network|lifecycle|base' | tr '\n' ' ')
  echo "VERSIONS fabric-api $v modules: $mods"
done
