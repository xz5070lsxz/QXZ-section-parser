#!/bin/bash
# QXZ 构建脚本：编译 qxz-core 与 qz 运行时，产出到 build/ 目录
set -e
cd "$(dirname "$0")"

JAVA_HOME_DIR="$(pwd)/../tools/jdk-17.0.20.1+1"
if [ -d "$JAVA_HOME_DIR" ]; then
    JAVA="$JAVA_HOME_DIR/bin/java"
    JAVAC="$JAVA_HOME_DIR/bin/javac"
else
    JAVA="java"
    JAVAC="javac"
fi

echo "使用 JDK: $($JAVAC -version 2>&1)"

rm -rf build
mkdir -p build

# 编译全部源码
find qxz-core/src qz/src -name "*.java" > sources.txt
$JAVAC -encoding UTF-8 -d build @sources.txt
rm sources.txt

echo "构建完成: build/"
echo "运行示例: $JAVA -cp build qxz.Main run examples/hello.qxz"
