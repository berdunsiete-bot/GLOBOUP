package com.aiastudio.globoup.arcade

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.example.AdMobManager

class AdsManager private constructor(private val context: Context) {

    fun initializeSdk() {
        try {
            // Sincroniza con el gestor centralizado de anuncios de Globoup
            AdMobManager.getInstance(context).initializeSdk()
        } catch (_: Exception) {
            // Manejo silencioso y seguro para estabilidad offline
        }
    }

    companion object {
        @Volatile
        private var instance: AdsManager? = null

        fun getInstance(context: Context): AdsManager {
            return instance ?: synchronized(this) {
                instance ?: AdsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
