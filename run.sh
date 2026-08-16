#!/usr/bin/env bash
# CineSeat 실행 스크립트
#   ./run.sh          필요한 준비를 마친 뒤 실행
#   ./run.sh --clean  build 디렉터리를 지우고 처음부터 컴파일
#   ./run.sh --reset  데이터베이스를 지우고 예시 데이터로 다시 만든 뒤 실행
set -euo pipefail

cd "$(dirname "$0")"

BUILD_DIR="build"
LIB_DIR="lib"
DB_FILE="db/cineseat.db"
MAIN_CLASS="com.cineseat.CineSeatApp"

SQLITE_VERSION="3.47.1.0"
SQLITE_JAR="$LIB_DIR/sqlite-jdbc-$SQLITE_VERSION.jar"
SQLITE_URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/$SQLITE_VERSION/sqlite-jdbc-$SQLITE_VERSION.jar"

for arg in "$@"; do
  case "$arg" in
    --clean) rm -rf "$BUILD_DIR" ;;
    --reset) rm -f "$DB_FILE" ;;
    *) echo "알 수 없는 옵션: $arg"; exit 1 ;;
  esac
done

# 1. SQLite JDBC 드라이버 (없으면 내려받는다)
mkdir -p "$LIB_DIR"
if [[ ! -f "$SQLITE_JAR" ]]; then
  echo "SQLite JDBC 드라이버를 내려받습니다..."
  curl -fSL --progress-bar "$SQLITE_URL" -o "$SQLITE_JAR.tmp"
  mv "$SQLITE_JAR.tmp" "$SQLITE_JAR"
fi

# 2. 컴파일
mkdir -p "$BUILD_DIR"
find src -name '*.java' > "$BUILD_DIR/sources.txt"
javac -encoding UTF-8 -d "$BUILD_DIR" @"$BUILD_DIR/sources.txt"

# 3. 데이터베이스 (없으면 예시 데이터로 만든다)
if [[ ! -f "$DB_FILE" ]]; then
  echo "데이터베이스를 만듭니다: $DB_FILE"
  javac -encoding UTF-8 -d "$BUILD_DIR/tools" tools/InitDb.java
  java -cp "$BUILD_DIR/tools:$SQLITE_JAR" InitDb db/schema.sql "$DB_FILE"
fi

# 4. 실행
java -cp "$BUILD_DIR:$LIB_DIR/*" "$MAIN_CLASS"
