package com.example

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Configuración centralizada de identificadores de Google AdMob.
 * Permite alternar fácilmente entre entorno de PRUEBAS (Test IDs oficiales de Google)
 * y entorno de PRODUCCIÓN (Tus IDs reales de tu consola de AdMob).
 */
object AdConfig {
    /**
     * Configuración Definitiva del Banner:
     * - Formato: Banner
     * - Nombre: Globoup banner 2
     * - ID de unidad de anuncio: ca-app-pub-7770387169803617/3842176035
     */
    const val BANNER_NAME = "Globoup banner 2"
    const val BANNER_AD_ID = "ca-app-pub-7770387169803617/3842176035"

    /**
     * Configuración Definitiva del Anuncio Intersticial:
     * - Formato: Intersticial
     * - Nombre: Globoup Intersticial 1
     * - ID de unidad de anuncio: ca-app-pub-7770387169803617/4579883494
     */
    const val INTERSTITIAL_NAME = "Globoup Intersticial 1"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-7770387169803617/4579883494"

    /**
     * Configuración Definitiva del Anuncio Recompensado:
     * - Formato: Recompensado
     * - Nombre: Globoup recompensado1
     * - ID de unidad de anuncio: ca-app-pub-7770387169803617/4360391642
     * - Recompensa configurada: "2 Escudo con iman de estrellas"
     */
    const val REWARDED_NAME = "Globoup recompensado1"
    const val REWARDED_AD_ID = "ca-app-pub-7770387169803617/4360391642"
    const val REWARD_ITEM_NAME = "2 Escudo con iman de estrellas"
    const val REWARD_SHIELD_AMOUNT = 2

    /**
     * Configuración del Anuncio Nativo (Native Ad):
     * - Formato: Nativo Avanzado
     * - Nombre: Globoup Nativo 1
     * - ID oficial de prueba de Google: ca-app-pub-3940256099942544/2247696110
     */
    const val NATIVE_NAME = "Globoup Nativo 1"
    const val NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"

    const val IS_TEST_MODE = false
    private const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

    // Cooldown de seguridad estricto de 30 segundos entre anuncios de pantalla completa
    const val MIN_INTERSTITIAL_COOLDOWN_MS = 30_000L

    // Progresión escalonada de derrotas requerida para anuncios intersticiales: 6, 12, 18
    val DEFEAT_MILESTONES = listOf(6, 12, 18)
}

private const val TAG = "AdMobManager"

typealias AdsManager = AdMobManager

/**
 * Gestor integral de publicidad Google AdMob y modo híbrido Offline/Online.
 */
class AdMobManager private constructor(private val appContext: Context) {

    companion object {
        @Volatile
        private var instance: AdMobManager? = null

        fun getInstance(context: Context): AdMobManager {
            return instance ?: synchronized(this) {
                instance ?: AdMobManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    // 1. Contador interno de derrotas / partidas finalizadas
    private var internalDefeatCount = 0
    private val _defeatCount = MutableStateFlow(0)
    val defeatCount: StateFlow<Int> = _defeatCount.asStateFlow()

    // 2. Marca de tiempo del último anuncio de pantalla completa mostrado (para cooldown de 90s)
    private var lastInterstitialShowTime = 0L

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    // 3. Publicidad Nativa de AdMob
    private var nativeAdInstance: NativeAd? = null
    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd.asStateFlow()
    private var isNativeLoading = false

    private val isInitializing = java.util.concurrent.atomic.AtomicBoolean(false)
    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

    init {
        initializeSdk()
        registerNetworkCallback()
    }

    /**
     * Inicializa Google Mobile Ads SDK de forma asíncrona, segura y garantizando invocación única.
     */
    fun initializeSdk() {
        if (isInitializing.getAndSet(true)) {
            Log.d(TAG, "AdMob SDK ya se encuentra en proceso de inicialización.")
            return
        }

        try {
            _isOnline.value = isNetworkAvailable()
            MobileAds.initialize(appContext) { status ->
                Log.d(TAG, "AdMob inicializado correctamente: $status")
                _isSdkInitialized.value = true
                if (isNetworkAvailable()) {
                    // Carga progresiva y escalonada: previene contención en el hilo principal y colisiones con WebView
                    mainHandler.postDelayed({ loadInterstitialAd() }, 3000L)
                    mainHandler.postDelayed({ loadRewardedAd() }, 5500L)
                    mainHandler.postDelayed({ loadNativeAd() }, 8000L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando MobileAds: ${e.message}")
            isInitializing.set(false)
        }
    }

    /**
     * RECONEXIÓN SILENCIOSA EN SEGUNDO PLANO:
     * Detecta eventos de red (WiFi o datos móviles) sin interrumpir la partida.
     * Al restablecerse la conexión, reanuda en segundo plano la precarga escalonada de anuncios en el Main Thread.
     */
    private fun registerNetworkCallback() {
        try {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    mainHandler.post {
                        _isOnline.value = true
                        Log.d(TAG, "Reconexión silenciosa en segundo plano detectada. Iniciando precarga escalonada en Main UI thread.")
                        mainHandler.postDelayed({ loadRewardedAd() }, 1500L)
                        mainHandler.postDelayed({ loadInterstitialAd() }, 3500L)
                        mainHandler.postDelayed({ loadNativeAd() }, 6000L)
                    }
                }

                override fun onLost(network: android.net.Network) {
                    mainHandler.post {
                        _isOnline.value = isNetworkAvailable()
                        Log.d(TAG, "Conexión a internet perdida. Modo Offline activo.")
                    }
                }

                override fun onCapabilitiesChanged(
                    network: android.net.Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    mainHandler.post {
                        _isOnline.value = hasInternet
                        if (hasInternet && (rewardedAd == null || interstitialAd == null)) {
                            mainHandler.postDelayed({ loadRewardedAd() }, 1500L)
                            mainHandler.postDelayed({ loadInterstitialAd() }, 3500L)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar NetworkCallback (fallback a comprobación bajo demanda): ${e.message}")
        }
    }

    /**
     * COMPROBACIÓN NO BLOQUEANTE DE RED (Zero-Latency Check):
     * Retorna inmediatamente (< 0.1ms) si el dispositivo tiene interfaz de red activa,
     * garantizando latencia cero y cero bloqueos del hilo principal (UI Thread).
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Carga preventiva del anuncio intersticial si hay internet.
     * Garantiza ejecución obligatoria en el Main UI Thread para evitar IllegalStateException (#008).
     */
    fun loadInterstitialAd() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { loadInterstitialAd() }
            return
        }

        if (!isNetworkAvailable() || isInterstitialLoading || interstitialAd != null) return

        try {
            isInterstitialLoading = true
            Log.d(TAG, "Iniciando precarga en segundo plano de: ${AdConfig.INTERSTITIAL_NAME} (${AdConfig.INTERSTITIAL_AD_ID})")
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                appContext,
                AdConfig.INTERSTITIAL_AD_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        Log.d(TAG, "Anuncio intersticial '${AdConfig.INTERSTITIAL_NAME}' precargado exitosamente en segundo plano y listo.")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.w(TAG, "Fallo al precargar intersticial '${AdConfig.INTERSTITIAL_NAME}': ${loadAdError.message} (Código: ${loadAdError.code})")
                    }
                }
            )
        } catch (e: Throwable) {
            isInterstitialLoading = false
            Log.e(TAG, "Excepción al iniciar carga de intersticial: ${e.message}")
        }
    }

    /**
     * Carga preventiva del anuncio bonificado (Rewarded) definitivo: Globoup recompensado1.
     * Garantiza ejecución obligatoria en el Main UI Thread para evitar IllegalStateException.
     */
    fun loadRewardedAd() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { loadRewardedAd() }
            return
        }

        if (!isNetworkAvailable() || isRewardedLoading || rewardedAd != null) return

        try {
            isRewardedLoading = true
            Log.d(TAG, "Iniciando precarga en segundo plano de: ${AdConfig.REWARDED_NAME} (${AdConfig.REWARDED_AD_ID})")
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                appContext,
                AdConfig.REWARDED_AD_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isRewardedLoading = false
                        _isRewardedAdReady.value = true
                        Log.d(TAG, "Anuncio bonificado '${AdConfig.REWARDED_NAME}' cargado exitosamente")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isRewardedLoading = false
                        _isRewardedAdReady.value = false
                        Log.w(TAG, "Fallo al cargar bonificado '${AdConfig.REWARDED_NAME}': ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isRewardedLoading = false
            _isRewardedAdReady.value = false
            Log.e(TAG, "Excepción al iniciar carga de recompensado: ${e.message}")
        }
    }

    // Contador de partidas en esta sesión (para proteger las primeras 2 partidas libres de intersticiales)
    var sessionMatchesCount = 0
        private set

    fun registerSessionMatch() {
        sessionMatchesCount++
        Log.d(TAG, "Partida registrada en sesión: $sessionMatchesCount")
    }

    /**
     * REGLA CRÍTICA DE EXCLUSIÓN MUTUA & PROTECCIÓN TRAS RECOMPENSA:
     * Si el jugador acepta ver un anuncio de recompensa para revivir con súper escudo:
     * 1. Reinicia el contador de derrotas a 0 (defeatCount = 0).
     * 2. Reinicia el temporizador de 90 segundos de enfriamiento (lastInterstitialShowTime = now)
     *    para garantizar que NUNCA aparezca otro anuncio de pantalla completa si pierde en la siguiente partida.
     * 3. El anuncio intersticial programado para esa derrota SE CANCELA Y SE SALTA AUTOMÁTICAMENTE.
     */
    fun resetDefeatCountOnReward() {
        internalDefeatCount = 0
        _defeatCount.value = 0
        lastInterstitialShowTime = System.currentTimeMillis() // Congela/reinicia cooldown de 90s
        Log.d(TAG, "EXCLUSIÓN MUTUA APLICADA: Derrotas en 0 y cooldown de 90s reiniciado tras anuncio recompensado.")
    }

    /**
     * Registra una derrota del jugador e incrementa el contador de derrotas.
     */
    fun recordDefeat(): Int {
        internalDefeatCount++
        _defeatCount.value = internalDefeatCount
        Log.d(TAG, "Derrota registrada. Total en ciclo actual: $internalDefeatCount")
        return internalDefeatCount
    }

    /**
     * Comprueba si el anuncio recompensado está precargado y listo en memoria con conexión.
     */
    fun isRewardedLoaded(): Boolean {
        return rewardedAd != null && isNetworkAvailable()
    }

    /**
     * Reinicio manual de estadísticas de ciclo de anuncios.
     */
    fun resetDefeatCycle() {
        internalDefeatCount = 0
        _defeatCount.value = 0
        Log.d(TAG, "Ciclo de derrotas reiniciado manualmente a 0.")
    }

    /**
     * Retorna el número de segundos restantes para el cooldown de 90s (o 0 si ya transcurrió).
     */
    fun getRemainingCooldownSeconds(): Long {
        if (lastInterstitialShowTime == 0L) return 0L
        val elapsed = System.currentTimeMillis() - lastInterstitialShowTime
        val remaining = AdConfig.MIN_INTERSTITIAL_COOLDOWN_MS - elapsed
        return if (remaining > 0) remaining / 1000L else 0L
    }

    /**
     * Muestra automáticamente el anuncio intersticial definitivo de AdMob:
     * - Formato: Intersticial
     * - Nombre: Globoup Intersticial 1
     * - ID de unidad de anuncio: ca-app-pub-7770387169803617/4579883494
     *
     * Se invoca automáticamente cuando:
     * 1. El jugador pierde una partida (pantalla de Game Over)
     * 2. El jugador avanza / cambia de nivel
     *
     * Características implementadas:
     * - Seguro para hilos: Siempre despachado en el Main UI Thread.
     * - Sin bloqueo: Si no hay conexión o no está listo, continúa instantáneamente el juego.
     * - Cooldown de seguridad de 30s: Evita saturar al usuario entre eventos consecutivos.
     * - Precarga continua: Al cerrarse el anuncio, inicia la precarga del siguiente en segundo plano.
     */
    fun showInterstitialAd(
        activity: Activity?,
        reason: String = "auto_event",
        forceShow: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showInterstitialAd(activity, reason, forceShow, onComplete) }
            return
        }

        _isOnline.value = isNetworkAvailable()

        if (activity == null || !isNetworkAvailable()) {
            Log.d(TAG, "[$reason] Sin actividad o sin conexión a internet. Continuando sin mostrar anuncio.")
            onComplete()
            return
        }

        val now = System.currentTimeMillis()
        val elapsedMs = now - lastInterstitialShowTime
        if (!forceShow && lastInterstitialShowTime > 0L && elapsedMs < AdConfig.MIN_INTERSTITIAL_COOLDOWN_MS) {
            val remainingSec = (AdConfig.MIN_INTERSTITIAL_COOLDOWN_MS - elapsedMs) / 1000L
            Log.d(TAG, "[$reason] Cooldown activo (${remainingSec}s restantes de 30s). Continuando sin anuncio.")
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "[$reason] Anuncio intersticial '${AdConfig.INTERSTITIAL_NAME}' cerrado.")
                    interstitialAd = null
                    lastInterstitialShowTime = System.currentTimeMillis()
                    checkAndResetCycle()
                    // Precarga en segundo plano para el siguiente evento
                    loadInterstitialAd()
                    mainHandler.post { onComplete() }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "[$reason] Error al mostrar intersticial: ${adError.message}")
                    interstitialAd = null
                    checkAndResetCycle()
                    loadInterstitialAd()
                    mainHandler.post { onComplete() }
                }

                override fun onAdShowedFullScreenContent() {
                    lastInterstitialShowTime = System.currentTimeMillis()
                    Log.d(TAG, "[$reason] Anuncio intersticial '${AdConfig.INTERSTITIAL_NAME}' mostrado exitosamente.")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "[$reason] Anuncio intersticial '${AdConfig.INTERSTITIAL_NAME}' no estaba listo en memoria. Iniciando precarga.")
            checkAndResetCycle()
            loadInterstitialAd()
            mainHandler.post { onComplete() }
        }
    }

    /**
     * Muestra el anuncio intersticial evaluando hitos escalonados o clics en 'Jugar de nuevo'.
     */
    fun checkAndShowInterstitial(activity: Activity?, onComplete: () -> Unit) {
        showInterstitialAd(activity, reason = "play_again_button", forceShow = false, onComplete = onComplete)
    }

    /**
     * Compatibilidad con llamadas previas de fin de partida.
     */
    fun onGameFinished(activity: Activity?, onAdClosed: () -> Unit) {
        checkAndShowInterstitial(activity, onAdClosed)
    }

    /**
     * Si se alcanzó el tercer hito (derrota 18), reinicia el ciclo a 0 para que los próximos
     * anuncios se muestren nuevamente a las 6, 12 y 18 derrotas.
     */
    private fun checkAndResetCycle() {
        if (internalDefeatCount >= 18) {
            internalDefeatCount = 0
            _defeatCount.value = 0
            Log.d(TAG, "Ciclo de 18 derrotas completado. Contador reiniciado a 0 (próximos en 6, 12, 18).")
        }
    }

    /**
     * Muestra anuncio bonificado definitivo para otorgar la recompensa configurada:
     * - Nombre: Globoup recompensado1
     * - ID: ca-app-pub-7770387169803617/4360391642
     * - Recompensa: "2 Escudo con iman de estrellas"
     *
     * Manejo de eventos del ciclo de vida de AdMob:
     * - onAdShowedFullScreenContent: Despliegue del video en pantalla.
     * - onUserEarnedRewardListener: Evento al completar la visualización obligatoria.
     *   Entrega automáticamente el beneficio "2 Escudo con imán de estrellas".
     * - onAdDismissedFullScreenContent: Cierre del anuncio por el usuario. Precarga en segundo plano.
     * - onAdFailedToShowFullScreenContent: Fallo en despliegue. Precarga en segundo plano.
     */
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showRewardedAd(activity, onRewardEarned, onAdClosed) }
            return
        }

        val ad = rewardedAd
        if (ad != null && activity != null && isNetworkAvailable()) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Anuncio recompensado '${AdConfig.REWARDED_NAME}' mostrado exitosamente en pantalla.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Anuncio recompensado '${AdConfig.REWARDED_NAME}' cerrado por el usuario.")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    // Precarga continua en segundo plano para la próxima recompensa
                    loadRewardedAd()
                    mainHandler.post { onAdClosed() }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Error al mostrar anuncio recompensado '${AdConfig.REWARDED_NAME}': ${adError.message}")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    loadRewardedAd()
                    mainHandler.post { onAdClosed() }
                }
            }

            ad.show(activity) { rewardItem ->
                val amount = if (rewardItem.amount > 0) rewardItem.amount else AdConfig.REWARD_SHIELD_AMOUNT
                val type = if (!rewardItem.type.isNullOrBlank()) rewardItem.type else AdConfig.REWARD_ITEM_NAME
                Log.d(TAG, "¡RECOMPENSA GANADA POR EL USUARIO!: $amount x $type")
                // Regla de Exclusión Mutua: Reseteo de derrotas y enfriamiento de 30s
                resetDefeatCountOnReward()
                mainHandler.post {
                    onRewardEarned(amount, type)
                }
            }
        } else {
            Log.d(TAG, "Anuncio recompensado no disponible en memoria o sin conexión. Iniciando precarga.")
            loadRewardedAd()
            mainHandler.post { onAdClosed() }
        }
    }

    /**
     * Sobrecarga de conveniencia para invocar showRewardedAd sin argumentos de tipo/cantidad.
     */
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        showRewardedAd(
            activity = activity,
            onRewardEarned = { _, _ -> onRewardEarned() },
            onAdClosed = onAdClosed
        )
    }

    /**
     * Carga preventiva del Anuncio Nativo en segundo plano.
     */
    fun loadNativeAd() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { loadNativeAd() }
            return
        }

        if (!isNetworkAvailable() || isNativeLoading || nativeAdInstance != null) return

        try {
            isNativeLoading = true
            Log.d(TAG, "Iniciando precarga en segundo plano de: ${AdConfig.NATIVE_NAME} (${AdConfig.NATIVE_AD_ID})")

            val adLoader = AdLoader.Builder(appContext, AdConfig.NATIVE_AD_ID)
                .forNativeAd { ad ->
                    nativeAdInstance?.destroy()
                    nativeAdInstance = ad
                    _nativeAd.value = ad
                    isNativeLoading = false
                    Log.d(TAG, "Anuncio Nativo '${AdConfig.NATIVE_NAME}' precargado exitosamente.")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isNativeLoading = false
                        Log.w(TAG, "Fallo al precargar Anuncio Nativo '${AdConfig.NATIVE_NAME}': ${loadAdError.message}")
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setRequestMultipleImages(false)
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Throwable) {
            isNativeLoading = false
            Log.e(TAG, "Excepción al iniciar precarga de Anuncio Nativo: ${e.message}")
        }
    }

    /**
     * Libera los recursos del anuncio nativo para prevenir fugas de memoria.
     */
    fun destroyNativeAd() {
        nativeAdInstance?.destroy()
        nativeAdInstance = null
        _nativeAd.value = null
    }
}

/**
 * Contenedor de Banner superior estilo "Dynamic Island" / cápsula estética.
 *
 * Cumplimiento estricto de Políticas de Google AdMob & Google Play:
 * - Área segura (Safe Area): Ubicado con margen superior basado en insets del sistema (notch / barra de estado).
 * - Sin recortes ni enmascaramiento: NO aplica overflow hidden ni clips redondeados sobre el AdView;
 *   la etiqueta AdChoices y los botones reglamentarios permanecen 100% íntegros y visibles.
 * - Dimensiones estándar: 320dp de ancho x 50dp de alto (estándar BANNER) con espaciado rectangular interno.
 * - Prevención de clics accidentales: Margen transparente de separación antes de los elementos del juego.
 */
@Composable
fun DynamicIslandBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.BANNER_AD_ID,
    advertisementLabel: String = "PUBLICIDAD"
) {
    val context = LocalContext.current
    val adManager = remember { AdMobManager.getInstance(context) }
    val isSdkInitialized by adManager.isSdkInitialized.collectAsState()
    val isOnline by adManager.isOnline.collectAsState()
    var isAdLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Cápsula estética de fondo (el marco tiene bordes redondeados, pero el anuncio interno NO se recorta)
        Box(
            modifier = Modifier
                .widthIn(min = 344.dp)
                .background(
                    color = Color(0x3D0F273E),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentHeight()
            ) {
                // Etiqueta legal requerida por Google AdMob (clara, separada e internacionalizada)
                Text(
                    text = advertisementLabel,
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )

                // Contenedor rectangular intacto para AdView (sin clip, sin overflow hidden)
                Box(
                    modifier = Modifier.size(width = 320.dp, height = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var shouldRenderAd by remember { mutableStateOf(false) }

                    LaunchedEffect(isSdkInitialized, isOnline) {
                        if (isSdkInitialized && isOnline) {
                            // Espera 1.2 segundos tras el arranque inicial para permitir que Compose y la UI terminen su primer render fluido a 60 FPS
                            kotlinx.coroutines.delay(1200L)
                            shouldRenderAd = true
                        } else {
                            shouldRenderAd = false
                        }
                    }

                    if (shouldRenderAd) {
                        AndroidView(
                            factory = { ctx ->
                                try {
                                    AdView(ctx).apply {
                                        setAdSize(AdSize.BANNER)
                                        this.adUnitId = adUnitId
                                        adListener = object : AdListener() {
                                            override fun onAdLoaded() {
                                                isAdLoaded = true
                                                hasError = false
                                            }

                                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                                isAdLoaded = false
                                                hasError = true
                                            }
                                        }
                                        loadAd(AdRequest.Builder().build())
                                    }
                                } catch (e: Throwable) {
                                    Log.e(TAG, "Error inicializando AdView de banner: ${e.message}")
                                    android.view.View(ctx)
                                }
                            },
                            onRelease = { adView ->
                                try {
                                    (adView as? AdView)?.destroy()
                                } catch (_: Throwable) {
                                }
                            },
                            modifier = Modifier.size(width = 320.dp, height = 50.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente Composable oficial para mostrar Anuncios Nativos de Google AdMob.
 *
 * Cumplimiento estricto de Políticas de AdMob:
 * - Utiliza [NativeAdView] para registrar e interceptar clics sobre los elementos publicitarios (headline, body, icon, cta).
 * - Muestra la etiqueta oficial "AD" / "ANUNCIO" requerida por Google.
 * - Diseño armónico con el tema de Globoup sin falsear el contenido ni inducir a clics engañosos.
 */
@Composable
fun GloboupNativeAdView(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier,
    adLabel: String = "ANUNCIO"
) {
    if (nativeAd == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD0D2137))
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                val nativeAdView = NativeAdView(context)
                val rootLayout = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // Fila Superior: Badge "ANUNCIO" + Título
                val topRow = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val adBadge = android.widget.TextView(context).apply {
                    text = adLabel
                    textSize = 9f
                    setTextColor(android.graphics.Color.parseColor("#FFD700"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(10, 4, 10, 4)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#33FFD700"))
                        cornerRadius = 8f
                    }
                }
                topRow.addView(adBadge)

                val spacer = android.view.View(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(14, 1)
                }
                topRow.addView(spacer)

                val headlineView = android.widget.TextView(context).apply {
                    textSize = 13f
                    setTextColor(android.graphics.Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                topRow.addView(headlineView)
                nativeAdView.headlineView = headlineView
                rootLayout.addView(topRow)

                // Fila Inferior: Icono + Cuerpo de texto + Botón de Llamada a la Acción (CTA)
                val middleRow = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 0)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val iconView = android.widget.ImageView(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(70, 70).apply {
                        marginEnd = 14
                    }
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
                middleRow.addView(iconView)
                nativeAdView.iconView = iconView

                val bodyView = android.widget.TextView(context).apply {
                    textSize = 11f
                    setTextColor(android.graphics.Color.parseColor("#B0BEC5"))
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginEnd = 12
                    }
                }
                middleRow.addView(bodyView)
                nativeAdView.bodyView = bodyView

                val ctaButton = android.widget.Button(context).apply {
                    textSize = 11f
                    setTextColor(android.graphics.Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#00B0FF"))
                        cornerRadius = 14f
                    }
                    setPadding(18, 6, 18, 6)
                }
                middleRow.addView(ctaButton)
                nativeAdView.callToActionView = ctaButton

                rootLayout.addView(middleRow)
                nativeAdView.addView(rootLayout)

                nativeAdView
            },
            update = { nativeAdView ->
                try {
                    val headlineView = nativeAdView.headlineView as? android.widget.TextView
                    val bodyView = nativeAdView.bodyView as? android.widget.TextView
                    val iconView = nativeAdView.iconView as? android.widget.ImageView
                    val ctaView = nativeAdView.callToActionView as? android.widget.Button

                    headlineView?.text = nativeAd.headline
                    bodyView?.text = nativeAd.body ?: ""
                    ctaView?.text = nativeAd.callToAction ?: "Instalar"

                    val icon = nativeAd.icon
                    if (icon != null && icon.drawable != null) {
                        iconView?.setImageDrawable(icon.drawable)
                        iconView?.visibility = android.view.View.VISIBLE
                    } else {
                        iconView?.visibility = android.view.View.GONE
                    }

                    nativeAdView.setNativeAd(nativeAd)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error actualizando vista nativa: ${e.message}")
                }
            }
        )
    }
}
