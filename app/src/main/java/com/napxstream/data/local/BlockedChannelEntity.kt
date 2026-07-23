package com.napxstream.data.local

import androidx.room.Entity

/**
 * Yönetim panelinden belirli kanalların/kategorilerin gizlenmesi için ("buket kanal
 * şifreleme" — kanal paketi kısıtlaması). Ebeveyn kilidinden farklıdır: kategori adı
 * anahtar kelimesine değil, yöneticinin elle seçtiği tekil kanal/kategori ID'lerine dayanır.
 * targetType: "category" | "channel"
 */
@Entity(tableName = "blocked_channels", primaryKeys = ["targetType", "targetId"])
data class BlockedChannelEntity(
    val targetType: String,
    val targetId: String,
    val label: String,
    val blockedAt: Long = System.currentTimeMillis()
)
