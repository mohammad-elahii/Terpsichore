package com.Mohammad.Elahi.terpsichore.core.sources

enum class Quality(val label: String, val tier: QualityTier) {
    UNKNOWN("", QualityTier.UNKNOWN),
    MP3_128("MP3 128", QualityTier.LOSSY_LOW),
    MP3_320("MP3 320", QualityTier.LOSSY_HIGH),
    FLAC("FLAC", QualityTier.LOSSLESS),
}

enum class QualityTier {
    UNKNOWN,
    LOSSY_LOW,
    LOSSY_HIGH,
    LOSSLESS,
}

data class QualityChip(
    val label: String,
    val tier: QualityTier,
    val visible: Boolean,
)

fun Quality.toChip(): QualityChip =
    QualityChip(
        label = label,
        tier = tier,
        visible = this != Quality.UNKNOWN,
    )
