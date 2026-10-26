package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.LinearProgressIndicator
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GlobUpScreen(modifier: Modifier = Modifier) {
    GloboUpScreen(modifier = modifier)
}

@Composable
fun GloboUpScreen(
    viewModel: GloboUpViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val gameStatus by viewModel.gameStatus.collectAsState()
    val score by viewModel.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val balloonX by viewModel.balloonX.collectAsState()
    val balloonY by viewModel.balloonY.collectAsState()
    val balloonTilt by viewModel.balloonTilt.collectAsState()
    val clouds by viewModel.clouds.collectAsState()
    val stars by viewModel.stars.collectAsState()
    val bgClouds by viewModel.bgClouds.collectAsState()
    val particles by viewModel.particles.collectAsState()
    val floatingTexts by viewModel.floatingTexts.collectAsState()
    val hasRevivedThisMatch by viewModel.hasRevivedThisMatch.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val isShieldFlickering by viewModel.isShieldFlickering.collectAsState()
    val isGamePausedForCountdown by viewModel.isGamePausedForCountdown.collectAsState()
    val countdownTimer by viewModel.countdownTimer.collectAsState()
    val fomoTimer by viewModel.fomoTimer.collectAsState()
    val isFomoActive by viewModel.isFomoActive.collectAsState()
    val scoreMultiplier by viewModel.scoreMultiplier.collectAsState()

    val isRewardedAdReady by viewModel.adMobManager.isRewardedAdReady.collectAsState()
    val isOnline by viewModel.adMobManager.isOnline.collectAsState()
    val gameStrings by viewModel.gameStrings.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val defeatCount by viewModel.adMobManager.defeatCount.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val shieldsInventory by viewModel.shieldsInventory.collectAsState()
    val nativeAd by viewModel.nativeAd.collectAsState()

    // Gestión automática y sin bloqueo de anuncios intersticiales (Game Over y cambio de nivel)
    LaunchedEffect(Unit) {
        viewModel.adUiEvents.collect { event ->
            when (event) {
                is AdUiEvent.ShowInterstitial -> {
                    val wasPlaying = viewModel.gameStatus.value == GameStatus.PLAYING
                    if (wasPlaying) {
                        viewModel.pauseForInterstitialAd()
                    }
                    viewModel.adMobManager.showInterstitialAd(
                        activity = activity,
                        reason = event.reason
                    ) {
                        if (wasPlaying && viewModel.gameStatus.value == GameStatus.PLAYING) {
                            viewModel.resumeAfterInterstitialAd()
                        }
                    }
                }
            }
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHowToPlayDialog by remember { mutableStateOf(false) }

    // Pulsing animation for start prompt
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val promptScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "promptScale"
    )

    // Soporte nativo de dirección de lectura RTL (Right to Left) para Árabe
    val layoutDirection = if (gameStrings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = modifier.fillMaxSize()) {
            // 1. Interactive Arcade Canvas
            GloboUpCanvas(
                balloonX = balloonX,
                balloonY = balloonY,
                balloonTilt = balloonTilt,
                clouds = clouds,
                stars = stars,
                bgClouds = bgClouds,
                particles = particles,
                floatingTexts = floatingTexts,
                isShieldActive = isShieldActive,
                isShieldFlickering = isShieldFlickering,
                onDrag = { deltaX, deltaY, w, h ->
                    viewModel.onBalloonDrag(deltaX, deltaY, w, h)
                },
                onTap = { xNorm, yNorm ->
                    if (gameStatus == GameStatus.READY) {
                        viewModel.startGame()
                    } else if (gameStatus == GameStatus.PLAYING) {
                        viewModel.onBalloonTargetSet(xNorm, yNorm)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Top Area: Banner AdMob Superior + Top Bar HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 4.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Banner AdMob Definitivo ubicado en la parte superior para no molestar al usuario
                DynamicIslandBanner(
                    modifier = Modifier.fillMaxWidth(),
                    adUnitId = AdConfig.BANNER_AD_ID,
                    advertisementLabel = gameStrings.advertisement
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Top Bar HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left: Capsule Badge with Balloon icon + Titulo del juego
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x381565C0))
                            .border(1.5.dp, Color(0x55FFFFFF), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎈",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = gameStrings.gameTitle,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Top-Right: Level + Points Capsule Cards
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level Capsule Card
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x381565C0))
                                .border(1.5.dp, Color(0x66FFD700), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "NIVEL",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$currentLevel",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Points Capsule Card (Puntos / Score)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x381565C0))
                                .border(1.5.dp, Color(0x55FFFFFF), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = gameStrings.points,
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "$score",
                                    color = Color(0xFFE8F5E9),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Zona de separación transparente de seguridad (previene clics accidentales en el banner)
                Spacer(modifier = Modifier.height(14.dp))
            }

        // 3. Bottom-Right Settings Button (Matching Screenshot dark circular button with gear icon)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF263238).copy(alpha = 0.85f))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    .shadow(6.dp, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.pauseGame()
                        showSettingsDialog = true
                    }
                    .testTag("settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes de Juego",
                    tint = Color(0xFFCFD8DC),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 3.1 Botón para activar Escudo con imán de estrellas durante la partida (si tiene en inventario)
        if (gameStatus == GameStatus.PLAYING && shieldsInventory > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isShieldActive) Color(0xAA00E5FF) else Color(0xDD0A2540))
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(26.dp))
                        .shadow(6.dp, RoundedCornerShape(26.dp))
                        .clickable(enabled = !isShieldActive) {
                            viewModel.useInventoryShield()
                        }
                        .padding(horizontal = 16.dp)
                        .testTag("use_inventory_shield_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Usar Escudo",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ESCUDO ($shieldsInventory)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 4. Overlays
        // Ready State: Tap to Play Overlay
        AnimatedVisibility(
            visible = gameStatus == GameStatus.READY,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xE6102A43)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎈 ${gameStrings.gameTitle} ⚡",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = gameStrings.tagline,
                        color = Color(0xFFB0BEC5),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (highScore > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33FFD700))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${gameStrings.record}: $highScore",
                                color = Color(0xFFFFEB3B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    Button(
                        onClick = { viewModel.startGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_game_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = gameStrings.tapToStart,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botón para recibir recompensa definitiva: 2 Escudo con imán de estrellas
                    OutlinedButton(
                        onClick = {
                            viewModel.adMobManager.showRewardedAd(
                                activity = activity,
                                onRewardEarned = { amount, type ->
                                    viewModel.grantRewardedShields(amount, type)
                                },
                                onAdClosed = {}
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("watch_ad_for_reward_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x2200E5FF),
                            contentColor = Color(0xFF00E5FF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎁 ${gameStrings.watchAdForReward}\n(+2 Escudos con imán)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }

                    if (shieldsInventory > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🛡️ ${gameStrings.myShields}: $shieldsInventory",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = gameStrings.swipePrompt,
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Game Over Overlay
        AnimatedVisibility(
            visible = gameStatus == GameStatus.GAME_OVER,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF21C2833)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF5722)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = gameStrings.electricStrike,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = gameStrings.stormHitSubtitle,
                        color = Color(0xFFB0BEC5),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Score Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = gameStrings.points,
                                color = Color(0xAAFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$score",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color(0x33FFFFFF))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = gameStrings.record,
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$highScore",
                                color = Color(0xFFFFD700),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 1. BOTÓN PRINCIPAL CON URGENCIA FOMO Y COPYWRITING DINÁMICO (5 SEGUNDOS)
                    val showReviveButton = !hasRevivedThisMatch && isRewardedAdReady && isFomoActive && isOnline

                    AnimatedVisibility(
                        visible = showReviveButton,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            // Cabecera del temporizador FOMO
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ SÚPER ESCUDO 2X",
                                    color = Color(0xFF00FFFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "⏱️ %.1fs", fomoTimer),
                                    color = Color(0xFFFFD54F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Barra visual de urgencia FOMO (5 segundos)
                            LinearProgressIndicator(
                                progress = { (fomoTimer / 5.0f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF00FFFF),
                                trackColor = Color(0x3300FFFF)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Copywriting dinámico basado en el rendimiento del jugador
                            val isNearRecord = score >= (highScore * 0.7f).toInt().coerceAtLeast(1)
                            val dynamicCopy = if (isNearRecord) {
                                gameStrings.almostBeatRecord
                            } else {
                                String.format(Locale.getDefault(), gameStrings.savePointsSuperShield, score)
                            }

                            Button(
                                onClick = {
                                    viewModel.adMobManager.showRewardedAd(
                                        activity = activity,
                                        onRewardEarned = { amount, type ->
                                            viewModel.grantRewardedShields(amount, type)
                                            viewModel.onReviveWithSuperShield()
                                        },
                                        onAdClosed = {}
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("revive_with_shield_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00B0FF)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dynamicCopy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Botón para recibir recompensa de AdMob en Game Over (+2 Escudos con imán)
                    OutlinedButton(
                        onClick = {
                            viewModel.adMobManager.showRewardedAd(
                                activity = activity,
                                onRewardEarned = { amount, type ->
                                    viewModel.grantRewardedShields(amount, type)
                                },
                                onAdClosed = {}
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("watch_ad_for_reward_button_gameover"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x2200E5FF),
                            contentColor = Color(0xFF00E5FF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎁 ${gameStrings.watchAdForReward}\n(+2 Escudos con imán)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botón "Jugar de Nuevo"
                    Button(
                        onClick = {
                            // Evaluación escalonada de anuncio intersticial (6, 12, 18) y exclusión mutua
                            viewModel.adMobManager.checkAndShowInterstitial(activity) {
                                viewModel.startGame()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("play_again_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0288D1)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = gameStrings.playAgain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 4. Anuncio Nativo de Google AdMob integrado de forma elegante en Game Over
                    GloboupNativeAdView(
                        nativeAd = nativeAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        adLabel = gameStrings.advertisement
                    )
                }
            }
        }

        // 4. PAUSA ANTI-ESTRÉS CON CONTEO (3, 2, 1) POST-ANUNCIO
        AnimatedVisibility(
            visible = isGamePausedForCountdown,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xF00A192F))
                        .border(2.dp, Color(0xFF00FFFF), RoundedCornerShape(32.dp))
                        .padding(horizontal = 40.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = gameStrings.countdownGetReady,
                        color = Color(0xFF00FFFF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300FFFF))
                            .border(3.5.dp, Color(0xFF00FFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (countdownTimer > 0) "$countdownTimer" else "⚡",
                            color = Color(0xFFFFD700),
                            fontSize = 58.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Acomoda tu dedo en la pantalla sin peligro",
                        color = Color(0xDDFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

        // Settings Dialog
        if (showSettingsDialog) {
            var soundOn by remember { mutableStateOf(viewModel.soundAndHaptics.soundEnabled) }
            var hapticsOn by remember { mutableStateOf(viewModel.soundAndHaptics.hapticsEnabled) }

            AlertDialog(
                onDismissRequest = {
                    showSettingsDialog = false
                    viewModel.resumeGame()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = gameStrings.settingsTitle,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        // Selector de Idioma (i18n)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF0288D1))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = gameStrings.languageLabel,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = when (appLanguage) {
                                    AppLanguage.AUTO -> "Auto (${gameStrings.language.displayName})"
                                    else -> appLanguage.displayName
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Botones de selección rápida de idioma
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val supportedLanguages = listOf(
                                AppLanguage.AUTO to "Auto",
                                AppLanguage.ENGLISH to "EN",
                                AppLanguage.SPANISH to "ES",
                                AppLanguage.PORTUGUESE to "PT",
                                AppLanguage.ARABIC to "العربية"
                            )
                            supportedLanguages.forEach { (lang, label) ->
                                val isSelected = appLanguage == lang
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.setLanguage(lang) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Audio Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFF0288D1))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(gameStrings.soundEffects)
                            }
                            Switch(
                                checked = soundOn,
                                onCheckedChange = {
                                    soundOn = it
                                    viewModel.toggleSound(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0288D1))
                            )
                        }

                        // Haptics Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFF0288D1))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(gameStrings.hapticVibration)
                            }
                            Switch(
                                checked = hapticsOn,
                                onCheckedChange = {
                                    hapticsOn = it
                                    viewModel.toggleHaptics(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0288D1))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = gameStrings.localStats,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${gameStrings.statsHighScore}$highScore")
                                Text("${gameStrings.statsStars}${viewModel.preferences.totalStars}")
                                Text("${gameStrings.statsGames}${viewModel.preferences.gamesPlayed}")
                                val nextTarget = when {
                                    defeatCount < 6 -> 6
                                    defeatCount < 12 -> 12
                                    else -> 18
                                }
                                Text("🎯 Ciclo de derrotas: $defeatCount / $nextTarget (Hitos: 6, 12, 18)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reset Record
                        OutlinedButton(
                            onClick = {
                                viewModel.resetStats()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(gameStrings.resetRecord, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // How to Play Button
                        OutlinedButton(
                            onClick = {
                                showHowToPlayDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(gameStrings.howToPlay)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Google Play Security & Privacy Button
                        OutlinedButton(
                            onClick = {
                                showPrivacyDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("privacy_policy_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(gameStrings.privacySecurity, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            viewModel.resumeGame()
                        },
                        modifier = Modifier.testTag("resume_game_button")
                    ) {
                        Text(gameStrings.resume)
                    }
                }
            )
        }

        // Google Play Store Security and Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = gameStrings.privacyDialogTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = gameStrings.privacyComplianceTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = gameStrings.privacyDataSafety,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = gameStrings.privacyLocalStorage,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = gameStrings.privacyPermissions,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = gameStrings.privacyNetworkSecurity,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = gameStrings.privacyBundleReady,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showPrivacyDialog = false }) {
                        Text(gameStrings.understood)
                    }
                }
            )
        }

        // How to Play Dialog
        if (showHowToPlayDialog) {
            AlertDialog(
                onDismissRequest = { showHowToPlayDialog = false },
                title = {
                    Text(text = gameStrings.howToPlayTitle, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = gameStrings.balloonControlGuide,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = gameStrings.goldenStarsGuide,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = gameStrings.stormCloudsGuide,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showHowToPlayDialog = false }) {
                        Text(gameStrings.close)
                    }
                }
            )
        }
    }
}
