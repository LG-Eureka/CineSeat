#!/usr/bin/env bash
# CineSeat 실행 스크립트
#   ./run.sh          컴파일 후 실행
#   ./run.sh --clean  build 디렉터리를 지우고 처음부터 컴파일
set -euo pipefail

cd "$(dirname "$0")"

BUILD_DIR="build"
LIB_DIR="lib"
MAIN_CLASS="com.cineseat.CineSeatApp"

if [[ "${1:-}" == "--clean" ]]; then
  rm -rf "$BUILD_DIR"
fi

if [[ -z "$(find "$LIB_DIR" -name 'mysql-connector-j-*.jar' 2>/dev/null)" ]]; then
  echo "MySQL 커넥터 JAR 이 없습니다."
  echo "https://dev.mysql.com/downloads/connector/j/ 에서 내려받아 $LIB_DIR/ 에 넣어 주세요."
  exit 1
fi

if [[ ! -f "config/config.properties" ]]; then
  echo "config/config.properties 가 없습니다. config/config.properties.example 을 복사해 채워 주세요."
  exit 1
fi

mkdir -p "$BUILD_DIR"
find src -name '*.java' > "$BUILD_DIR/sources.txt"
javac -encoding UTF-8 -d "$BUILD_DIR" @"$BUILD_DIR/sources.txt"

java -cp "$BUILD_DIR:$LIB_DIR/*" "$MAIN_CLASS"
