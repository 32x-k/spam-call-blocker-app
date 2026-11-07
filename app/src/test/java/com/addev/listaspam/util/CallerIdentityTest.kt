package com.addev.listaspam.util

import android.telecom.TelecomManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerIdentityTest {

    private val allowed = TelecomManager.PRESENTATION_ALLOWED
    private val restricted = TelecomManager.PRESENTATION_RESTRICTED
    private val unknown = TelecomManager.PRESENTATION_UNKNOWN
    private val unavailable = TelecomManager.PRESENTATION_UNAVAILABLE

    @Test
    fun realNumbersAreNumbers() {
        assertEquals(CallerKind.NUMBER, CallerIdentity.classify("+817084175626", allowed))
        assertEquals(CallerKind.NUMBER, CallerIdentity.classify("03-1234-5678", allowed))
        assertEquals(CallerKind.NUMBER, CallerIdentity.classify(" 07084175626 ", allowed))
    }

    @Test
    fun aUsableNumberWinsOverThePresentation() {
        // Some carriers still deliver the number with a restricted presentation: identifying it
        // is better than blocking it as hidden.
        assertEquals(CallerKind.NUMBER, CallerIdentity.classify("+817084175626", restricted))
        assertEquals(CallerKind.NUMBER, CallerIdentity.classify("+817084175626", unknown))
    }

    @Test
    fun withheldNumbersAreRestricted() {
        assertEquals(CallerKind.RESTRICTED, CallerIdentity.classify(null, restricted))
        assertEquals(CallerKind.RESTRICTED, CallerIdentity.classify("", restricted))
        assertEquals(CallerKind.RESTRICTED, CallerIdentity.classify("   ", restricted))
    }

    @Test
    fun unknownPresentationIsUnknown() {
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify(null, unknown))
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("", unknown))
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify(null, unavailable))
    }

    @Test
    fun blankNumberWithAllowedPresentationIsUnknown() {
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify(null, allowed))
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("", allowed))
    }

    @Test
    fun placeholderStringsAreUnknownRegardlessOfCase() {
        listOf("Unknown", "UNKNOWN", " unknown ", "Unknown number", "Private", "anonymous")
            .forEach { value ->
                assertEquals(value, CallerKind.UNKNOWN, CallerIdentity.classify(value, allowed))
            }
    }

    @Test
    fun japanesePlaceholdersAreUnknown() {
        listOf("不明", "表示圏外", "圏外", "非通知", "不明な番号").forEach { value ->
            assertEquals(value, CallerKind.UNKNOWN, CallerIdentity.classify(value, allowed))
        }
    }

    @Test
    fun numericPlaceholdersAreUnknown() {
        listOf("-1", "-2", "-3", "null", "none").forEach { value ->
            assertEquals(value, CallerKind.UNKNOWN, CallerIdentity.classify(value, allowed))
        }
        // All-zero numbers are placeholders too
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("0", allowed))
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("000000000", allowed))
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("+0", allowed))
    }

    @Test
    fun valuesWithoutDigitsAreUnknown() {
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("***", allowed))
        assertEquals(CallerKind.UNKNOWN, CallerIdentity.classify("+", allowed))
    }

    @Test
    fun placeholderAndUsableHelpersAgreeWithClassify() {
        assertTrue(CallerIdentity.isPlaceholder("Unknown"))
        assertTrue(CallerIdentity.isPlaceholder("-1"))
        assertFalse(CallerIdentity.isPlaceholder("+817084175626"))
        assertFalse(CallerIdentity.isPlaceholder(null))

        assertTrue(CallerIdentity.isUsableNumber("+817084175626"))
        assertFalse(CallerIdentity.isUsableNumber("Unknown"))
        assertFalse(CallerIdentity.isUsableNumber(null))
    }
}
