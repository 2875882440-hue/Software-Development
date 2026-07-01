package com.localbookkeeping.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

object AppVisibilityTracker : Application.ActivityLifecycleCallbacks {
    private var startedCount: Int = 0

    val isInBackground: Boolean
        get() = startedCount <= 0

    override fun onActivityStarted(activity: Activity) {
        startedCount += 1
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
