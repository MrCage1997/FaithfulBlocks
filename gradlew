#!/bin/sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER="$DIR/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v9.7.1/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d"

if ! command -v java >/dev/null 2>&1; then
  echo "Java wurde nicht gefunden. Bitte JDK 25 installieren." >&2
  exit 1
fi

if [ ! -f "$WRAPPER" ]; then
  echo "Lade offiziellen Gradle Wrapper herunter..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$WRAPPER"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$WRAPPER" "$URL"
  else
    echo "curl oder wget wird zum einmaligen Herunterladen des Gradle Wrappers benoetigt." >&2
    exit 1
  fi
fi

if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL=$(sha256sum "$WRAPPER" | awk '{print $1}')
elif command -v shasum >/dev/null 2>&1; then
  ACTUAL=$(shasum -a 256 "$WRAPPER" | awk '{print $1}')
else
  ACTUAL="$EXPECTED"
fi

if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "Fehler: Gradle Wrapper Pruefsumme stimmt nicht." >&2
  rm -f "$WRAPPER"
  exit 1
fi

exec java -classpath "$WRAPPER" org.gradle.wrapper.GradleWrapperMain "$@"
