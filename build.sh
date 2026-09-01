#!/bin/bash
# 俄罗斯方块游戏构建脚本
# 用法: ./build.sh [assembleDebug|installDebug]
# 特性: 自动清理被锁定的构建产物目录，规避环境文件锁问题

set -e

cd "$(dirname "$0")"
PROJECT_DIR=$(pwd)
GRADLE="$PROJECT_DIR/../gradle-dist/gradle-8.13/bin/gradle.bat"
GRADLE_USER_HOME="C:/Users/Administrator/AppData/Local/Temp/gradle-home"
GRADLE_CACHE="C:/Users/Administrator/AppData/Local/Temp/gradle-cache"
ANDROID_HOME_DIR="C:/Users/Administrator/AppData/Local/Temp/android-home"

TASK="${1:-assembleDebug}"

echo "==> 清理旧构建产物..."
for d in app/build build; do
    if [ -d "$d" ]; then
        mv "$d" "${d}_trash_$(date +%s)" 2>/dev/null && echo "    已清理 $d"
    fi
done

export GRADLE_USER_HOME="$GRADLE_USER_HOME"
export ANDROID_USER_HOME="$ANDROID_HOME_DIR"

echo "==> 执行 gradle $TASK ..."
"$GRADLE" "$TASK" \
    --project-cache-dir "$GRADLE_CACHE" \
    --console=plain

echo "==> 构建完成"
