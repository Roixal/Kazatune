package com.kazatune.updater

import com.kazatune.updater.util.SemanticVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun testParseStandardVersion() {
        val v = SemanticVersion.parse("v1.0.1")
        assertNotNull(v)
        assertEquals(1, v!!.major)
        assertEquals(0, v.minor)
        assertEquals(1, v.patch)
        assertEquals("1.0.1", v.toString())
    }

    @Test
    fun testParseReleaseTitleWithPrefix() {
        val v = SemanticVersion.parse("SKIM MUSIC V1.0.0")
        assertNotNull(v)
        assertEquals(1, v!!.major)
        assertEquals(0, v.minor)
        assertEquals(0, v.patch)
        assertEquals("1.0.0", v.toString())
    }

    @Test
    fun testVersionComparison() {
        val v100 = SemanticVersion.parse("1.0.0")!!
        val v101 = SemanticVersion.parse("1.0.1")!!
        val v110 = SemanticVersion.parse("1.1.0")!!
        val v200 = SemanticVersion.parse("2.0.0")!!
        val v199 = SemanticVersion.parse("1.9.9")!!

        assertTrue(v101.isNewerThan(v100))
        assertTrue(v110.isNewerThan(v101))
        assertTrue(v200.isNewerThan(v199))
        assertFalse(v100.isNewerThan(v101))
        assertFalse(v100.isNewerThan(v100))
    }

    @Test
    fun testPrereleaseComparison() {
        val stable = SemanticVersion.parse("1.0.1")!!
        val beta = SemanticVersion.parse("1.0.1-beta1")!!

        assertTrue(stable.isNewerThan(beta))
    }
}
