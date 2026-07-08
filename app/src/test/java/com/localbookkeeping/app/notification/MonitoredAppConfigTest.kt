package com.localbookkeeping.app.notification

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoredAppConfigTest {
    @Test
    fun defaultsEnableWechatAndAlipay() {
        assertEquals(
            setOf(
                MonitoredAppConfig.WECHAT_PACKAGE,
                MonitoredAppConfig.ALIPAY_PACKAGE
            ),
            MonitoredAppConfig.defaultEnabledPackages()
        )
    }

    @Test
    fun canEnableAndDisableAdditionalApp() {
        val withUnionPay = MonitoredAppConfig.toggleEnabled(
            current = MonitoredAppConfig.defaultEnabledPackages(),
            packageName = "com.unionpay",
            enabled = true
        )

        assertTrue("com.unionpay" in withUnionPay)
        assertEquals(3, withUnionPay.size)

        val disabled = MonitoredAppConfig.toggleEnabled(
            current = withUnionPay,
            packageName = "com.unionpay",
            enabled = false
        )

        assertFalse("com.unionpay" in disabled)
        assertEquals(MonitoredAppConfig.defaultEnabledPackages(), disabled)
    }

    @Test
    fun recommendsKnownPaymentAndBankApps() {
        assertTrue(MonitoredAppConfig.isRecommendedApp("com.unionpay", "云闪付"))
        assertTrue(MonitoredAppConfig.isRecommendedApp("com.jingdong.app.mall", "京东"))
        assertTrue(MonitoredAppConfig.isRecommendedApp("com.example.bank", "中国建设银行"))
        assertTrue(MonitoredAppConfig.isRecommendedApp("com.example.cmb", "招商银行"))
        assertFalse(MonitoredAppConfig.isRecommendedApp("com.example.notes", "备忘录"))
    }

    @Test
    fun filtersOutSystemAndUpdatedSystemApps() {
        assertTrue(MonitoredAppConfig.isUserInstalledAppFlags(0))
        assertFalse(MonitoredAppConfig.isUserInstalledAppFlags(ApplicationInfo.FLAG_SYSTEM))
        assertFalse(MonitoredAppConfig.isUserInstalledAppFlags(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
        assertFalse(
            MonitoredAppConfig.isUserInstalledAppFlags(
                ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            )
        )
    }
}
