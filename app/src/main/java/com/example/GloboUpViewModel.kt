package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GloboUpViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = GamePreferences(application)
    val soundAndHaptics = SoundAndHaptics(application).apply {
        soundEnabled = preferences.soundEnabled
        hapticsEnabled = preferences.hapticsEnabled
    }

    private val _gameStatus = MutableStateFlow(GameStatus.READY)
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _highScore = MutableStateFlow(preferences.highScore)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Balloon normalized position
    private val _balloonX = MutableStateFlow(0.5f)
    val balloonX: StateFlow<Float> = _balloonX.asStateFlow()

    private val _balloonY = MutableStateFlow(0.74f)
    val balloonY: StateFlow<Float> = _balloonY.asStateFlow()

    private val _balloonTilt = MutableStateFlow(0f)
    val balloonTilt: StateFlow<Float> = _balloonTilt.asStateFlow()

    // Game world entities
    private val _clouds = MutableStateFlow<List<StormCloud>>(emptyList())
    val clouds: StateFlow<List<StormCloud>> = _clouds.asStateFlow()

    private val _stars = MutableStateFlow<List<StarCollectible>>(emptyList())
    val stars: StateFlow<List<StarCollectible>> = _stars.asStateFlow()

    private val _particles = MutableStateFlow<List<SparkleParticle>>(emptyList())
    val particles: StateFlow<List<SparkleParticle>> = _particles.asStateFlow()

    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts: StateFlow<List<FloatingText>> = _floatingTexts.asStateFlow()

    private val _bgClouds = MutableStateFlow<List<BackgroundCloud>>(emptyList())
    val bgClouds: StateFlow<List<BackgroundCloud>> = _bgClouds.asStateFlow()

    private var gameLoopJob: Job? = null
    private var nextEntityId = 1L
    private var targetBalloonX = 0.5f
    private var targetBalloonY = 0.74f
    private var spawnTimer = 0

    init {
        initBackgroundClouds()
    }

    private fun initBackgroundClouds() {
        val initialBg = mutableListOf<BackgroundCloud>()
        for (i in 0 until 5) {
            initialBg.add(
                BackgroundCloud(
                    id = nextEntityId++,
                    x = Random.nextFloat() * 0.9f + 0.05f,
                    y = Random.nextFloat() * 1.1f - 0.1f,
                    size = Random.nextFloat() * 0.18f + 0.12f,
                    speed = 0.0008f + Random.nextFloat() * 0.0006f,
                    alpha = 0.18f + Random.nextFloat() * 0.15f
                )
            )
        }
        _bgClouds.value = initialBg
    }

    fun startGame() {
        _score.value = 0
        _balloonX.value = 0.5f
        _balloonY.value = 0.74f
        targetBalloonX = 0.5f
        targetBalloonY = 0.74f
        _balloonTilt.value = 0f
        _clouds.value = emptyList()
        _stars.value = emptyList()
        _particles.value = emptyList()
        _floatingTexts.value = emptyList()
        _gameStatus.value = GameStatus.PLAYING
        spawnTimer = 0

        soundAndHaptics.playStartSound()

        // Spawn initial friendly stars to get the player started
        val initialStars = listOf(
            createStar(x = 0.35f, y = 0.45f),
            createStar(x = 0.65f, y = 0.25f),
            createStar(x = 0.20f, y = 0.10f)
        )
        _stars.value = initialStars

        startGameLoop()
    }

    fun resumeGame() {
        if (_gameStatus.value == GameStatus.PAUSED) {
            _gameStatus.value = GameStatus.PLAYING
            startGameLoop()
        }
    }

    fun pauseGame() {
        if (_gameStatus.value == GameStatus.PLAYING) {
            _gameStatus.value = GameStatus.PAUSED
            gameLoopJob?.cancel()
        }
    }

    fun onBalloonDrag(deltaX: Float, deltaY: Float, screenWidthPx: Float, screenHeightPx: Float) {
        if (_gameStatus.value != GameStatus.PLAYING) return
        val normDeltaX = deltaX / screenWidthPx
        val normDeltaY = deltaY / screenHeightPx
        targetBalloonX = (targetBalloonX + normDeltaX).coerceIn(0.10f, 0.90f)
        targetBalloonY = (targetBalloonY + normDeltaY).coerceIn(0.40f, 0.88f)
    }

    fun onBalloonTargetSet(targetNormX: Float, targetNormY: Float) {
        if (_gameStatus.value != GameStatus.PLAYING) return
        targetBalloonX = targetNormX.coerceIn(0.10f, 0.90f)
        targetBalloonY = targetNormY.coerceIn(0.40f, 0.88f)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var tick = 0L
            while (isActive && _gameStatus.value == GameStatus.PLAYING) {
                val loopStartTime = System.currentTimeMillis()
                tick++
                updateGame(tick)
                val elapsed = System.currentTimeMillis() - loopStartTime
                val delayTime = (16L - elapsed).coerceAtLeast(4L)
                delay(delayTime)
            }
        }
    }

    private fun updateGame(tick: Long) {
        val currentScore = _score.value
        val speedMultiplier = 1f + (currentScore * 0.02f).coerceAtMost(1.8f)
        val baseSpeed = 0.0042f * speedMultiplier

        // Smooth balloon movement towards target
        val currentX = _balloonX.value
        val currentY = _balloonY.value
        val newX = currentX + (targetBalloonX - currentX) * 0.28f
        val newY = currentY + (targetBalloonY - currentY) * 0.28f
        _balloonX.value = newX
        _balloonY.value = newY

        // Balloon tilt based on lateral velocity
        val targetTilt = ((targetBalloonX - currentX) * 140f).coerceIn(-22f, 22f)
        _balloonTilt.value = _balloonTilt.value + (targetTilt - _balloonTilt.value) * 0.22f

        // Update background clouds
        _bgClouds.value = _bgClouds.value.map { bg ->
            val newBgY = bg.y + bg.speed * speedMultiplier
            if (newBgY > 1.2f) {
                bg.copy(
                    y = -0.2f,
                    x = Random.nextFloat() * 0.9f + 0.05f
                )
            } else {
                bg.copy(y = newBgY)
            }
        }

        // Spawn obstacles & collectibles
        spawnTimer++
        val spawnInterval = (45 - (currentScore * 0.4f).toInt()).coerceAtLeast(24)
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0
            spawnEntities(baseSpeed)
        }

        // Update Storm Clouds
        val updatedClouds = mutableListOf<StormCloud>()
        for (cloud in _clouds.value) {
            val newCloudY = cloud.y + cloud.speed
            val newAlpha = (0.85f + 0.15f * sin((tick * 0.18f) + cloud.id)).toFloat()
            if (newCloudY < 1.3f) {
                updatedClouds.add(cloud.copy(y = newCloudY, lightningAlpha = newAlpha))
            }
        }
        _clouds.value = updatedClouds

        // Update Stars
        val updatedStars = mutableListOf<StarCollectible>()
        for (star in _stars.value) {
            val newStarY = star.y + star.speed
            val newRotation = (star.rotation + 2.5f) % 360f
            if (newStarY < 1.15f) {
                updatedStars.add(star.copy(y = newStarY, rotation = newRotation))
            }
        }
        _stars.value = updatedStars

        // Collision Checks
        checkCollisions(newX, newY)

        // Update Particles
        updateParticles()

        // Update Floating Texts
        updateFloatingTexts()
    }

    private fun spawnEntities(baseSpeed: Float) {
        val currentScore = _score.value
        val shouldSpawnCloud = Random.nextFloat() < (0.55f + (currentScore * 0.005f).coerceAtMost(0.25f))

        if (shouldSpawnCloud) {
            // Spawn storm cloud with lightning
            val cloudX = Random.nextFloat() * 0.74f + 0.13f
            val cloudRadiusX = Random.nextFloat() * 0.04f + 0.09f
            val cloudRadiusY = cloudRadiusX * 0.72f
            val cloudSpeed = baseSpeed * (0.9f + Random.nextFloat() * 0.35f)

            // Generate jagged lightning offsets
            val points = listOf(
                Pair(0f, 0f),
                Pair(-0.015f, 0.035f),
                Pair(0.012f, 0.065f),
                Pair(-0.008f, 0.10f)
            )

            val newCloud = StormCloud(
                id = nextEntityId++,
                x = cloudX,
                y = -0.16f,
                radiusX = cloudRadiusX,
                radiusY = cloudRadiusY,
                speed = cloudSpeed,
                lightningOffsetRatio = Random.nextFloat() * 0.04f - 0.02f,
                lightningLength = 0.11f,
                lightningPoints = points
            )
            _clouds.value = _clouds.value + newCloud

            // Also spawn a star safely separated from the cloud
            val starX = if (cloudX < 0.5f) {
                Random.nextFloat() * 0.35f + 0.55f
            } else {
                Random.nextFloat() * 0.35f + 0.10f
            }
            val starSpeed = baseSpeed * (1.0f + Random.nextFloat() * 0.3f)
            _stars.value = _stars.value + createStar(starX, -0.08f, starSpeed)
        } else {
            // Spawn 1 or 2 stars
            val count = if (Random.nextFloat() < 0.4f) 2 else 1
            val newStars = mutableListOf<StarCollectible>()
            for (i in 0 until count) {
                val starX = Random.nextFloat() * 0.80f + 0.10f
                val starY = -0.06f - (i * 0.08f)
                val starSpeed = baseSpeed * (1.0f + Random.nextFloat() * 0.25f)
                newStars.add(createStar(starX, starY, starSpeed))
            }
            _stars.value = _stars.value + newStars
        }
    }

    private fun createStar(x: Float, y: Float, speed: Float = 0.0045f): StarCollectible {
        return StarCollectible(
            id = nextEntityId++,
            x = x,
            y = y,
            radius = 0.034f,
            speed = speed,
            rotation = Random.nextFloat() * 360f,
            twinklePhase = Random.nextFloat() * 6.28f
        )
    }

    private fun checkCollisions(bX: Float, bY: Float) {
        val balloonHitRadius = 0.052f // Effective collision radius of balloon envelope

        // 1. Check Stars collision
        val remainingStars = mutableListOf<StarCollectible>()
        for (star in _stars.value) {
            val dist = hypot(star.x - bX, (star.y - bY) * 1.6f) // aspect ratio correction
            if (dist < (balloonHitRadius + star.radius)) {
                // Collect Star!
                onCollectStar(star)
            } else {
                remainingStars.add(star)
            }
        }
        _stars.value = remainingStars

        // 2. Check Storm Cloud and Lightning collision
        for (cloud in _clouds.value) {
            // Cloud body collision
            val distToCloud = hypot(
                (cloud.x - bX) / cloud.radiusX,
                ((cloud.y - bY) / cloud.radiusY) * 1.4f
            )

            // Lightning bolt collision: lightning hangs from (cloud.x, cloud.y + cloud.radiusY * 0.8f) down to (cloud.y + cloud.radiusY + cloud.lightningLength)
            val lightningTopY = cloud.y + cloud.radiusY * 0.5f
            val lightningBottomY = cloud.y + cloud.radiusY + cloud.lightningLength
            val isNearLightningX = kotlin.math.abs(bX - cloud.x) < (balloonHitRadius + 0.022f)
            val isInsideLightningY = bY in lightningTopY..lightningBottomY

            if (distToCloud < 1.15f || (isNearLightningX && isInsideLightningY)) {
                // Collision with storm or lightning!
                triggerGameOver(bX, bY)
                return
            }
        }
    }

    private fun onCollectStar(star: StarCollectible) {
        val newScore = _score.value + 1
        _score.value = newScore

        // Check high score
        if (newScore > _highScore.value) {
            _highScore.value = newScore
            preferences.highScore = newScore
        }

        preferences.totalStars = preferences.totalStars + 1

        soundAndHaptics.playStarSound()
        soundAndHaptics.vibrateStarCollect()

        // Spawn golden sparkle particles
        spawnStarSparkles(star.x, star.y)

        // Add floating "+1"
        _floatingTexts.value = _floatingTexts.value + FloatingText(
            id = nextEntityId++,
            text = "+1",
            x = star.x,
            y = star.y
        )
    }

    private fun triggerGameOver(bX: Float, bY: Float) {
        _gameStatus.value = GameStatus.GAME_OVER
        gameLoopJob?.cancel()

        preferences.gamesPlayed = preferences.gamesPlayed + 1

        soundAndHaptics.playThunderShock()
        soundAndHaptics.vibrateCollision()

        // Spawn shockwave explosion particles
        val explosionParticles = mutableListOf<SparkleParticle>()
        for (i in 0 until 24) {
            val angle = Random.nextFloat() * 6.283f
            val speed = Random.nextFloat() * 0.015f + 0.005f
            explosionParticles.add(
                SparkleParticle(
                    id = nextEntityId++,
                    x = bX,
                    y = bY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) 0xFFFFEB3B else 0xFFFF5722,
                    size = Random.nextFloat() * 8f + 5f,
                    life = 1f
                )
            )
        }
        _particles.value = _particles.value + explosionParticles
    }

    private fun spawnStarSparkles(x: Float, y: Float) {
        val sparkles = mutableListOf<SparkleParticle>()
        for (i in 0 until 8) {
            val angle = Random.nextFloat() * 6.283f
            val speed = Random.nextFloat() * 0.008f + 0.002f
            sparkles.add(
                SparkleParticle(
                    id = nextEntityId++,
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = 0xFFFFD700,
                    size = Random.nextFloat() * 6f + 4f,
                    life = 1f
                )
            )
        }
        _particles.value = _particles.value + sparkles
    }

    private fun updateParticles() {
        if (_particles.value.isEmpty()) return
        val remaining = mutableListOf<SparkleParticle>()
        for (p in _particles.value) {
            val newLife = p.life - 0.05f
            if (newLife > 0f) {
                remaining.add(
                    p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        life = newLife,
                        alpha = newLife
                    )
                )
            }
        }
        _particles.value = remaining
    }

    private fun updateFloatingTexts() {
        if (_floatingTexts.value.isEmpty()) return
        val remaining = mutableListOf<FloatingText>()
        for (ft in _floatingTexts.value) {
            val newAlpha = ft.alpha - 0.04f
            if (newAlpha > 0f) {
                remaining.add(
                    ft.copy(
                        y = ft.y - 0.004f,
                        alpha = newAlpha
                    )
                )
            }
        }
        _floatingTexts.value = remaining
    }

    fun toggleSound(enabled: Boolean) {
        preferences.soundEnabled = enabled
        soundAndHaptics.soundEnabled = enabled
    }

    fun toggleHaptics(enabled: Boolean) {
        preferences.hapticsEnabled = enabled
        soundAndHaptics.hapticsEnabled = enabled
    }

    fun resetStats() {
        preferences.resetStats()
        _highScore.value = 0
    }
}
