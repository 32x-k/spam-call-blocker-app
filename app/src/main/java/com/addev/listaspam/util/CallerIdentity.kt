package com.addev.listaspam.util

import android.telecom.TelecomManager

/**
 * What an incoming call actually tells us about its caller.
 */
enum class CallerKind {
    /** A usable phone number was received, so the usual spam checks can be applied to it. */
    NUMBER,

    /** The caller withheld the number (private call, "non-notification" / 非通知). */
    RESTRICTED,

    /**
     * No usable number was received: the network reported it as unknown or unavailable, or the
     * carrier/device sent a placeholder such as "Unknown", "不明" or "-1" instead of a number.
     */
    UNKNOWN
}

/**
 * Tells apart calls that carry a dialable phone number from calls that carry none at all.
 *
 * Some carriers and devices do not deliver an empty number for such calls: they deliver a
 * placeholder string ("Unknown", "不明", "-1", "-2"...) or report
 * [TelecomManager.PRESENTATION_UNKNOWN] / [TelecomManager.PRESENTATION_RESTRICTED] instead of
 * [TelecomManager.PRESENTATION_ALLOWED]. Classifying them in one place lets the blocking code treat
 * all of these cases the same way.
 *
 * This object has no Android runtime dependencies (only compile-time constants), so it can be
 * unit tested on the JVM.
 */
object CallerIdentity {

    /**
     * Placeholders that carriers and devices send in place of a number. Compared after
     * trimming and lower-casing, so "Unknown", "UNKNOWN" and " unknown " all match.
     */
    private val PLACEHOLDER_VALUES = setOf(
        // English
        "unknown", "unknown number", "private", "private number", "anonymous",
        "restricted", "unavailable", "withheld", "blocked", "no caller id", "no callerid",
        "caller id blocked", "null", "none", "n/a", "na",
        // Japanese
        "不明", "不明な番号", "不明な電話番号", "表示圏外", "圏外", "非通知", "通知不可",
        "番号非通知", "非表示", "非公開",
        // Numeric placeholders
        "-1", "-2", "-3"
    )

    /**
     * Classifies an incoming call.
     *
     * A usable number always wins over the presentation: when a dialable number is delivered it
     * is checked like any other number (whitelist, contacts, spam databases), even if the network
     * flagged the presentation as restricted, because blocking a caller who can actually be
     * identified would be wrong.
     *
     * @param number The number as delivered by Telecom (may be null, blank or a placeholder).
     * @param presentation The number presentation, one of the
     *        `TelecomManager.PRESENTATION_*` values. Defaults to
     *        [TelecomManager.PRESENTATION_ALLOWED] when unknown.
     * @return [CallerKind.NUMBER] when [number] is dialable, [CallerKind.RESTRICTED] for a
     *         withheld number and [CallerKind.UNKNOWN] otherwise.
     */
    fun classify(
        number: String?,
        presentation: Int = TelecomManager.PRESENTATION_ALLOWED
    ): CallerKind {
        if (isUsableNumber(number)) return CallerKind.NUMBER

        return when (presentation) {
            TelecomManager.PRESENTATION_RESTRICTED -> CallerKind.RESTRICTED
            TelecomManager.PRESENTATION_UNKNOWN,
            TelecomManager.PRESENTATION_UNAVAILABLE,
            TelecomManager.PRESENTATION_PAYPHONE -> CallerKind.UNKNOWN
            // An allowed presentation without a number: the carrier gave us nothing usable.
            else -> CallerKind.UNKNOWN
        }
    }

    /**
     * Returns true when [number] is a real number that can be dialed and looked up: it contains
     * at least one non-zero digit and is not one of the placeholders listed above.
     */
    fun isUsableNumber(number: String?): Boolean {
        val normalized = normalize(number)
        if (normalized.isEmpty()) return false
        if (normalized in PLACEHOLDER_VALUES) return false

        val digits = normalized.filter { it.isDigit() }
        // Numbers made only of zeros ("000", "00000") are placeholders as well.
        return digits.any { it != '0' }
    }

    /** True when [number] is a placeholder value rather than a real number. */
    fun isPlaceholder(number: String?): Boolean {
        val normalized = normalize(number)
        return normalized.isNotEmpty() && normalized in PLACEHOLDER_VALUES
    }

    private fun normalize(number: String?): String =
        number.orEmpty().trim().trim('"', '\'').trim().lowercase()
}
