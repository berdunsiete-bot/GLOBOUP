package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
fun GloboUpScreen(
    viewModel: GloboUpViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
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

        // 2. Top Bar HUD (Matching Screenshot)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top-Left: Capsule Badge with "???? GloboUp" (Balloon icon + GloboUp)
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
                        text = "Globoup",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Top-Right: Points Capsule Card (Matching Screenshot "PUNTOS / 5")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x381565C0))
                    .border(1.5.dp, Color(0x55FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "PUNTOS",
                        color = Color(0xCCFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$score",
                        color = Color(0xFFE8F5E9),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 3. Bottom-Right Settings Button (Matching Screenshot dark circular button with gear icon)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
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
                        text = "🎈 Globoup ⚡",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "¡Esquiva las tormentas con rayos y recolecta todas las estrellas doradas!",
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
                                text = "Récord: $highScore",
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
                            text = "¡Despegar!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Desliza el dedo en la pantalla para mover el globo",
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
                        text = "¡Rayo Eléctrico!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tu globo impactó con una tormenta",
                        color = Color(0xFFB0BEC5),
                        fontSize = 14.sp
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
                                text = "PUNTOS",
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
                                text = "RÉCORD",
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

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = { viewModel.startGame() },
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
                            text = "Jugar de Nuevo",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
                        text = "Ajustes de Globoup",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Audio Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF0288D1))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Efectos de Sonido")
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
                            Text("Vibración Háptica")
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
                                text = "Estadísticas Locales",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Puntuación Récord: $highScore")
                            Text("Estrellas Recogidas: ${viewModel.preferences.totalStars}")
                            Text("Partidas Totales: ${viewModel.preferences.gamesPlayed}")
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
                        Text("Restablecer Récord", color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // How to Play Button
                    OutlinedButton(
                        onClick = {
                            showHowToPlayDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cómo Jugar")
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
                        Text("Seguridad Google Play y Privacidad", fontSize = 12.sp)
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
                    Text("Continuar")
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
                        text = "Seguridad de Google Play",
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
                        text = "Declaración de Cumplimiento de Políticas (Google Play Store):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✓ Privacidad y Datos (Data Safety):\n" +
                                "Esta aplicación no recopila, no comparte ni transmite ningún dato personal, ubicación, ID de dispositivo ni información sensible del usuario a servidores externos.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✓ Almacenamiento 100% Local:\n" +
                                "Toda la información del juego (récord de puntos, estrellas y opciones de audio) se guarda exclusivamente de forma local y privada en tu dispositivo (SharedPreferences seguras).",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✓ Principio de Menor Privilegio (Permisos Mínimos):\n" +
                                "No se solicitan permisos peligrosos ni invasivos (sin cámara, sin micrófono, sin contactos, sin almacenamiento externo). Únicamente se declara el permiso normal de VIBRACIÓN para respuesta háptica al jugar.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✓ Seguridad de Red:\n" +
                                "android:usesCleartextTraffic está deshabilitado por política de seguridad, impidiendo comunicaciones inseguras no encriptadas.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✓ Compatibilidad para APK y AAB (Android App Bundle):\n" +
                                "El código fuente está optimizado con Target SDK 36, listo para ser firmado y empaquetado en formato APK o Android App Bundle (.aab) para su publicación en Google Play Console.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // How to Play Dialog
    if (showHowToPlayDialog) {
        AlertDialog(
            onDismissRequest = { showHowToPlayDialog = false },
            title = {
                Text(text = "Cómo Jugar a Globoup", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "🎈 Control del Globo:\n" +
                                "Arrastra o pulsa en la pantalla para mover el globo aerostático a la izquierda o derecha.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⭐ Estrellas Doradas:\n" +
                                "Recoge todas las estrellas brillantes para sumar puntos y batir tu récord personal.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⛈️ Nubes de Tormenta con Rayos:\n" +
                                "¡Cuidado! No permitas que el globo toque las nubes negras ni los rayos eléctricos amarillos o el vuelo terminará.",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHowToPlayDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
