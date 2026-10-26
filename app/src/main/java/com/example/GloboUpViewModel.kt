package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

import com.google.android.gms.ads.nativead.NativeAd

/**
 * Eventos de interfaz para disparar anuncios de Google AdMob
 */
sealed class AdUiEvent {
    data class ShowInterstitial(val reason: String) : AdUiEvent()
}

class GloboUpViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = GamePreferences(application)
    val soundAndHaptics = SoundAndHaptics(application).apply {
        soundEnabled = preferences.soundEnabled
        hapticsEnabled = preferences.hapticsEnabled
    }
    val adMobManager = AdMobManager.getInstance(application)

    // Anuncio nativo de Google AdMob
    val nativeAd: StateFlow<NativeAd?> = adMobManager.nativeAd

    companion object {
        // Límite de área jugable (Gameplay Bounds):
        // Con el banner AdMob y el HUD en la parte superior (0.00f a 0.18f),
        // las nubes y estrellas nacen de forma suave por debajo de este límite sin tapar el banner.
        // La zona inferior (hasta 0.96f) queda completamente libre y cómoda para el control del globo.
        const val GAMEPLAY_TOP_LIMIT = 0.18f
        const val GAMEPLAY_BOTTOM_LIMIT = 0.96f
        const val BALLOON_MIN_Y = 0.32f
        const val BALLOON_MAX_Y = 0.88f
    }

    // Estados y variables de control del superpoder y monetización
    private val _hasRevivedThisMatch = MutableStateFlow(false)
    val hasRevivedThisMatch: StateFlow<Boolean> = _hasRevivedThisMatch.asStateFlow()

    private val _isShieldActive = MutableStateFlow(false)
    val isShieldActive: StateFlow<Boolean> = _isShieldActive.asStateFlow()

    private val _shieldTimer = MutableStateFlow(0.0f)
    val shieldTimer: StateFlow<Float> = _shieldTimer.asStateFlow()

    private val _isShieldFlickering = MutableStateFlow(false)
    val isShieldFlickering: StateFlow<Boolean> = _isShieldFlickering.asStateFlow()

    private val _isGamePausedForCountdown = MutableStateFlow(false)
    val isGamePausedForCountdown: StateFlow<Boolean> = _isGamePausedForCountdown.asStateFlow()

    private val _countdownTimer = MutableStateFlow(3)
    val countdownTimer: StateFlow<Int> = _countdownTimer.asStateFlow()

    private val _fomoTimer = MutableStateFlow(5.0f)
    val fomoTimer: StateFlow<Float> = _fomoTimer.asStateFlow()

    private val _isFomoActive = MutableStateFlow(false)
    val isFomoActive: StateFlow<Boolean> = _isFomoActive.asStateFlow()

    private val _scoreMultiplier = MutableStateFlow(1)
    val scoreMultiplier: StateFlow<Int> = _scoreMultiplier.asStateFlow()

    // Compatibilidad
    val hasShield: StateFlow<Boolean> = _isShieldActive.asStateFlow()

    private var fomoJob: Job? = null
    private var countdownJob: Job? = null

    private val _gameStatus = MutableStateFlow(GameStatus.READY)
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _highScore = MutableStateFlow(preferences.highScore)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    // Nivel del juego (progresión cada 15 estrellas)
    private val _currentLevel = MutableStateFlow(1)
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

    // Flujo de eventos para disparar anuncios intersticiales de forma no bloqueante
    private val _adUiEvents = MutableSharedFlow<AdUiEvent>(extraBufferCapacity = 4)
    val adUiEvents: SharedFlow<AdUiEvent> = _adUiEvents.asSharedFlow()

    // Inventario de Escudos con imán de estrellas (Recompensa de AdMob)
    private val _shieldsInventory = MutableStateFlow(preferences.shieldsCount)
    val shieldsInventory: StateFlow<Int> = _shieldsInventory.asStateFlow()

    // Internacionalización (i18n): Detección automática y gestión de idioma activo
    private val _appLanguage = MutableStateFlow(I18nManager.resolveLanguage(preferences.languageSetting))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _gameStrings = MutableStateFlow(I18nManager.getStringsForLanguage(_appLanguage.value))
    val gameStrings: StateFlow<GameStrings> = _gameStrings.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        preferences.languageSetting = language.code
        _appLanguage.value = language
        _gameStrings.value = I18nManager.getStringsForLanguage(language)
    }

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
    private var starComboStreak = 0
    private var lastStarCollectTime = 0L

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
        fomoJob?.cancel()
        countdownJob?.cancel()
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
        _currentLevel.value = 1
        
        // Reset de estados de poder y revivir por partida
        _hasRevivedThisMatch.value = false
        _isShieldActive.value = false
        _shieldTimer.value = 0f
        _isShieldFlickering.value = false
        _isGamePausedForCountdown.value = false
        _countdownTimer.value = 3
        _fomoTimer.value = 5.0f
        _isFomoActive.value = false
        _scoreMultiplier.value = 1
        spawnTimer = 0

        // Registra la partida para la protección de sesión (primeras 2 sin intersticiales)
        adMobManager.registerSessionMatch()

        soundAndHaptics.playStartSound()

        // Spawn initial friendly stars strictly below the gameplay top bound
        val initialStars = listOf(
            createStar(x = 0.35f, y = 0.46f),
            createStar(x = 0.65f, y = 0.38f),
            createStar(x = 0.20f, y = 0.32f)
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
        targetBalloonY = (targetBalloonY + normDeltaY).coerceIn(BALLOON_MIN_Y, BALLOON_MAX_Y)
    }

    fun onBalloonTargetSet(targetNormX: Float, targetNormY: Float) {
        if (_gameStatus.value != GameStatus.PLAYING) return
        // Ignora toques en la zona superior (HUD) o inferior (Banner AdMob)
        if (targetNormY < GAMEPLAY_TOP_LIMIT || targetNormY > GAMEPLAY_BOTTOM_LIMIT) return
        targetBalloonX = targetNormX.coerceIn(0.10f, 0.90f)
        targetBalloonY = targetNormY.coerceIn(BALLOON_MIN_Y, BALLOON_MAX_Y)
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

        // PAUSA ANTI-ESTRÉS: Si estamos en el conteo regresivo (3, 2, 1),
        // congelamos la física del escenario para que el jugador coloque su dedo sin peligro
        if (_isGamePausedForCountdown.value) {
            updateParticles()
            updateFloatingTexts()
            return
        }

        // CONTROL DEL SÚPER ESCUDO DE 5 SEGUNDOS
        if (_isShieldActive.value) {
            val updatedTimer = (_shieldTimer.value - 0.016f).coerceAtLeast(0f)
            _shieldTimer.value = updatedTimer
            // Aviso de caducidad en el segundo 4.0 (último segundo parpadeante)
            _isShieldFlickering.value = (updatedTimer <= 1.0f)
            if (updatedTimer <= 0f) {
                _isShieldActive.value = false
                _isShieldFlickering.value = false
                _scoreMultiplier.value = 1
            }
        }

        val currentScore = _score.value
        val speedMultiplier = 1f + (currentScore * 0.012f).coerceAtMost(1.45f)
        val baseSpeed = 0.0028f * speedMultiplier

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

        // Spawn obstacles & collectibles con cadencia natural y espaciada
        spawnTimer++
        val spawnInterval = (70 - (currentScore * 0.4f).toInt()).coerceAtLeast(38)
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0
            spawnEntities(baseSpeed)
        }

        // Update Storm Clouds con transición natural de condensación y escala
        val updatedClouds = mutableListOf<StormCloud>()
        for (cloud in _clouds.value) {
            val newCloudY = cloud.y + cloud.speed
            // Progresión suave de materialización (toma ~38 frames en condensarse suavemente)
            val newProgress = (cloud.spawnProgress + 0.026f).coerceAtMost(1f)
            val easeOut = 1f - (1f - newProgress) * (1f - newProgress)
            val newCloudAlpha = easeOut.coerceIn(0f, 1f)
            val newScale = 0.40f + 0.60f * easeOut

            // Rayo eléctrico: chispas sutiles durante la formación, voltaje completo al estar formada
            val newLightningAlpha = if (newProgress < 0.60f) {
                (newProgress * 0.30f * sin(tick * 0.45f).coerceAtLeast(0f))
            } else {
                (0.85f + 0.15f * sin((tick * 0.18f) + cloud.id)).toFloat()
            }

            if (newCloudY < 1.3f) {
                updatedClouds.add(
                    cloud.copy(
                        y = newCloudY,
                        spawnProgress = newProgress,
                        cloudAlpha = newCloudAlpha,
                        scaleMultiplier = newScale,
                        lightningAlpha = newLightningAlpha
                    )
                )
            }
        }
        _clouds.value = updatedClouds

        // Update Stars & IMÁN DE ESTRELLAS durante el Súper Escudo
        val updatedStars = mutableListOf<StarCollectible>()
        val isMagnetActive = _isShieldActive.value
        for (star in _stars.value) {
            var starX = star.x
            var starY = star.y + star.speed
            if (isMagnetActive) {
                // Atracción magnética progresiva hacia el centro del globo
                val dx = newX - starX
                val dy = newY - starY
                val dist = hypot(dx, dy)
                val pullSpeed = 0.022f + (0.06f * (1f - dist).coerceAtLeast(0f))
                starX += dx * pullSpeed
                starY += dy * pullSpeed
            }
            val newRotation = (star.rotation + 2.5f) % 360f

            // Progresión suave de materialización de estrella debajo del banner (sin aparecer de golpe)
            val newProgress = (star.spawnProgress + 0.028f).coerceAtMost(1f)
            val easeOut = 1f - (1f - newProgress) * (1f - newProgress)
            val newStarAlpha = easeOut.coerceIn(0f, 1f)
            val newScale = 0.40f + 0.60f * easeOut

            if (starY < 1.15f) {
                updatedStars.add(
                    star.copy(
                        x = starX,
                        y = starY,
                        rotation = newRotation,
                        spawnProgress = newProgress,
                        starAlpha = newStarAlpha,
                        scaleMultiplier = newScale
                    )
                )
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
            val cloudSpeed = baseSpeed * (0.85f + Random.nextFloat() * 0.25f)

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
                y = GAMEPLAY_TOP_LIMIT + 0.01f,
                radiusX = cloudRadiusX,
                radiusY = cloudRadiusY,
                speed = cloudSpeed,
                lightningOffsetRatio = Random.nextFloat() * 0.04f - 0.02f,
                lightningLength = 0.11f,
                lightningPoints = points,
                spawnProgress = 0f,
                cloudAlpha = 0f,
                scaleMultiplier = 0.40f,
                lightningAlpha = 0f
            )
            _clouds.value = _clouds.value + newCloud

            // Also spawn a star safely separated from the cloud debajo del banner
            val starX = if (cloudX < 0.5f) {
                Random.nextFloat() * 0.35f + 0.55f
            } else {
                Random.nextFloat() * 0.35f + 0.10f
            }
            val starSpeed = baseSpeed * (1.0f + Random.nextFloat() * 0.3f)
            _stars.value = _stars.value + createStar(starX, GAMEPLAY_TOP_LIMIT + 0.02f, starSpeed)
        } else {
            // Spawn 1 or 2 stars debajo del banner superior
            val count = if (Random.nextFloat() < 0.4f) 2 else 1
            val newStars = mutableListOf<StarCollectible>()
            for (i in 0 until count) {
                val starX = Random.nextFloat() * 0.80f + 0.10f
                val starY = GAMEPLAY_TOP_LIMIT + 0.02f + (i * 0.05f)
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
            twinklePhase = Random.nextFloat() * 6.28f,
            spawnProgress = 0f,
            starAlpha = 0f,
            scaleMultiplier = 0.40f
        )
    }

    private fun checkCollisions(bX: Float, bY: Float) {
        val balloonHitRadius = 0.052f // Effective collision radius of balloon envelope

        // 1. Check Stars collision
        val remainingStars = mutableListOf<StarCollectible>()
        for (star in _stars.value) {
            val dist = hypot(star.x - bX, (star.y - bY) * 1.6f)
            if (dist < (balloonHitRadius + star.radius)) {
                onCollectStar(star)
            } else {
                remainingStars.add(star)
            }
        }
        _stars.value = remainingStars

        // 2. Check Storm Cloud and Lightning collision
        val survivingClouds = mutableListOf<StormCloud>()
        for (cloud in _clouds.value) {
            // Protección de transición: durante la condensación/nacimiento (alpha < 0.50f) no colisiona letalmente
            if (cloud.cloudAlpha < 0.50f) {
                survivingClouds.add(cloud)
                continue
            }

            val effRadiusX = cloud.radiusX * cloud.scaleMultiplier
            val effRadiusY = cloud.radiusY * cloud.scaleMultiplier

            // Cloud body collision
            val distToCloud = hypot(
                (cloud.x - bX) / effRadiusX,
                ((cloud.y - bY) / effRadiusY) * 1.4f
            )

            // Lightning bolt collision
            val lightningTopY = cloud.y + effRadiusY * 0.5f
            val lightningBottomY = cloud.y + effRadiusY + (cloud.lightningLength * cloud.scaleMultiplier)
            val isNearLightningX = kotlin.math.abs(bX - cloud.x) < (balloonHitRadius + 0.022f * cloud.scaleMultiplier)
            val isInsideLightningY = bY in lightningTopY..lightningBottomY

            val isColliding = distToCloud < 1.15f || (isNearLightningX && isInsideLightningY && cloud.lightningAlpha > 0.4f)

            if (isColliding) {
                if (_isShieldActive.value) {
                    // DESTRUCCIÓN CONTINUA: Cualquier nube que toque el escudo azul es destruida automáticamente
                    destroyCloudWithShield(cloud)
                    continue
                } else {
                    // Colisión dañina: Game Over
                    survivingClouds.add(cloud)
                    _clouds.value = survivingClouds
                    triggerGameOver(bX, bY)
                    return
                }
            } else {
                survivingClouds.add(cloud)
            }
        }
        _clouds.value = survivingClouds
    }

    private fun destroyCloudWithShield(cloud: StormCloud) {
        soundAndHaptics.playThunderShock()
        soundAndHaptics.vibrateCollision()
        // Partículas de desintegración cian y dorada
        val burst = mutableListOf<SparkleParticle>()
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 6.283f
            val speed = Random.nextFloat() * 0.016f + 0.006f
            burst.add(
                SparkleParticle(
                    id = nextEntityId++,
                    x = cloud.x,
                    y = cloud.y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) 0xFF00FFFF else 0xFFFFD700,
                    size = Random.nextFloat() * 8f + 5f,
                    life = 0.9f
                )
            )
        }
        _particles.value = _particles.value + burst
        _floatingTexts.value = _floatingTexts.value + FloatingText(
            id = nextEntityId++,
            text = "⚡ SHIELD",
            x = cloud.x,
            y = cloud.y
        )
    }

    private fun onCollectStar(star: StarCollectible) {
        val pointsToAdd = _scoreMultiplier.value
        val newScore = _score.value + pointsToAdd
        _score.value = newScore

        // Check high score
        if (newScore > _highScore.value) {
            _highScore.value = newScore
            preferences.highScore = newScore
        }

        preferences.totalStars = preferences.totalStars + pointsToAdd

        // Respuesta sensorial: Tono ascendente dinámico por estrellas consecutivas (Combo)
        val now = System.currentTimeMillis()
        if (now - lastStarCollectTime < 1400L) {
            starComboStreak = (starComboStreak + 1).coerceAtMost(8)
        } else {
            starComboStreak = 1
        }
        lastStarCollectTime = now

        soundAndHaptics.playStarSound(starComboStreak)
        soundAndHaptics.vibrateStarCollect()

        // Spawn golden sparkle particles
        spawnStarSparkles(star.x, star.y)

        // Floating text: "+2 ⭐" si el multiplicador x2 está activo, "+1" normal
        val textLabel = if (pointsToAdd > 1) "+$pointsToAdd ⭐" else "+1"
        _floatingTexts.value = _floatingTexts.value + FloatingText(
            id = nextEntityId++,
            text = textLabel,
            x = star.x,
            y = star.y
        )

        // Comprobación y avance de nivel: cada 15 puntos se sube de nivel
        val calculatedLevel = (newScore / 15) + 1
        if (calculatedLevel > _currentLevel.value) {
            _currentLevel.value = calculatedLevel
            soundAndHaptics.playQuantumJumpSound()
            soundAndHaptics.vibrateFirmBurst()

            // Notificación visual de nuevo nivel
            _floatingTexts.value = _floatingTexts.value + FloatingText(
                id = nextEntityId++,
                text = "🌟 ¡NIVEL $calculatedLevel!",
                x = star.x,
                y = (star.y - 0.05f).coerceAtLeast(GAMEPLAY_TOP_LIMIT + 0.03f)
            )

            // Disparar automáticamente anuncio intersticial al cambiar de nivel
            _adUiEvents.tryEmit(AdUiEvent.ShowInterstitial("level_change_$calculatedLevel"))
        }
    }

    private fun triggerGameOver(bX: Float) {
        triggerGameOver(bX, _balloonY.value)
    }

    private fun triggerGameOver(bX: Float, bY: Float) {
        _gameStatus.value = GameStatus.GAME_OVER
        gameLoopJob?.cancel()

        preferences.gamesPlayed = preferences.gamesPlayed + 1
        adMobManager.recordDefeat()

        // Disparar automáticamente anuncio intersticial en pantalla de Game Over
        _adUiEvents.tryEmit(AdUiEvent.ShowInterstitial("game_over"))

        soundAndHaptics.playThunderShock()
        soundAndHaptics.vibrateCollision()

        // Temporizador de urgencia FOMO de 5 segundos en Game Over
        fomoJob?.cancel()
        if (!_hasRevivedThisMatch.value && adMobManager.isRewardedLoaded()) {
            _fomoTimer.value = 5.0f
            _isFomoActive.value = true
            fomoJob = viewModelScope.launch {
                while (isActive && _fomoTimer.value > 0f) {
                    delay(50)
                    val remaining = (_fomoTimer.value - 0.05f).coerceAtLeast(0f)
                    _fomoTimer.value = remaining
                    if (remaining <= 0f) {
                        _isFomoActive.value = false
                        break
                    }
                }
            }
        } else {
            _isFomoActive.value = false
            _fomoTimer.value = 0f
        }

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
                    color = if (_scoreMultiplier.value > 1) 0xFF00FFFF else 0xFFFFD700,
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
        adMobManager.resetDefeatCycle()
    }

    /**
     * FLUJO COMPLETO POST-ANUNCIO RECOMPENSADO (onUserEarnedReward) - MODO DIOS / RESCATE:
     * 1. hasRevivedThisMatch = true (máximo 1 vez por partida).
     * 2. TELETRANSPORTE DE SEGURIDAD (Quantum Jump): Reubica instantáneamente el globo en la zona inferior central (0.5f, 0.74f).
     * 3. ONDA EXPANSIVA: Destruye instantáneamente todas las nubes de tormenta visibles en pantalla.
     * 4. PAUSA ANTI-ESTRÉS: 3 segundos con física congelada (3, 2, 1) para acomodar los dedos.
     * 5. ACTIVACIÓN DE MODO FANTASMA (5.0s): Escudo Cian Neón (#00FFFF), Intangibilidad, Destrucción, Imán y Puntos Dobles (+2).
     */
    fun onReviveWithSuperShield() {
        fomoJob?.cancel()
        _isFomoActive.value = false
        _hasRevivedThisMatch.value = true

        // 1. TELETRANSPORTE DE SEGURIDAD (Quantum Jump)
        _balloonX.value = 0.5f
        _balloonY.value = 0.74f
        targetBalloonX = 0.5f
        targetBalloonY = 0.74f
        _balloonTilt.value = 0f

        // Partículas del portal de teletransporte cuántico
        val portalParticles = mutableListOf<SparkleParticle>()
        for (i in 0 until 24) {
            val angle = Random.nextFloat() * 6.283f
            val speed = Random.nextFloat() * 0.018f + 0.006f
            portalParticles.add(
                SparkleParticle(
                    id = nextEntityId++,
                    x = 0.5f,
                    y = 0.74f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) 0xFF00FFFF else 0xFF80D8FF,
                    size = Random.nextFloat() * 8f + 4f,
                    life = 1f
                )
            )
        }
        _particles.value = _particles.value + portalParticles
        soundAndHaptics.playQuantumJumpSound()
        soundAndHaptics.vibrateFirmBurst()

        // 2. ONDA EXPANSIVA: Destrucción instantánea de todas las nubes en pantalla
        _clouds.value = emptyList()
        val shockwaveParticles = mutableListOf<SparkleParticle>()
        for (i in 0 until 40) {
            val angle = Random.nextFloat() * 6.283f
            val speed = Random.nextFloat() * 0.024f + 0.008f
            shockwaveParticles.add(
                SparkleParticle(
                    id = nextEntityId++,
                    x = 0.5f,
                    y = 0.74f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) 0xFF00FFFF else 0xFFFFD700,
                    size = Random.nextFloat() * 10f + 6f,
                    life = 1f
                )
            )
        }
        _particles.value = _particles.value + shockwaveParticles
        soundAndHaptics.playThunderShock()

        // 3. PAUSA ANTI-ESTRÉS CON CONTEO (3, 2, 1)
        _isGamePausedForCountdown.value = true
        _countdownTimer.value = 3
        _gameStatus.value = GameStatus.PLAYING
        startGameLoop()

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _countdownTimer.value = 3
            soundAndHaptics.playStartSound()
            delay(1000)
            _countdownTimer.value = 2
            soundAndHaptics.playStartSound()
            delay(1000)
            _countdownTimer.value = 1
            soundAndHaptics.playStartSound()
            delay(1000)
            _countdownTimer.value = 0
            _isGamePausedForCountdown.value = false

            // 4. ACTIVAR MODO FANTASMA / SUPERPODER POR 5.0 SEGUNDOS
            activateSuperShield()
        }
    }

    private fun activateSuperShield() {
        _isShieldActive.value = true
        _shieldTimer.value = 5.0f
        _isShieldFlickering.value = false
        _scoreMultiplier.value = 2
        soundAndHaptics.playStartSound()
        soundAndHaptics.vibrateStarCollect()
        _floatingTexts.value = _floatingTexts.value + FloatingText(
            id = nextEntityId++,
            text = _gameStrings.value.superShieldActivated,
            x = _balloonX.value,
            y = _balloonY.value - 0.06f
        )
    }

    fun reviveWithShield() {
        onReviveWithSuperShield()
    }

    /**
     * Pausa física y lógica del juego cuando se muestra un anuncio intersticial en mitad de partida (e.g. subida de nivel).
     */
    fun pauseForInterstitialAd() {
        _isGamePausedForCountdown.value = true
    }

    /**
     * Reanuda la partida con conteo anti-estrés (3, 2, 1) tras cerrar un anuncio intersticial.
     */
    fun resumeAfterInterstitialAd() {
        if (_gameStatus.value == GameStatus.PLAYING) {
            startPostAdCountdown()
        }
    }

    fun startPostAdCountdown() {
        _isGamePausedForCountdown.value = true
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _countdownTimer.value = 3
            soundAndHaptics.playStartSound()
            delay(1000)
            _countdownTimer.value = 2
            soundAndHaptics.playStartSound()
            delay(1000)
            _countdownTimer.value = 1
            soundAndHaptics.playStartSound()
            delay(1000)
            _countdownTimer.value = 0
            _isGamePausedForCountdown.value = false
        }
    }

    /**
     * Otorga la recompensa definitiva tras ver el video recompensado:
     * "+2 Escudo con imán de estrellas".
     */
    fun grantRewardedShields(
        amount: Int = AdConfig.REWARD_SHIELD_AMOUNT,
        type: String = AdConfig.REWARD_ITEM_NAME
    ) {
        val updated = _shieldsInventory.value + amount
        _shieldsInventory.value = updated
        preferences.shieldsCount = updated

        soundAndHaptics.playQuantumJumpSound()
        soundAndHaptics.vibrateFirmBurst()

        // Notificación flotante de recompensa
        _floatingTexts.value = _floatingTexts.value + FloatingText(
            id = nextEntityId++,
            text = "🛡️ +$amount ESCUDOS IMÁN",
            x = _balloonX.value,
            y = (_balloonY.value - 0.08f).coerceAtLeast(GAMEPLAY_TOP_LIMIT + 0.05f)
        )
    }

    /**
     * Activa 1 escudo con imán de estrellas del inventario en plena partida.
     */
    fun useInventoryShield(): Boolean {
        if (_shieldsInventory.value <= 0 || _isShieldActive.value) return false
        val updated = _shieldsInventory.value - 1
        _shieldsInventory.value = updated
        preferences.shieldsCount = updated
        activateSuperShield()
        return true
    }
}
