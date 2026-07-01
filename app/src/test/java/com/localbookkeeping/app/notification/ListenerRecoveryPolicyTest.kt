package com.localbookkeeping.app.notification

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenerRecoveryPolicyTest {
    @Test
    fun allowsFirstRequestRebindAttempt() {
        assertTrue(
            ListenerRecoveryPolicy.canAttemptRebind(
                nowMillis = 10_000L,
                lastAttemptAtMillis = 0L
            )
        )
    }

    @Test
    fun blocksRequestRebindInsideCooldown() {
        assertFalse(
            ListenerRecoveryPolicy.canAttemptRebind(
                nowMillis = 11_000L,
                lastAttemptAtMillis = 10_000L,
                cooldownMillis = 5_000L
            )
        )
    }

    @Test
    fun allowsRequestRebindAfterCooldown() {
        assertTrue(
            ListenerRecoveryPolicy.canAttemptRebind(
                nowMillis = 15_000L,
                lastAttemptAtMillis = 10_000L,
                cooldownMillis = 5_000L
            )
        )
    }

    @Test
    fun marksOnlyMyPackageReplacedForListenerRecheck() {
        assertTrue(ListenerRecoveryPolicy.shouldMarkPackageRecheck(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertFalse(ListenerRecoveryPolicy.shouldMarkPackageRecheck(Intent.ACTION_BOOT_COMPLETED))
    }
}
