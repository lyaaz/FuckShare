package org.baiyu.fuckshare

import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "android") {
            return
        }
        XposedBridge.hookAllMethods(
            XposedHelpers.findClass(
                "com.android.server.wm.ActivityTaskManagerService",
                lpparam.classLoader
            ),
            "startActivity",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val callingPackage = param.args[1] as? String ?: return
                    val chooserIntent = param.args[3] as? Intent ?: return
                    if (callingPackage == BuildConfig.APPLICATION_ID || Intent.ACTION_CHOOSER != chooserIntent.action) {
                        return
                    }

                    prefs.reload()
                    if (!settings.enableForceForwardHook()) {
                        return
                    }
                    val intent = Utils.getParcelableExtra(
                        chooserIntent,
                        Intent.EXTRA_INTENT,
                        Intent::class.java
                    )!!
                    if (Intent.ACTION_SEND == intent.action || Intent.ACTION_SEND_MULTIPLE == intent.action) {
                        param.args[3] = intent.apply {
                            setClassName(
                                BuildConfig.APPLICATION_ID,
                                HandleShareActivity::class.java.name
                            )
                        }
                    }
                }
            }
        )
    }

    companion object {
        private val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID)
        private val settings: Settings = Settings.getInstance(prefs)
    }
}