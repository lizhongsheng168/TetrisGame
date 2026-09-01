# 方块奇旅 TetrisGame — V1.0

轻休闲 + 强竞技 + 闯关养成俄罗斯方块手游，对标《俄罗斯方块环游记》，经典无尽 + 冒险关卡，Kotlin + Jetpack Compose 构建。

## 亮点
- 经典无尽 / 冒险闯关（15+关，手写+程序化+障碍行）
- 7-bag 随机、墙踢旋转、幽灵落点、硬降/软降
- 本地存档（DataStore）：最高分/星级/货币/关卡进度
- 消除特效 + 合成音效（AudioTrack 无资源）
- Material3 深色主题，流畅 60fps

## 技术栈
- Kotlin 1.9.24 / Compose BOM 2024.05.00 / Material3 / AGP 8.5.2 / Gradle 8.13
- compileSdk 34 / minSdk 26 / DataStore Preferences 1.1.1

## 结构
```
TetrisGame/
  app/src/main/java/com/tetris/game/
    engine/  TetrisEngine.kt / Tetromino.kt
    ui/      TetrisApp.kt / screens/ / components/BoardView.kt / theme/
    audio/   SoundManager.kt
    data/    SaveManager.kt / LevelCatalog.kt
  output/    方块奇旅.apk (构建后生成)
```

## 构建
```bash
# 沙箱友好（解决 Windows build 锁）
GRADLE_USER_HOME=C:/Temp/gradle-home \
ANDROID_USER_HOME=C:/Temp/android-home \
./gradle-dist/gradle-8.13/bin/gradle.bat assembleDebug \
  --project-cache-dir C:/Temp/gradle-cache \
  --init-script C:/Temp/tetris-init.gradle --rerun-tasks

# 或直接
./build.sh assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk → 拷贝为 方块奇旅-v1.0-debug.apk / output/方块奇旅.apk
```

## 操作
- ◀/▶ 长按连移（220ms/70ms），⟳ 旋转，⬇ 按住加速软降（120ms/36ms 1.5倍速）
- 幽灵虚影 + 顶部 TopBar（分数/Lv/行/COMBO）+ 右侧 NEXT 预览

## 版本
- V1.0 — 首发：无尽+冒险+本地存档+特效音效
