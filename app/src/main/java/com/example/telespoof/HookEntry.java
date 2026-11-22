package com.example.telespoof;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        // 不改系统进程，只在被勾选的 App 进程里生效。
        if ("android".equals(lpparam.packageName)
                || "com.example.telespoof".equals(lpparam.packageName)) {
            return;
        }
        try {
            Hooks.install(lpparam.classLoader);
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: install failed in " + lpparam.packageName + ": " + t);
        }
    }
}
