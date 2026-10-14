package com.tetris.game.audio

/**
 * 动感 BGM 曲库 — 无资源文件，纯合成循环
 * 每首为 (freqHz, durationMs) 序列，Bpm 已折算进时长，loop 无缝
 * 覆盖：动感电音 / 霓虹疾走 / 糖果狂欢 / 经典重燃 / 星际巡航 / 巅峰冲刺
 */
object BgmLibrary {
    data class Track(val id: String, val name: String, val subtitle: String, val bpm: Int, val seq: List<Pair<Double, Int>>)

    // 音名常量便于编曲可读
    private const val E2 = 82.41;  private const val A2 = 110.0;  private const val C3 = 130.81; private const val E3 = 164.81; private const val F3 = 174.61
    private const val G3 = 196.0;  private const val A3 = 220.0;  private const val B3 = 246.94; private const val C4 = 261.63
    private const val D4 = 293.66; private const val E4 = 329.63; private const val F4 = 349.23; private const val G4 = 392.00
    private const val A4 = 440.0;  private const val B4 = 493.88; private const val C5 = 523.25; private const val D5 = 587.33
    private const val E5 = 659.25; private const val F5 = 698.46; private const val Fs5 = 739.99; private const val G5 = 783.99
    private const val A5 = 880.0;  private const val B5 = 987.77; private const val C6 = 1046.50; private const val D6 = 1174.66
    private const val E6 = 1318.51; private const val Gs3 = 207.65

    val tracks: List<Track> = listOf(
        Track(
            id = "energy", name = "动感节拍", subtitle = "150 BPM · 电音驱动 · 落块踩点", bpm = 150,
            seq = listOf(
                // 4小节高能循环，16分 100ms 重拍带低鼓点 E2
                E3 to 80, E5 to 100, G5 to 100, B5 to 100, G5 to 100, A5 to 100, G5 to 100, E5 to 200, 0.0 to 60,
                E3 to 80, D5 to 100, E5 to 100, G5 to 100, A5 to 100, G5 to 100, E5 to 100, D5 to 200, 0.0 to 60,
                E3 to 80, E5 to 100, G5 to 100, B5 to 180, A5 to 100, G5 to 180, E5 to 100, G5 to 100, A5 to 180, 0.0 to 80,
                A2 to 80, B5 to 100, A5 to 100, G5 to 100, E5 to 200, D5 to 100, C5 to 200, 0.0 to 120,
                E3 to 80, E5 to 100, G5 to 100, B5 to 100, C6 to 200, B5 to 100, A5 to 100, G5 to 200, 0.0 to 60,
                E3 to 80, D5 to 100, E5 to 100, G5 to 100, A5 to 200, G5 to 100, E5 to 100, D5 to 100, C5 to 200, 0.0 to 120,
            )
        ),
        Track(
            id = "neon", name = "霓虹疾走", subtitle = "132 BPM · 合成波 · 都市疾驰", bpm = 132,
            seq = listOf(
                // Am - F - G - Em 琶音 8分150ms + 底鼓
                A3 to 90, A4 to 130, C5 to 130, E5 to 130, C5 to 130, A4 to 180, 0.0 to 60,
                A3 to 90, F4 to 130, A4 to 130, C5 to 130, A4 to 130, F4 to 180, 0.0 to 60,
                A3 to 90, G4 to 130, B4 to 130, D5 to 130, B4 to 130, G4 to 180, 0.0 to 60,
                E3 to 90, E4 to 130, G4 to 130, B4 to 130, G4 to 130, E4 to 180, 0.0 to 80,
                A3 to 90, A4 to 130, C5 to 130, E5 to 200, G5 to 130, E5 to 130, C5 to 180, 0.0 to 60,
                A3 to 90, F4 to 130, A4 to 130, C5 to 200, A4 to 130, G4 to 130, E4 to 200, 0.0 to 120,
            )
        ),
        Track(
            id = "candy", name = "糖果狂欢", subtitle = "148 BPM · 弹跳流行 · 甜蜜冲刺", bpm = 148,
            seq = listOf(
                // C - G - Am - F 大调弹跳
                C4 to 80, C5 to 120, E5 to 120, G5 to 120, E5 to 120, C5 to 180, 0.0 to 60,
                G3 to 80, B4 to 120, D5 to 120, G5 to 120, D5 to 120, B4 to 180, 0.0 to 60,
                A3 to 80, C5 to 120, E5 to 120, A5 to 120, E5 to 120, C5 to 180, 0.0 to 60,
                F3 to 80, A4 to 120, C5 to 120, F5 to 120, C5 to 120, A4 to 180, 0.0 to 80,
                C4 to 80, C5 to 100, E5 to 100, G5 to 180, A5 to 100, G5 to 100, E5 to 200, 0.0 to 60,
                G3 to 80, G5 to 100, F5 to 100, E5 to 100, D5 to 200, C5 to 100, D5 to 100, E5 to 200, 0.0 to 120,
            )
        ),
        Track(
            id = "classic", name = "经典重燃", subtitle = "138 BPM · Korobeiniki 重编 · 情怀加速", bpm = 138,
            // 俄罗斯方块 A+B 主题完整重编，带底鼓点
            seq = listOf(
                E5 to 180, B4 to 90, C5 to 90, D5 to 180, C5 to 90, B4 to 90, A4 to 180, A4 to 90, C5 to 90, E5 to 180, D5 to 90, C5 to 90,
                B4 to 180, B4 to 90, C5 to 90, D5 to 180, E5 to 180, C5 to 160, A4 to 180, A4 to 90, 0.0 to 90,
                D5 to 180, F5 to 90, A5 to 180, G5 to 90, F5 to 90, E5 to 260, C5 to 90, E5 to 180, D5 to 90, C5 to 90,
                B4 to 180, B4 to 90, C5 to 90, D5 to 180, E5 to 180, C5 to 160, A4 to 180, A4 to 160, 0.0 to 120,
                E5 to 180, C5 to 160, D5 to 160, B4 to 160, C5 to 160, A4 to 160, Gs3 to 160, B4 to 160,
                E5 to 180, C5 to 160, D5 to 160, B4 to 160, C5 to 180, E5 to 180, A5 to 180, Gs3 to 180, 0.0 to 140,
            )
        ),
        Track(
            id = "chill", name = "星际巡航", subtitle = "118 BPM · 律动巡航 · 松弛带感", bpm = 118,
            seq = listOf(
                E5 to 260, G5 to 260, A5 to 420, E5 to 260, G5 to 260, B5 to 420, 0.0 to 100,
                D5 to 260, E5 to 260, G5 to 420, D5 to 260, E5 to 260, A4 to 420, 0.0 to 100,
                C5 to 260, E5 to 260, G5 to 420, C5 to 260, E5 to 260, A5 to 420, 0.0 to 80,
                B4 to 260, D5 to 260, Fs5 to 420, B4 to 260, D5 to 260, A4 to 500, 0.0 to 140,
                E5 to 260, G5 to 260, B5 to 260, C6 to 420, B5 to 260, A5 to 260, G5 to 500, 0.0 to 140,
            )
        ),
        Track(
            id = "boss", name = "巅峰冲刺", subtitle = "165 BPM · 极限竞速 · 心跳同步", bpm = 165,
            seq = listOf(
                // 16分 90ms 疯狂跑动
                E3 to 70, E5 to 90, E5 to 90, G5 to 90, E5 to 90, D5 to 90, C5 to 90, D5 to 90, E5 to 180, 0.0 to 40,
                E3 to 70, G5 to 90, A5 to 90, G5 to 90, E5 to 90, D5 to 90, C5 to 90, D5 to 90, E5 to 180, 0.0 to 40,
                E3 to 70, E5 to 90, G5 to 90, B5 to 90, C6 to 180, B5 to 90, A5 to 90, G5 to 90, E5 to 90, G5 to 90, A5 to 180, 0.0 to 60,
                A2 to 70, B5 to 90, A5 to 90, G5 to 90, E5 to 180, D5 to 90, C5 to 90, D5 to 90, E5 to 260, 0.0 to 80,
                E3 to 70, E5 to 90, G5 to 90, B5 to 180, A5 to 90, G5 to 90, E5 to 90, D5 to 90, C5 to 180, 0.0 to 60,
                E3 to 70, E5 to 80, G5 to 80, E5 to 80, G5 to 80, A5 to 80, B5 to 80, C6 to 320, 0.0 to 120,
            )
        ),
    )

    fun byId(id: String?): Track = tracks.find { it.id == id } ?: tracks.first()
    fun indexOf(id: String?): Int = tracks.indexOf(byId(id)).coerceAtLeast(0)
}
