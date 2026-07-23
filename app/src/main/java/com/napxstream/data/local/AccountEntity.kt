package com.napxstream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kullanıcının cihazda kayıtlı birden fazla IPTV hesabını (Xtream veya M3U) tutar.
 * Yerel yönetim panelinden (Ayarlar > Uzaktan Yönetim) eklenip düzenlenebilir/silinebilir.
 * Aynı anda yalnızca bir hesap `isActive = true` olur; uygulama o hesapla çalışır.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Kullanıcının hesaba verdiği görünen ad, ör. "Ev Aboneliği" */
    val label: String,
    /** "xtream" | "m3u" */
    val type: String,
    // Xtream alanları
    val host: String? = null,
    val port: String? = null,
    val username: String? = null,
    val password: String? = null,
    val useHttps: Boolean = false,
    // M3U alanları
    val m3uUrl: String? = null,
    val m3uEpgUrl: String? = null,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
