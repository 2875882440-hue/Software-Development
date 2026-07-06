package com.localbookkeeping.app.notification

import android.os.Build

data class DeviceCompatInfo(
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val osVersion: String,
    val sdkVersion: Int,
    val harmonyOsVersion: String,
    val emuiVersion: String,
    val magicOsVersion: String,
    val hyperOsVersion: String,
    val displayId: String,
    val detectedRomType: String,
    val isHuaweiHonorDevice: Boolean
) {
    val deviceName: String
        get() = listOf(brand, model).filter { it.isNotBlank() && it != "unknown" }.distinct().joinToString(" ")
            .ifBlank { model.ifBlank { "unknown" } }

    val systemLabel: String
        get() = when {
            harmonyOsVersion != "unknown" -> "HarmonyOS $harmonyOsVersion"
            magicOsVersion != "unknown" -> "MagicOS $magicOsVersion"
            emuiVersion != "unknown" -> "EMUI $emuiVersion"
            else -> "Android $osVersion"
        }

    val compatibilityModeLabel: String
        get() = if (isHuaweiHonorDevice) "荣耀 / 华为监听兼容模式" else "标准 Android 监听模式"

    val vendorCompatSuggestion: String
        get() = if (isHuaweiHonorDevice) {
            "请重新授权通知监听，并检查应用启动管理、后台活动、电池优化和通知权限。"
        } else {
            "按标准 Android 通知监听恢复流程处理。"
        }

    companion object {
        fun current(): DeviceCompatInfo =
            from(
                brand = Build.BRAND.orUnknown(),
                manufacturer = Build.MANUFACTURER.orUnknown(),
                model = Build.MODEL.orUnknown(),
                device = Build.DEVICE.orUnknown(),
                osVersion = Build.VERSION.RELEASE.orUnknown(),
                sdkVersion = Build.VERSION.SDK_INT,
                harmonyOsVersion = firstSystemProperty(
                    "hw_sc.build.platform.version",
                    "ro.build.version.harmony",
                    "ro.harmony.version"
                ),
                emuiVersion = firstSystemProperty(
                    "ro.build.version.emui",
                    "ro.confg.hw_systemversion",
                    "ro.config.hw_systemversion"
                ),
                magicOsVersion = firstSystemProperty(
                    "ro.build.version.magic",
                    "ro.build.version.magicui",
                    "ro.build.magic.version"
                ),
                hyperOsVersion = firstSystemProperty(
                    "ro.mi.os.version.name",
                    "ro.miui.ui.version.name"
                ),
                displayId = Build.DISPLAY.orUnknown()
            )

        fun from(
            brand: String,
            manufacturer: String,
            model: String,
            device: String,
            osVersion: String,
            sdkVersion: Int,
            harmonyOsVersion: String = "unknown",
            emuiVersion: String = "unknown",
            magicOsVersion: String = "unknown",
            hyperOsVersion: String = "unknown",
            displayId: String = "unknown"
        ): DeviceCompatInfo {
            val combined = listOf(
                brand,
                manufacturer,
                model,
                device,
                harmonyOsVersion,
                emuiVersion,
                magicOsVersion,
                displayId
            ).joinToString(" ").uppercase()
            val romType = when {
                combined.contains("HARMONY") || harmonyOsVersion != "unknown" -> "HarmonyOS"
                combined.contains("MAGIC") || magicOsVersion != "unknown" -> "MagicOS"
                combined.contains("EMUI") || emuiVersion != "unknown" -> "EMUI"
                combined.contains("HYPER") || hyperOsVersion != "unknown" -> "HyperOS"
                else -> "Android"
            }
            val isHuaweiHonor = combined.contains("HUAWEI") ||
                combined.contains("HONOR") ||
                romType == "HarmonyOS" ||
                romType == "MagicOS" ||
                romType == "EMUI"
            return DeviceCompatInfo(
                brand = brand.orUnknown(),
                manufacturer = manufacturer.orUnknown(),
                model = model.orUnknown(),
                device = device.orUnknown(),
                osVersion = osVersion.orUnknown(),
                sdkVersion = sdkVersion,
                harmonyOsVersion = harmonyOsVersion.orUnknown(),
                emuiVersion = emuiVersion.orUnknown(),
                magicOsVersion = magicOsVersion.orUnknown(),
                hyperOsVersion = hyperOsVersion.orUnknown(),
                displayId = displayId.orUnknown(),
                detectedRomType = romType,
                isHuaweiHonorDevice = isHuaweiHonor
            )
        }

        private fun firstSystemProperty(vararg keys: String): String =
            keys.asSequence()
                .map { getSystemProperty(it) }
                .firstOrNull { it.isNotBlank() }
                .orUnknown()

        @Suppress("PrivateApi")
        private fun getSystemProperty(key: String): String =
            runCatching {
                val clazz = Class.forName("android.os.SystemProperties")
                val method = clazz.getMethod("get", String::class.java)
                method.invoke(null, key) as? String
            }.getOrNull().orEmpty().trim()

        private fun String?.orUnknown(): String =
            this?.trim()?.takeIf { it.isNotBlank() } ?: "unknown"
    }
}
