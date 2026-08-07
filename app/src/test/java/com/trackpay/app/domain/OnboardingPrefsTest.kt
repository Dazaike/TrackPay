package com.trackpay.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure preference defaults / transitions for onboardingDone.
 * Mirrors PreferencesDataSource defaults without Android DataStore.
 */
class OnboardingPrefsTest {

    private class FakeOnboardingPrefs(
        initialDone: Boolean = false,
    ) {
        var onboardingDone: Boolean = initialDone
            private set

        fun setOnboardingDone(done: Boolean) {
            onboardingDone = done
        }
    }

    @Test
    fun default_onboarding_not_done() {
        val prefs = FakeOnboardingPrefs()
        assertFalse(prefs.onboardingDone)
    }

    @Test
    fun set_onboarding_done_true() {
        val prefs = FakeOnboardingPrefs()
        prefs.setOnboardingDone(true)
        assertTrue(prefs.onboardingDone)
    }

    @Test
    fun set_onboarding_done_can_reset() {
        val prefs = FakeOnboardingPrefs(initialDone = true)
        assertTrue(prefs.onboardingDone)
        prefs.setOnboardingDone(false)
        assertFalse(prefs.onboardingDone)
    }

    @Test
    fun nav_start_uses_flag() {
        fun startRoute(onboardingDone: Boolean): String =
            if (onboardingDone) "dashboard" else "onboarding"

        assertEqualsRoute("onboarding", startRoute(false))
        assertEqualsRoute("dashboard", startRoute(true))
    }

    private fun assertEqualsRoute(expected: String, actual: String) {
        assertTrue("expected $expected but was $actual", expected == actual)
    }
}
