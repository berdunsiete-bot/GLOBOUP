package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Palette matching screenshot
val SkyTopColor = Color(0xFF0277BD)
val SkyMidColor = Color(0xFF0288D1)
val SkyBottomColor = Color(0xFF81D4FA)

val StormCloudColor = Color(0xFF263238)
val StormCloudDark = Color(0xFF1E272C)
val LightningYellow = Color(0xFFFFD600)
val LightningGlow = Color(0x66FFEE58)

val StarGold = Color(0xFFFFD700)
val StarGoldGlow = Color(0x55FFCA28)

val BalloonRed = Color(0xFFE53935)
val BalloonRedDark = Color(0xFFC62828)
val BalloonRedLight = Color(0xFFEF5350)
val BasketBrown = Color(0xFF8D6E63)
val BasketDark = Color(0xFF5D4037)
val BurnerFlame = Color(0xFFFFB300)

@Composable
fun GloboUpCanvas(
    balloonX: Float,
    balloonY: Float,
    balloonTilt: Float,
    clouds: List<StormCloud>,
    stars: List<StarCollectible>,
    bgClouds: List<BackgroundCloud>,
    particles: List<SparkleParticle>,
    floatingTexts: List<FloatingText>,
    onDrag: (deltaX: Float, deltaY: Float, widthPx: Float, heightPx: Float) -> Unit,
    onTap: (xNorm: Float, yNorm: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTap(offset.x / size.width, offset.y / size.height)
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Sky Gradient Background (Darker sky blue at top, lighter cyan-blue at bottom)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(SkyTopColor, SkyMidColor, SkyBottomColor),
                startY = 0f,
                endY = height
            )
        )

        // 2. Soft Ambient Background Clouds
        for (bg in bgClouds) {
            drawAmbientCloud(
                centerX = bg.x * width,
                centerY = bg.y * height,
                baseRadius = bg.size * width * 0.5f,
                alpha = bg.alpha
            )
        }

        // 3. Falling Golden Stars
        for (star in stars) {
            val starPxX = star.x * width
            val starPxY = star.y * height
            val starRadiusPx = star.radius * width

            drawStarCollectible(
                centerX = starPxX,
                centerY = starPxY,
                outerRadius = starRadiusPx,
                rotationDegrees = star.rotation
            )
        }

        // 4. Falling Storm Clouds with Lightning Bolts
        for (cloud in clouds) {
            val cloudPxX = cloud.x * width
            val cloudPxY = cloud.y * height
            val cloudRadXPx = cloud.radiusX * width
            val cloudRadYPx = cloud.radiusY * height

            drawStormCloudWithLightning(
                centerX = cloudPxX,
                centerY = cloudPxY,
                radiusX = cloudRadXPx,
                radiusY = cloudRadYPx,
                lightningLengthPx = cloud.lightningLength * height,
                lightningPoints = cloud.lightningPoints,
                lightningAlpha = cloud.lightningAlpha,
                width = width,
                height = height
            )
        }

        // 5. Hot Air Balloon (Player)
        val balloonPxX = balloonX * width
        val balloonPxY = balloonY * height
        val balloonScale = width * 0.16f

        drawHotAirBalloon(
            centerX = balloonPxX,
            centerY = balloonPxY,
            size = balloonScale,
            tiltDegrees = balloonTilt
        )

        // 6. Sparkle Particles
        for (p in particles) {
            drawCircle(
                color = Color(p.color).copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * width, p.y * height)
            )
        }

        // 7. Floating "+1" texts
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = width * 0.055f
            isFakeBoldText = true
            setShadowLayer(8f, 0f, 2f, android.graphics.Color.argb(160, 0, 0, 0))
        }

        for (ft in floatingTexts) {
            textPaint.alpha = (ft.alpha * 255).toInt().coerceIn(0, 255)
            drawContext.canvas.nativeCanvas.drawText(
                ft.text,
                ft.x * width,
                ft.y * height,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawAmbientCloud(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    alpha: Float
) {
    val cloudPaintColor = Color.White.copy(alpha = alpha)
    drawCircle(cloudPaintColor, radius = baseRadius * 0.9f, center = Offset(centerX, centerY))
    drawCircle(cloudPaintColor, radius = baseRadius * 0.7f, center = Offset(centerX - baseRadius * 0.8f, centerY + baseRadius * 0.15f))
    drawCircle(cloudPaintColor, radius = baseRadius * 0.75f, center = Offset(centerX + baseRadius * 0.8f, centerY + baseRadius * 0.15f))
}

private fun DrawScope.drawStormCloudWithLightning(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    lightningLengthPx: Float,
    lightningPoints: List<Pair<Float, Float>>,
    lightningAlpha: Float,
    width: Float,
    height: Float
) {
    // 1. Draw Jagged Lightning Bolt protruding from bottom of cloud
    val lightningOriginX = centerX
    val lightningOriginY = centerY + radiusY * 0.4f

    val boltPath = Path().apply {
        moveTo(lightningOriginX, lightningOriginY)
        if (lightningPoints.isNotEmpty()) {
            for (pt in lightningPoints) {
                lineTo(lightningOriginX + pt.first * width, lightningOriginY + pt.second * height)
            }
        } else {
            // Default lightning zigzag
            lineTo(lightningOriginX - 10f, lightningOriginY + lightningLengthPx * 0.35f)
            lineTo(lightningOriginX + 8f, lightningOriginY + lightningLengthPx * 0.65f)
            lineTo(lightningOriginX - 4f, lightningOriginY + lightningLengthPx)
        }
    }

    // Glow effect for lightning
    drawPath(
        path = boltPath,
        color = LightningGlow.copy(alpha = 0.5f * lightningAlpha),
        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Crisp inner lightning bolt
    drawPath(
        path = boltPath,
        color = LightningYellow.copy(alpha = lightningAlpha),
        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Lightning tip spark
    val tipOffset = if (lightningPoints.isNotEmpty()) {
        val last = lightningPoints.last()
        Offset(lightningOriginX + last.first * width, lightningOriginY + last.second * height)
    } else {
        Offset(lightningOriginX - 4f, lightningOriginY + lightningLengthPx)
    }
    drawCircle(
        color = LightningYellow.copy(alpha = lightningAlpha),
        radius = 5f,
        center = tipOffset
    )

    // 2. Draw Storm Cloud Body (Dark slate gray puffs)
    // Center puff
    drawCircle(
        color = StormCloudColor,
        radius = radiusX * 0.92f,
        center = Offset(centerX, centerY)
    )
    // Left puff
    drawCircle(
        color = StormCloudDark,
        radius = radiusX * 0.76f,
        center = Offset(centerX - radiusX * 0.62f, centerY + radiusY * 0.15f)
    )
    // Right puff
    drawCircle(
        color = StormCloudColor,
        radius = radiusX * 0.80f,
        center = Offset(centerX + radiusX * 0.60f, centerY + radiusY * 0.12f)
    )
    // Bottom flatter puff
    drawCircle(
        color = StormCloudDark,
        radius = radiusX * 0.70f,
        center = Offset(centerX, centerY + radiusY * 0.35f)
    )

    // Subtle edge highlight on top of cloud
    drawArc(
        color = Color(0x33FFFFFF),
        startAngle = 190f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(centerX - radiusX * 0.90f, centerY - radiusX * 0.90f),
        size = Size(radiusX * 1.8f, radiusX * 1.8f),
        style = Stroke(width = 4f)
    )
}

private fun DrawScope.drawStarCollectible(
    centerX: Float,
    centerY: Float,
    outerRadius: Float,
    rotationDegrees: Float
) {
    // Soft outer golden halo glow
    drawCircle(
        color = StarGoldGlow,
        radius = outerRadius * 1.7f,
        center = Offset(centerX, centerY)
    )

    rotate(rotationDegrees, pivot = Offset(centerX, centerY)) {
        val path = Path()
        val innerRadius = outerRadius * 0.44f
        val points = 5
        val step = PI / points

        for (i in 0 until 2 * points) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * step - (PI / 2.0)
            val px = (centerX + r * cos(angle)).toFloat()
            val py = (centerY + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        // Draw solid gold star
        drawPath(
            path = path,
            color = StarGold,
            style = Fill
        )

        // Draw star stroke for crispness
        drawPath(
            path = path,
            color = Color(0xFFFFF176),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawHotAirBalloon(
    centerX: Float,
    centerY: Float,
    size: Float,
    tiltDegrees: Float
) {
    rotate(tiltDegrees, pivot = Offset(centerX, centerY)) {
        val envelopeWidth = size * 0.88f
        val envelopeHeight = size * 1.15f
        val envelopeCenterY = centerY - size * 0.15f

        // 1. Suspension Ropes
        val basketTopY = centerY + size * 0.58f
        val basketCenterX = centerX
        val ropeColor = Color(0xCC37474F)

        // Left rope
        drawLine(
            color = ropeColor,
            start = Offset(centerX - envelopeWidth * 0.22f, envelopeCenterY + envelopeHeight * 0.46f),
            end = Offset(basketCenterX - size * 0.14f, basketTopY),
            strokeWidth = 2.5f
        )
        // Right rope
        drawLine(
            color = ropeColor,
            start = Offset(centerX + envelopeWidth * 0.22f, envelopeCenterY + envelopeHeight * 0.46f),
            end = Offset(basketCenterX + size * 0.14f, basketTopY),
            strokeWidth = 2.5f
        )

        // 2. Burner Flame Glow
        drawCircle(
            color = BurnerFlame,
            radius = size * 0.08f,
            center = Offset(centerX, envelopeCenterY + envelopeHeight * 0.49f)
        )
        drawCircle(
            color = Color(0xFFFFF9C4),
            radius = size * 0.045f,
            center = Offset(centerX, envelopeCenterY + envelopeHeight * 0.49f)
        )

        // 3. Basket (Wicker brown box)
        val basketWidth = size * 0.28f
        val basketHeight = size * 0.22f
        drawRect(
            color = BasketBrown,
            topLeft = Offset(basketCenterX - basketWidth * 0.5f, basketTopY),
            size = Size(basketWidth, basketHeight)
        )
        // Basket rim
        drawRect(
            color = BasketDark,
            topLeft = Offset(basketCenterX - basketWidth * 0.54f, basketTopY),
            size = Size(basketWidth * 1.08f, basketHeight * 0.32f)
        )

        // 4. Balloon Envelope (Teardrop Hot Air Balloon)
        val envelopePath = Path().apply {
            val top = envelopeCenterY - envelopeHeight * 0.5f
            val bottom = envelopeCenterY + envelopeHeight * 0.48f
            val left = centerX - envelopeWidth * 0.5f
            val right = centerX + envelopeWidth * 0.5f
            val neckWidth = envelopeWidth * 0.32f

            moveTo(centerX, top)
            // Right top curve
            cubicTo(
                right + envelopeWidth * 0.1f, top + envelopeHeight * 0.15f,
                right, envelopeCenterY + envelopeHeight * 0.2f,
                centerX + neckWidth * 0.5f, bottom
            )
            // Bottom neck line
            lineTo(centerX - neckWidth * 0.5f, bottom)
            // Left bottom to top curve
            cubicTo(
                left, envelopeCenterY + envelopeHeight * 0.2f,
                left - envelopeWidth * 0.1f, top + envelopeHeight * 0.15f,
                centerX, top
            )
            close()
        }

        // Fill balloon main red
        drawPath(
            path = envelopePath,
            brush = Brush.horizontalGradient(
                colors = listOf(BalloonRedDark, BalloonRed, BalloonRedLight, BalloonRed, BalloonRedDark),
                startX = centerX - envelopeWidth * 0.5f,
                endX = centerX + envelopeWidth * 0.5f
            )
        )

        // Vertical stripe highlights on the balloon
        val stripePath = Path().apply {
            val top = envelopeCenterY - envelopeHeight * 0.5f
            val bottom = envelopeCenterY + envelopeHeight * 0.48f
            val centerStripeWidth = envelopeWidth * 0.26f

            moveTo(centerX, top)
            cubicTo(
                centerX + centerStripeWidth * 0.8f, top + envelopeHeight * 0.2f,
                centerX + centerStripeWidth * 0.8f, envelopeCenterY + envelopeHeight * 0.25f,
                centerX + envelopeWidth * 0.08f, bottom
            )
            lineTo(centerX - envelopeWidth * 0.08f, bottom)
            cubicTo(
                centerX - centerStripeWidth * 0.8f, envelopeCenterY + envelopeHeight * 0.25f,
                centerX - centerStripeWidth * 0.8f, top + envelopeHeight * 0.2f,
                centerX, top
            )
            close()
        }
        drawPath(
            path = stripePath,
            color = Color(0x33FFFFFF)
        )

        // Specular curved reflection on upper left
        drawArc(
            color = Color(0x55FFFFFF),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(centerX - envelopeWidth * 0.42f, envelopeCenterY - envelopeHeight * 0.44f),
            size = Size(envelopeWidth * 0.75f, envelopeHeight * 0.65f),
            style = Stroke(width = size * 0.05f, cap = StrokeCap.Round)
        )
    }
}
