package io.nekohasekai.sfa.compose.screen.profileoverride

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.utils.PackageQueryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import java.io.File
import java.util.zip.ZipFile

object PerAppProxyScanner {
    private val skipPrefixList =
        listOf(
            "com.google",
            "com.android.chrome",
            "com.android.vending",
            "com.microsoft",
            "com.apple",
            "com.zhiliaoapp.musically",
            "com.android.providers.downloads",
        )

    private val chinaAppPrefixList =
        listOf(
            "com.tencent",
            "com.alibaba",
            "com.umeng",
            "com.qihoo",
            "com.ali",
            "com.alipay",
            "com.amap",
            "com.sina",
            "com.weibo",
            "com.vivo",
            "com.xiaomi",
            "com.huawei",
            "com.taobao",
            "com.secneo",
            "s.h.e.l.l",
            "com.stub",
            "com.kiwisec",
            "com.secshell",
            "com.wrapper",
            "cn.securitystack",
            "com.mogosec",
            "com.secoen",
            "com.netease",
            "com.mx",
            "com.qq.e",
            "com.baidu",
            "com.bytedance",
            "com.bugly",
            "com.miui",
            "com.oppo",
            "com.coloros",
            "com.iqoo",
            "com.meizu",
            "com.gionee",
            "cn.nubia",
            "com.oplus",
            "andes.oplus",
            "com.unionpay",
            "cn.wps",
        )

    private val chinaAppRegex by lazy {
        ("(" + chinaAppPrefixList.joinToString("|").replace(".", "\\.") + ").*").toRegex()
    }

    suspend fun scanAllChinaApps(): Set<String> = withContext(Dispatchers.Default) {
        val packageManagerFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.MATCH_UNINSTALLED_PACKAGES or
                PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_UNINSTALLED_PACKAGES or
                PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
        }
        val retryFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_PERMISSIONS
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_UNINSTALLED_PACKAGES or PackageManager.GET_PERMISSIONS
        }
        val installedPackages = PackageQueryManager.getInstalledPackages(packageManagerFlags, retryFlags)
        val chinaApps = mutableSetOf<String>()
        installedPackages.map { packageInfo ->
            async {
                if (scanChinaPackage(packageInfo)) {
                    synchronized(chinaApps) {
                        chinaApps.add(packageInfo.packageName)
                    }
                }
            }
        }.awaitAll()
        chinaApps.toSet()
    }

    fun scanChinaPackage(packageInfo: PackageInfo): Boolean {
        val packageName = packageInfo.packageName
        if (packageName == Application.application.packageName) return false
        skipPrefixList.forEach {
            if (packageName == it || packageName.startsWith("$it.")) return false
        }

        if (packageName.matches(chinaAppRegex)) {
            Log.d("PerAppProxyScanner", "Match package name: $packageName")
            return true
        }
        try {
            val appInfo = packageInfo.applicationInfo ?: return false
            packageInfo.services?.forEach {
                if (it.name.matches(chinaAppRegex)) {
                    Log.d("PerAppProxyScanner", "Match service ${it.name} in $packageName")
                    return true
                }
            }
            packageInfo.activities?.forEach {
                if (it.name.matches(chinaAppRegex)) {
                    Log.d("PerAppProxyScanner", "Match activity ${it.name} in $packageName")
                    return true
                }
            }
            packageInfo.receivers?.forEach {
                if (it.name.matches(chinaAppRegex)) {
                    Log.d("PerAppProxyScanner", "Match receiver ${it.name} in $packageName")
                    return true
                }
            }
            packageInfo.providers?.forEach {
                if (it.name.matches(chinaAppRegex)) {
                    Log.d("PerAppProxyScanner", "Match provider ${it.name} in $packageName")
                    return true
                }
            }
            ZipFile(File(appInfo.publicSourceDir)).use {
                for (packageEntry in it.entries()) {
                    if (packageEntry.name.startsWith("firebase-")) return false
                }
                for (packageEntry in it.entries()) {
                    if (!(
                            packageEntry.name.startsWith("classes") &&
                                packageEntry.name.endsWith(".dex")
                            )
                    ) {
                        continue
                    }
                    if (packageEntry.size > 15000000) {
                        Log.d(
                            "PerAppProxyScanner",
                            "Confirm $packageName due to large dex file",
                        )
                        return true
                    }
                    val input = it.getInputStream(packageEntry).buffered()
                    val dexFile =
                        try {
                            DexBackedDexFile.fromInputStream(null, input)
                        } catch (e: Exception) {
                            Log.e("PerAppProxyScanner", "Error reading dex file", e)
                            return false
                        }
                    for (clazz in dexFile.classes) {
                        val clazzName =
                            clazz.type.substring(1, clazz.type.length - 1).replace("/", ".")
                                .replace("$", ".")
                        if (clazzName.matches(chinaAppRegex)) {
                            Log.d("PerAppProxyScanner", "Match $clazzName in $packageName")
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PerAppProxyScanner", "Error scanning package $packageName", e)
        }
        return false
    }
}
