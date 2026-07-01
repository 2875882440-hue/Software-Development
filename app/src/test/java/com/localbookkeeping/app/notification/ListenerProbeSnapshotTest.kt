package com.localbookkeeping.app.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenerProbeSnapshotTest {
    @Test
    fun latestProbeFailedWhenFailureIsNewerThanSuccess() {
        assertTrue(ListenerProbeSnapshot(3_000L, 1_000L, 2_000L).latestProbeFailed)
    }

    @Test
    fun latestProbeIsHealthyWhenSuccessIsNewest() {
        assertFalse(ListenerProbeSnapshot(3_000L, 2_000L, 1_000L).latestProbeFailed)
    }
}
