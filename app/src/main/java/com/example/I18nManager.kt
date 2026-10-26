package com.example

import java.util.Locale

/**
 * Idiomas soportados por Globoup con soporte de dirección de lectura (LTR / RTL)
 * y detección automática del sistema operativo del dispositivo.
 */
enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    AUTO("auto", "Auto (Sistema)", false),
    ENGLISH("en", "English", false),
    SPANISH("es", "Español", false),
    PORTUGUESE("pt", "Português", false),
    ARABIC("ar", "العربية (RTL)", true)
}

/**
 * Diccionario de cadenas traducidas para la interfaz y juego Globoup.
 */
data class GameStrings(
    val language: AppLanguage,
    val isRtl: Boolean,

    // Cadenas solicitadas específicamente
    val points: String,
    val record: String,
    val electricStrike: String,
    val reviveWithShield: String,
    val reviveOffline: String,
    val playAgain: String,
    val advertisement: String,
    val tapToStart: String,
    val watchAd: String,
    val superShield: String,
    val almostBeatRecord: String,
    val savePointsSuperShield: String,
    val superShieldActivated: String,
    val countdownGetReady: String,
    val watchAdForReward: String = "Ver anuncio para recibir recompensa",
    val rewardEarnedTitle: String = "¡2 Escudo con imán de estrellas obtenido!",
    val useShield: String = "Usar Escudo",
    val myShields: String = "Escudos Imán",

    // Interfaz principal y juego
    val gameTitle: String,
    val tagline: String,
    val takeOff: String,
    val swipePrompt: String,
    val stormHitSubtitle: String,
    val shieldRevivedFloating: String,

    // Ajustes y estadísticas
    val settingsTitle: String,
    val soundEffects: String,
    val hapticVibration: String,
    val localStats: String,
    val statsHighScore: String,
    val statsStars: String,
    val statsGames: String,
    val resetRecord: String,
    val howToPlay: String,
    val privacySecurity: String,
    val resume: String,
    val languageLabel: String,
    val autoLanguage: String,

    // Diálogo "Cómo Jugar"
    val howToPlayTitle: String,
    val balloonControlGuide: String,
    val goldenStarsGuide: String,
    val stormCloudsGuide: String,
    val close: String,

    // Diálogo "Seguridad y Privacidad"
    val privacyDialogTitle: String,
    val privacyComplianceTitle: String,
    val privacyDataSafety: String,
    val privacyLocalStorage: String,
    val privacyPermissions: String,
    val privacyNetworkSecurity: String,
    val privacyBundleReady: String,
    val understood: String
)

object I18nManager {

    /**
     * Diccionario en Español (es)
     */
    val SPANISH = GameStrings(
        language = AppLanguage.SPANISH,
        isRtl = false,
        points = "PUNTOS",
        record = "RÉCORD",
        electricStrike = "¡Rayo Eléctrico!",
        reviveWithShield = "Revivir con Escudo 🛡️ (Ver Anuncio)",
        reviveOffline = "Revivir (Modo Sin Conexión)",
        playAgain = "Jugar de Nuevo",
        advertisement = "PUBLICIDAD",
        tapToStart = "Toca para volar",
        watchAd = "Ver Anuncio",
        superShield = "Revivir con Modo Fantasma",
        almostBeatRecord = "¡CASI SUPERAS TU RÉCORD! (Ver Anuncio)",
        savePointsSuperShield = "¡Salva tus %d Puntos + Modo Fantasma! (Ver Anuncio)",
        superShieldActivated = "⚡ MODO FANTASMA 2X ⚡",
        countdownGetReady = "¡PREPÁRATE!",

        gameTitle = "GloboUp",
        tagline = "¡Esquiva las tormentas con rayos y recolecta todas las estrellas doradas!",
        takeOff = "¡Despegar!",
        swipePrompt = "Desliza el dedo en la pantalla para mover el globo",
        stormHitSubtitle = "Tu globo impactó con una tormenta",
        shieldRevivedFloating = "🛡️ ESCUDO",

        settingsTitle = "Ajustes de GloboUp",
        soundEffects = "Efectos de Sonido",
        hapticVibration = "Vibración Háptica",
        localStats = "Estadísticas Locales",
        statsHighScore = "Puntuación Récord: ",
        statsStars = "Estrellas Recogidas: ",
        statsGames = "Partidas Totales: ",
        resetRecord = "Restablecer Récord",
        howToPlay = "Cómo Jugar",
        privacySecurity = "Seguridad Google Play y Privacidad",
        resume = "Continuar",
        languageLabel = "Idioma",
        autoLanguage = "Automático (Sistema)",

        howToPlayTitle = "Cómo Jugar a GloboUp",
        balloonControlGuide = "🎈 Control del Globo:\nArrastra o pulsa en la pantalla para mover el globo aerostático a la izquierda o derecha.",
        goldenStarsGuide = "⭐ Estrellas Doradas:\nRecoge todas las estrellas brillantes para sumar puntos y batir tu récord personal.",
        stormCloudsGuide = "⛈️ Nubes de Tormenta con Rayos:\n¡Cuidado! No permitas que el globo toque las nubes negras ni los rayos eléctricos o el vuelo terminará.",
        close = "Cerrar",

        privacyDialogTitle = "Seguridad de Google Play",
        privacyComplianceTitle = "Declaración de Cumplimiento de Políticas (Google Play Store):",
        privacyDataSafety = "✓ Privacidad y Datos (Data Safety):\nEsta aplicación no recopila, no comparte ni transmite ningún dato personal, ubicación, ID de dispositivo ni información sensible del usuario a servidores externos.",
        privacyLocalStorage = "✓ Almacenamiento 100% Local:\nToda la información del juego (récord de puntos, estrellas y opciones de audio) se guarda exclusivamente de forma local y privada en tu dispositivo (SharedPreferences seguras).",
        privacyPermissions = "✓ Principio de Menor Privilegio (Permisos Mínimos):\nNo se solicitan permisos peligrosos ni invasivos. Únicamente se declara el permiso normal de VIBRACIÓN para respuesta háptica al jugar.",
        privacyNetworkSecurity = "✓ Seguridad de Red:\nandroid:usesCleartextTraffic está deshabilitado por política de seguridad, impidiendo comunicaciones inseguras no encriptadas.",
        privacyBundleReady = "✓ Compatibilidad para APK y AAB (Android App Bundle):\nEl código fuente está optimizado con Target SDK 36, listo para ser firmado y empaquetado en formato APK o Android App Bundle (.aab) para su publicación en Google Play Console.",
        understood = "Entendido"
    )

    /**
     * Diccionario en Inglés (en) - Fallback Default para idiomas no soportados
     */
    val ENGLISH = GameStrings(
        language = AppLanguage.ENGLISH,
        isRtl = false,
        points = "SCORE",
        record = "HIGH SCORE",
        electricStrike = "Electric Strike!",
        reviveWithShield = "Revive with Shield 🛡️ (Watch Ad)",
        reviveOffline = "Revive (Offline Mode)",
        playAgain = "Play Again",
        advertisement = "ADVERTISEMENT",
        tapToStart = "Tap to fly",
        watchAd = "Watch Ad",
        superShield = "Revive with Ghost Mode",
        almostBeatRecord = "ALMOST BEAT YOUR RECORD! (Watch Ad)",
        savePointsSuperShield = "Save your %d Points + Ghost Mode! (Watch Ad)",
        superShieldActivated = "⚡ GHOST MODE 2X ⚡",
        countdownGetReady = "GET READY!",

        gameTitle = "GloboUp",
        tagline = "Dodge lightning storms and collect all golden stars!",
        takeOff = "Take Off!",
        swipePrompt = "Swipe on screen to steer the hot air balloon",
        stormHitSubtitle = "Your balloon was struck by a thunderstorm",
        shieldRevivedFloating = "🛡️ SHIELD",

        settingsTitle = "GloboUp Settings",
        soundEffects = "Sound Effects",
        hapticVibration = "Haptic Vibration",
        localStats = "Local Statistics",
        statsHighScore = "High Score: ",
        statsStars = "Stars Collected: ",
        statsGames = "Total Games: ",
        resetRecord = "Reset High Score",
        howToPlay = "How to Play",
        privacySecurity = "Google Play Security & Privacy",
        resume = "Resume",
        languageLabel = "Language",
        autoLanguage = "Automatic (System)",

        howToPlayTitle = "How to Play GloboUp",
        balloonControlGuide = "🎈 Balloon Control:\nDrag or tap on the screen to steer the hot air balloon left or right.",
        goldenStarsGuide = "⭐ Golden Stars:\nCollect all shiny golden stars to score points and beat your personal record.",
        stormCloudsGuide = "⛈️ Lightning Storm Clouds:\nWatch out! Don't let your balloon touch dark clouds or electric strikes or the flight will end.",
        close = "Close",

        privacyDialogTitle = "Google Play Security",
        privacyComplianceTitle = "Google Play Store Policy Compliance Statement:",
        privacyDataSafety = "✓ Privacy & Data Safety:\nThis app does not collect, share, or transmit any personal data, location, or sensitive information to external servers.",
        privacyLocalStorage = "✓ 100% Local Storage:\nAll game data (high score, collected stars, and audio options) is stored purely locally and privately on your device.",
        privacyPermissions = "✓ Least-Privilege Principle:\nNo dangerous permissions requested. Only normal VIBRATE permission is used for haptic feedback.",
        privacyNetworkSecurity = "✓ Network Security:\nandroid:usesCleartextTraffic is disabled by security policy, preventing unencrypted traffic.",
        privacyBundleReady = "✓ APK and AAB Compatibility:\nSource code optimized for Target SDK 36, ready for signing and deployment as APK or Android App Bundle (.aab).",
        understood = "Understood"
    )

    /**
     * Diccionario en Portugués (pt)
     */
    val PORTUGUESE = GameStrings(
        language = AppLanguage.PORTUGUESE,
        isRtl = false,
        points = "PONTOS",
        record = "RECORDE",
        electricStrike = "Raio Elétrico!",
        reviveWithShield = "Renascer com Escudo 🛡️ (Assistir Anúncio)",
        reviveOffline = "Renascer (Modo Offline)",
        playAgain = "Jogar Novamente",
        advertisement = "ANÚNCIO",
        tapToStart = "Toque para voar",
        watchAd = "Ver Anúncio",
        superShield = "Renascer com Modo Fantasma",
        almostBeatRecord = "QUASE BATEU SEU RECORDE! (Ver Anúncio)",
        savePointsSuperShield = "Salve seus %d Pontos + Modo Fantasma! (Ver Anúncio)",
        superShieldActivated = "⚡ MODO FANTASMA 2X ⚡",
        countdownGetReady = "PREPARE-SE!",

        gameTitle = "GloboUp",
        tagline = "Desvie das tempestades com raios e colete todas as estrelas douradas!",
        takeOff = "Decolar!",
        swipePrompt = "Deslize o dedo na tela para mover o balão",
        stormHitSubtitle = "Seu balão colidiu com uma tempestade",
        shieldRevivedFloating = "🛡️ ESCUDO",

        settingsTitle = "Configurações do GloboUp",
        soundEffects = "Efeitos Sonoros",
        hapticVibration = "Vibração Háptica",
        localStats = "Estatísticas Locais",
        statsHighScore = "Pontuação Recorde: ",
        statsStars = "Estrelas Coletadas: ",
        statsGames = "Total de Partidas: ",
        resetRecord = "Redefinir Recorde",
        howToPlay = "Como Jogar",
        privacySecurity = "Segurança do Google Play e Privacidade",
        resume = "Continuar",
        languageLabel = "Idioma",
        autoLanguage = "Automático (Sistema)",

        howToPlayTitle = "Como Jogar GloboUp",
        balloonControlGuide = "🎈 Controle do Balão:\nArraste ou toque na tela para guiar o balão de ar quente para a esquerda ou direita.",
        goldenStarsGuide = "⭐ Estrelas Douradas:\nColete todas as estrelas brilhantes para somar pontos e quebrar seu recorde pessoal.",
        stormCloudsGuide = "⛈️ Nuvens de Tempestade com Raios:\nCuidado! Não permita que o balão toque as nuvens escuras nem os raios elétricos.",
        close = "Fechar",

        privacyDialogTitle = "Segurança do Google Play",
        privacyComplianceTitle = "Declaração de Conformidade com Políticas (Google Play Store):",
        privacyDataSafety = "✓ Privacidade e Dados (Data Safety):\nEste aplicativo não coleta, não compartilha nem transmite dados pessoais ou sensíveis a servidores externos.",
        privacyLocalStorage = "✓ Armazenamento 100% Local:\nTodas as informações do jogo (recorde, estrelas e opções de áudio) são salvas exclusivamente de forma local e privada no seu dispositivo.",
        privacyPermissions = "✓ Princípio do Menor Privilégio:\nNão são solicitadas permissões perigosas. Apenas a permissão normal de VIBRAÇÃO para resposta tátil.",
        privacyNetworkSecurity = "✓ Segurança de Rede:\nandroid:usesCleartextTraffic está desativado por política de segurança.",
        privacyBundleReady = "✓ Compatibilidade para APK e AAB:\nO código fonte está otimizado com Target SDK 36, pronto para publicação no Google Play Console.",
        understood = "Entendido"
    )

    /**
     * Diccionario en Árabe (ar) - Con soporte de dirección Right-to-Left (RTL)
     */
    val ARABIC = GameStrings(
        language = AppLanguage.ARABIC,
        isRtl = true,
        points = "نقاط",
        record = "رقم قياسي",
        electricStrike = "صاعقة كهربائية!",
        reviveWithShield = "إحياء بالدرع 🛡️ (مشاهدة الإعلان)",
        reviveOffline = "إحياء (وضع دون اتصال)",
        playAgain = "اللعب مجددًا",
        advertisement = "إعلان",
        tapToStart = "انقر للطيران",
        watchAd = "مشاهدة الإعلان",
        superShield = "إحياء بوضع الشبح",
        almostBeatRecord = "كدت تحطم رقمك القياسي! (مشاهدة الإعلان)",
        savePointsSuperShield = "أنقذ %d نقطة + وضع الشبح! (مشاهدة الإعلان)",
        superShieldActivated = "⚡ وضع الشبح 2X ⚡",
        countdownGetReady = "استعد!",

        gameTitle = "غلوبوب",
        tagline = "تفادَ العواصف الرعدية واجمع كل النجوم الذهبية!",
        takeOff = "انطلق!",
        swipePrompt = "اسحب على الشاشة لتوجيه المنطاد",
        stormHitSubtitle = "اصطدم منطادك بعاصفة رعدية صاعقة",
        shieldRevivedFloating = "🛡️ درع",

        settingsTitle = "إعدادات غلوبوب",
        soundEffects = "المؤثرات الصوتية",
        hapticVibration = "الاهتزاز اللمسي",
        localStats = "الإحصائيات المحلية",
        statsHighScore = "الرقم القياسي: ",
        statsStars = "النجوم المجمعة: ",
        statsGames = "إجمالي المباريات: ",
        resetRecord = "إعادة تعيين الرقم القياسي",
        howToPlay = "طريقة اللعب",
        privacySecurity = "أمان جوجل بلاي والخصوصية",
        resume = "متابعة",
        languageLabel = "اللغة",
        autoLanguage = "تلقائي (لغة النظام)",

        howToPlayTitle = "طريقة لعب غلوبوب",
        balloonControlGuide = "🎈 التحكم في المنطاد:\nاسحب أو انقر على الشاشة لتوجيه منطاد الهواء الساخن يمينًا أو يسارًا.",
        goldenStarsGuide = "⭐ النجوم الذهبية:\nاجمع كل النجوم اللامعة لكسب النقاط وتحطيم رقمك القياسي.",
        stormCloudsGuide = "⛈️ سحب العواصف الرعدية:\nاحذر! لا تدع المنطاد يلمس السحب السوداء أو الصواعق الكهربائية لتجنب انتهاء الرحلة.",
        close = "إغلاق",

        privacyDialogTitle = "أمان جوجل بلاي",
        privacyComplianceTitle = "إقرار الالتزام بسياسات متجر Google Play:",
        privacyDataSafety = "✓ خصوصية وأمان البيانات:\nهذا التطبيق لا يجمع ولا يشارك أي بيانات شخصية أو موقع أو معلومات حساسة مع أي خوادم خارجية.",
        privacyLocalStorage = "✓ تخزين محلي بالكامل:\nجميع بيانات اللعبة (الرقم القياسي، النجوم، وإعدادات الصوت) تُحفظ محليًا داخل جهازك فقط.",
        privacyPermissions = "✓ أدنى الأذونات المطلوبة:\nلا يطلب التطبيق أي أذونات خطيرة. فقط إذن الاهتزاز للملاحظات اللمسية أثناء اللعب.",
        privacyNetworkSecurity = "✓ أمان الشبكة:\nتم تعطيل الاتصالات غير المشفرة التزاماً بسياسات الأمان.",
        privacyBundleReady = "✓ جاهز للنشر كـ APK و AAB:\nالكود البرمجي مهيأ لأحدث متطلبات Target SDK 36 وجاهز للنشر في Google Play.",
        understood = "حسناً"
    )

    /**
     * Detección automática del idioma del dispositivo.
     * Lee Locale.getDefault().language:
     * - Si es "es" -> Español
     * - Si es "pt" -> Portugués
     * - Si es "ar" -> Árabe (con soporte RTL)
     * - Si es "en" -> Inglés
     * - Cualquier otro idioma no soportado (de, fr, ru, zh, ja, etc.) -> Fallback en Inglés (en)
     */
    fun detectSystemLanguage(): AppLanguage {
        val systemLang = Locale.getDefault().language.lowercase()
        return when {
            systemLang.startsWith("es") -> AppLanguage.SPANISH
            systemLang.startsWith("pt") -> AppLanguage.PORTUGUESE
            systemLang.startsWith("ar") -> AppLanguage.ARABIC
            systemLang.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.ENGLISH // Fallback predeterminado según requerimiento
        }
    }

    /**
     * Resuelve los textos correspondientes a partir del código de configuración o detección.
     */
    fun getStringsForLanguage(language: AppLanguage): GameStrings {
        return when (language) {
            AppLanguage.SPANISH -> SPANISH
            AppLanguage.ENGLISH -> ENGLISH
            AppLanguage.PORTUGUESE -> PORTUGUESE
            AppLanguage.ARABIC -> ARABIC
            AppLanguage.AUTO -> getStringsForLanguage(detectSystemLanguage())
        }
    }

    /**
     * Resuelve la configuración guardada por el usuario (auto, en, es, pt, ar).
     */
    fun resolveLanguage(settingCode: String): AppLanguage {
        return when (settingCode.lowercase()) {
            "en" -> AppLanguage.ENGLISH
            "es" -> AppLanguage.SPANISH
            "pt" -> AppLanguage.PORTUGUESE
            "ar" -> AppLanguage.ARABIC
            "auto" -> AppLanguage.AUTO
            else -> AppLanguage.AUTO
        }
    }
}
