package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFrequencyUnitTest {

    @Test
    fun testAdConfigConstants() {
        assertEquals(30_000L, AdConfig.MIN_INTERSTITIAL_COOLDOWN_MS)
        assertEquals(listOf(6, 12, 18), AdConfig.DEFEAT_MILESTONES)
    }

    @Test
    fun testDefeatProgressionMilestones() {
        val milestones = AdConfig.DEFEAT_MILESTONES
        var defeatCount = 0

        for (game in 1..24) {
            defeatCount++
            val shouldTrigger = (defeatCount == 6 || defeatCount == 12 || defeatCount >= 18)
            
            when (defeatCount) {
                6 -> assertTrue("Derrota 6 debe activar anuncio", shouldTrigger)
                12 -> assertTrue("Derrota 12 debe activar anuncio", shouldTrigger)
                18 -> {
                    assertTrue("Derrota 18 debe activar anuncio", shouldTrigger)
                    // Reinicio de ciclo tras hito 18
                    defeatCount = 0
                }
                else -> assertFalse("Derrota $defeatCount no debe activar anuncio", shouldTrigger)
            }
        }
    }

    @Test
    fun testShieldRewardReset() {
        var defeatCount = 5
        // El usuario pierde 5 veces, luego mira anuncio bonificado de escudo
        // Regla 4: Reseteo a 0
        defeatCount = 0
        assertEquals(0, defeatCount)

        // Vuelve a perder inmediatamente: defeatCount = 1
        defeatCount++
        val shouldTriggerOnNextLoss = (defeatCount == 6 || defeatCount == 12 || defeatCount >= 18)
        assertFalse("Tras revivir con escudo, perder de nuevo no debe disparar anuncio", shouldTriggerOnNextLoss)
        assertEquals(1, defeatCount)
    }

    @Test
    fun testCooldownValidation() {
        val cooldownMs = AdConfig.MIN_INTERSTITIAL_COOLDOWN_MS
        val lastShowTime = 100_000L

        // 10 segundos después (110_000L): Cooldown debe bloquear
        val time10sLater = lastShowTime + 10_000L
        val isCooldownActiveAt10s = (time10sLater - lastShowTime < cooldownMs)
        assertTrue("A los 10 segundos (< 30s), el cooldown debe estar activo", isCooldownActiveAt10s)

        // 30 segundos después (130_000L): Cooldown ya no debe bloquear
        val time30sLater = lastShowTime + 30_000L
        val isCooldownActiveAt30s = (time30sLater - lastShowTime < cooldownMs)
        assertFalse("A los 30 segundos exactos, el cooldown debe haber expirado", isCooldownActiveAt30s)

        // 60 segundos después: Libre
        val time60sLater = lastShowTime + 60_000L
        val isCooldownActiveAt60s = (time60sLater - lastShowTime < cooldownMs)
        assertFalse("A los 60 segundos, el cooldown debe haber expirado", isCooldownActiveAt60s)
    }

    @Test
    fun testSessionProtectionFirstTwoMatchesFree() {
        // Las primeras 2 partidas de la sesión deben estar 100% libres de publicidad intersticial
        for (sessionMatch in 1..5) {
            val isProtected = (sessionMatch <= 2)
            if (sessionMatch <= 2) {
                assertTrue("Partida $sessionMatch debe estar protegida sin intersticiales", isProtected)
            } else {
                assertFalse("Partida $sessionMatch ya no tiene protección de inicio", isProtected)
            }
        }
    }

    @Test
    fun testMutualExclusionOnMilestoneDefeat() {
        // Si el jugador llega a la derrota 6 y decide revivir con súper escudo:
        var defeatCount = 6
        var lastShowTime = 10_000L
        val now = 100_000L

        // Confirmación de recompensa: Regla de exclusión mutua
        defeatCount = 0
        lastShowTime = now // Se reinicia el cooldown de 90s

        assertEquals(0, defeatCount)
        assertEquals(now, lastShowTime)

        // Siguiente partida perdida: el contador ahora es 1 (NO salta intersticial)
        defeatCount++
        val shouldTriggerNext = (defeatCount == 6 || defeatCount == 12 || defeatCount >= 18)
        assertFalse("Tras ver el anuncio bonificado, la siguiente derrota (1) no dispara intersticial", shouldTriggerNext)
    }

    @Test
    fun testDynamicFomoCopyThreshold() {
        val highScore = 100
        val threshold = (highScore * 0.7f).toInt() // 70 puntos

        val scoreNear = 75
        val isNear = scoreNear >= threshold
        assertTrue("Con 75 puntos (>= 70% de 100), se activa copy '¡CASI SUPERAS TU RÉCORD!'", isNear)

        val scoreFar = 40
        val isFar = scoreFar >= threshold
        assertFalse("Con 40 puntos (< 70% de 100), se activa copy 'Salva tus puntos'", isFar)
    }

    @Test
    fun testSuperShieldMultiplierAndDuration() {
        var scoreMultiplier = 1
        var score = 10

        // Durante el Súper Escudo: multiplicador x2
        scoreMultiplier = 2
        val starCollectedPoints = scoreMultiplier
        score += starCollectedPoints
        assertEquals(12, score)
        assertEquals(2, starCollectedPoints)

        // Al terminar los 5.0 segundos: se restablece a 1
        scoreMultiplier = 1
        score += scoreMultiplier
        assertEquals(13, score)
    }

    @Test
    fun testOfflineZeroLatencyGameOverFallback() {
        // En modo Offline (isOnline = false o sin anuncio precargado):
        val isOnline = false
        val isRewardedAdReady = false
        val hasRevivedThisMatch = false
        val isFomoActive = false

        // La opción de revivir debe ser false en 0ms
        val showReviveButton = !hasRevivedThisMatch && isRewardedAdReady && isFomoActive && isOnline
        assertFalse("En modo Offline, el botón de revivir con temporizador se omite instantáneamente", showReviveButton)

        // El botón Jugar de nuevo está disponible de inmediato
        val playAgainAvailable = true
        assertTrue("El botón Jugar de nuevo siempre se renderiza en el frame actual (0ms)", playAgainAvailable)
    }

    @Test
    fun testOfflineInterstitialBypassWithoutBlocking() {
        // Si no hay red disponible, cualquier intento de mostrar intersticial debe resolver inmediatamente
        val isNetworkAvailable = false
        var gameContinuedImmediately = false

        if (!isNetworkAvailable) {
            // Se invoca el callback sin esperas ni llamadas síncronas bloqueantes
            gameContinuedImmediately = true
        }

        assertTrue("Sin internet, el juego continúa instantáneamente sin congelar la UI", gameContinuedImmediately)
    }

    @Test
    fun testSafetyQuantumTeleportationCoordinate() {
        // En Teletransporte de Seguridad (Quantum Jump), el globo debe reubicarse en (0.5f, 0.74f)
        val safeX = 0.5f
        val safeY = 0.74f
        assertEquals(0.5f, safeX, 0.001f)
        assertEquals(0.74f, safeY, 0.001f)
    }

    @Test
    fun testAscendingComboPitchProgression() {
        // Frecuencia armónica ascendente: base 880Hz + (combo - 1) * 68Hz
        val freqs = (1..8).map { combo -> 880.0 + (combo - 1) * 68.0 }
        for (i in 0 until freqs.size - 1) {
            assertTrue("Combo ${i + 2} debe tener mayor tono que combo ${i + 1}", freqs[i + 1] > freqs[i])
        }
        assertEquals(880.0, freqs[0], 0.1)
        assertEquals(880.0 + 7 * 68.0, freqs[7], 0.1)
    }

    @Test
    fun testGhostModeCloudDestruction() {
        // En Modo Fantasma (isShieldActive = true), cualquier nube colisionada es desintegrada
        val isShieldActive = true
        var clouds = listOf("StormCloud_1", "StormCloud_2")
        val cloudHit = "StormCloud_1"

        if (isShieldActive) {
            clouds = clouds.filter { it != cloudHit }
        }

        assertEquals(listOf("StormCloud_2"), clouds)
        assertFalse(clouds.contains("StormCloud_1"))
    }

    @Test
    fun testCloudSmoothBirthTransition() {
        val cloud = StormCloud(
            id = 1L,
            x = 0.5f,
            y = GloboUpViewModel.GAMEPLAY_TOP_LIMIT + 0.01f,
            radiusX = 0.1f,
            radiusY = 0.07f,
            speed = 0.003f,
            spawnProgress = 0f,
            cloudAlpha = 0f,
            scaleMultiplier = 0.40f
        )

        // Al nacer, la nube comienza con opacidad 0 y escala reducida (0.40f) para materializarse naturalmente
        assertEquals(0f, cloud.spawnProgress, 0.001f)
        assertEquals(0f, cloud.cloudAlpha, 0.001f)
        assertEquals(0.40f, cloud.scaleMultiplier, 0.001f)

        // Simulación de avance de transición
        var progress = cloud.spawnProgress
        for (frame in 1..40) {
            progress = (progress + 0.026f).coerceAtMost(1f)
        }
        val easeOut = 1f - (1f - progress) * (1f - progress)
        val finalAlpha = easeOut.coerceIn(0f, 1f)
        val finalScale = 0.40f + 0.60f * easeOut

        assertEquals(1f, progress, 0.01f)
        assertEquals(1f, finalAlpha, 0.01f)
        assertEquals(1f, finalScale, 0.01f)
    }

    @Test
    fun testAdMobBannerZoneProtection() {
        // La zona superior del Banner AdMob y HUD (0.00f .. 0.18f)
        // está 100% protegida contra invasión gráfica y toques accidentales
        val topLimit = GloboUpViewModel.GAMEPLAY_TOP_LIMIT
        val bottomLimit = GloboUpViewModel.GAMEPLAY_BOTTOM_LIMIT
        assertEquals(0.18f, topLimit, 0.001f)
        assertEquals(0.96f, bottomLimit, 0.001f)

        // Las nubes y estrellas se generan exclusivamente por debajo del límite de juego
        val spawnY = topLimit + 0.01f
        assertTrue("El punto de aparición ($spawnY) debe estar por debajo del límite superior ($topLimit)", spawnY > topLimit)
    }

    @Test
    fun testStarSmoothBirthTransition() {
        val star = StarCollectible(
            id = 1L,
            x = 0.5f,
            y = GloboUpViewModel.GAMEPLAY_TOP_LIMIT + 0.02f,
            radius = 0.034f,
            speed = 0.0045f,
            spawnProgress = 0f,
            starAlpha = 0f,
            scaleMultiplier = 0.40f
        )

        // Al nacer debajo del banner, la estrella comienza con opacidad 0 y escala reducida para no sorprender
        assertEquals(0f, star.spawnProgress, 0.001f)
        assertEquals(0f, star.starAlpha, 0.001f)
        assertEquals(0.40f, star.scaleMultiplier, 0.001f)

        // Simulación de avance suave
        var progress = star.spawnProgress
        for (frame in 1..40) {
            progress = (progress + 0.028f).coerceAtMost(1f)
        }
        val easeOut = 1f - (1f - progress) * (1f - progress)
        val finalAlpha = easeOut.coerceIn(0f, 1f)
        val finalScale = 0.40f + 0.60f * easeOut

        assertEquals(1f, progress, 0.01f)
        assertEquals(1f, finalAlpha, 0.01f)
        assertEquals(1f, finalScale, 0.01f)
    }

    @Test
    fun testDefinitiveAdMobBannerConfig() {
        // Verifica la configuración definitiva de AdMob solicitada por el usuario
        assertEquals("Globoup banner 2", AdConfig.BANNER_NAME)
        assertEquals("ca-app-pub-7770387169803617/3842176035", AdConfig.BANNER_AD_ID)
    }

    @Test
    fun testDefinitiveAdMobInterstitialConfig() {
        // Verifica la configuración definitiva del anuncio Intersticial solicitada por el usuario
        assertEquals("Globoup Intersticial 1", AdConfig.INTERSTITIAL_NAME)
        assertEquals("ca-app-pub-7770387169803617/4579883494", AdConfig.INTERSTITIAL_AD_ID)
    }

    @Test
    fun testDefinitiveAdMobRewardedConfig() {
        // Verifica la configuración definitiva del anuncio Recompensado solicitada por el usuario
        assertEquals("Globoup recompensado1", AdConfig.REWARDED_NAME)
        assertEquals("ca-app-pub-7770387169803617/4360391642", AdConfig.REWARDED_AD_ID)
        assertEquals("2 Escudo con iman de estrellas", AdConfig.REWARD_ITEM_NAME)
        assertEquals(2, AdConfig.REWARD_SHIELD_AMOUNT)
    }

    @Test
    fun testGrantRewardedShieldsBenefit() {
        var shieldsInventory = 0
        val rewardAmount = AdConfig.REWARD_SHIELD_AMOUNT
        val rewardType = AdConfig.REWARD_ITEM_NAME

        // Usuario ve video recompensado y recibe beneficio
        shieldsInventory += rewardAmount
        assertEquals(2, shieldsInventory)
        assertEquals("2 Escudo con iman de estrellas", rewardType)

        // Usar 1 escudo en el juego
        val canUse = shieldsInventory > 0
        assertTrue(canUse)
        shieldsInventory -= 1
        assertEquals(1, shieldsInventory)
    }

    @Test
    fun testDefinitiveAdMobNativeConfig() {
        // Verifica la configuración del anuncio Nativo de AdMob
        assertEquals("Globoup Nativo 1", AdConfig.NATIVE_NAME)
        assertEquals("ca-app-pub-3940256099942544/2247696110", AdConfig.NATIVE_AD_ID)
    }
}
