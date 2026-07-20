package com.napxstream.util

import com.napxstream.data.model.Category
import com.napxstream.data.model.LiveStream

/**
 * Yetişkin/hassas kategorileri varsayılan olarak gizler. Kullanıcı Ayarlar'dan bir PIN
 * belirleyip "Yetişkin içerikleri göster" seçeneğini açtığında, PIN doğrulanana kadar
 * içerikler görünmez. Kilit açma durumu SADECE bellekte tutulur (oturum bazlı) — uygulama
 * her yeniden başlatıldığında yetişkin içerik tekrar gizlenir. Bu tasarım bilinçlidir:
 * paylaşılan/aile cihazlarında varsayılan olarak en güvenli durumu sağlar.
 */
object ParentalControlManager {

    /** Kategori adında bu anahtar kelimelerden biri geçiyorsa "yetişkin" kabul edilir. */
    private val ADULT_KEYWORDS = listOf(
        "adult", "xxx", "+18", "18+", "erotik", "erotic", "yetişkin", "yetiskin", "porn"
    )

    @Volatile
    var unlockedThisSession: Boolean = false
        private set

    fun unlock() {
        unlockedThisSession = true
    }

    fun lock() {
        unlockedThisSession = false
    }

    fun isAdultCategory(category: Category): Boolean {
        val name = category.categoryName.lowercase()
        return ADULT_KEYWORDS.any { name.contains(it) }
    }

    fun isAdultStream(stream: LiveStream): Boolean = stream.isAdult == "1"

    /** Kilit açık değilse yetişkin kategorileri listeden çıkarır. */
    fun filterCategories(categories: List<Category>): List<Category> {
        if (unlockedThisSession) return categories
        return categories.filterNot { isAdultCategory(it) }
    }

    /** Kilit açık değilse is_adult=1 olan yayınları listeden çıkarır. */
    fun filterLiveStreams(streams: List<LiveStream>): List<LiveStream> {
        if (unlockedThisSession) return streams
        return streams.filterNot { isAdultStream(it) }
    }
}
