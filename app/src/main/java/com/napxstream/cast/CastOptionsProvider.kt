package com.napxstream.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Chromecast (Google Cast) yapılandırması.
 * Varsayılan medya alıcısını (Default Media Receiver) kullanır; bu, standart
 * HLS/MP4 içerikleri herhangi bir özel alıcı uygulaması geliştirmeden Chromecast'e
 * yayınlayabilmemizi sağlar. Manifest'te meta-data ile referans verilir.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
