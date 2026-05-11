#!/usr/bin/env bash
# ─── Vireon Wild Harvest — Linux/macOS build script ──────────
# Requires: Java 21 JDK + Maven 3.9+ on PATH.
set -e

echo
echo " Building Vireon Wild Harvest..."
echo

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven not found on PATH."
  echo "        Install from https://maven.apache.org/download.cgi"
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] Java not found on PATH."
  echo "        Install JDK 21 from https://adoptium.net/"
  exit 1
fi

mvn -q clean package

echo
echo " ───────────────────────────────────────────────"
echo "  Build complete."
echo "  Output: target/VireonWildHarvest-0.1.0.jar"
echo " ───────────────────────────────────────────────"
echo
echo " Drop that file into your server's plugins/ folder and restart."
