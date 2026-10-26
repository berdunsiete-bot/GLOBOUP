package com.example

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}

data class StormCloud(
    val id: Long,
    var x: Float, // Normalized 0f..1f across screen width
    var y: Float, // Normalized 0f..1f across screen height
    val radiusX: Float, // Normalized radius
    val radiusY: Float,
    val speed: Float,
    val lightningOffsetRatio: Float = 0f, // horizontal shift of lightning origin
    val lightningLength: Float = 0.10f, // length of lightning bolt down
    val lightningPoints: List<Pair<Float, Float>> = emptyList(), // jagged relative offsets
    var lightningAlpha: Float = 1f,
    var spawnProgress: Float = 0f, // 0.0f (birth) -> 1.0f (fully condensed)
    var cloudAlpha: Float = 0f, // Opacidad de transición suave al aparecer
    var scaleMultiplier: Float = 0.45f // Expansión natural de condensación
)

data class StarCollectible(
    val id: Long,
    var x: Float, // Normalized 0f..1f
    var y: Float, // Normalized 0f..1f
    val radius: Float = 0.032f,
    val speed: Float,
    var rotation: Float = 0f,
    var twinklePhase: Float = 0f,
    var spawnProgress: Float = 0f, // 0.0f (aparición debajo del banner) -> 1.0f (formada)
    var starAlpha: Float = 0f,     // Opacidad gradual sin pop-up sorpresivo
    var scaleMultiplier: Float = 0.40f // Crecimiento natural y suave
)

data class SparkleParticle(
    val id: Long,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Long,
    var alpha: Float = 1f,
    val size: Float,
    var life: Float = 1f
)

data class FloatingText(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    var alpha: Float = 1f
)

data class BackgroundCloud(
    val id: Long,
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
