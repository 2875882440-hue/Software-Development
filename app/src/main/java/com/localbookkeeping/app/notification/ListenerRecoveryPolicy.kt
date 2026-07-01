package com.localbookkeeping.app.notification

object ListenerRecoveryPolicy {
    const val REBIND_COOLDOWN_MILLIS = 10L * 60L * 1000L

    fun canAttemptRebind(
        nowMillis: Long,
        lastAttemptAtMillis: Long,
        cooldownMillis: Long = REBIND_COOLDOWN_MILLIS
    ): Boolean =
        lastAttemptAtMillis <= 0L || nowMillis - lastAttemptAtMillis >= cooldownMillis

    fun shouldMarkPackageRecheck(intentAction: String?): Boolean =
        intentAction == android.content.Intent.ACTION_MY_PACKAGE_REPLACED
}
