package com.example.telespoof;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 所有 hook 都只作用于 LSPosed 里勾选的作用域进程。
 * 原则：返回值类型必须与目标方法一致，且不返回半初始化对象，避免目标 App NPE。
 */
final class Hooks {

    private Hooks() {
    }

    static void install(ClassLoader cl) {
        hookTelephonyScalars(cl);
        hookCells(cl);
        hookWifi(cl);
    }

    private static void hookTelephonyScalars(ClassLoader cl) {
        String tm = "android.telephony.TelephonyManager";

        hook(cl, tm, "getSimOperator", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                try {
                    return SpoofConfig.get().simOperator();
                } catch (Throwable t) {
                    return "310260";
                }
            }
        });
        hook(cl, tm, "getNetworkOperator", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                try {
                    return SpoofConfig.get().networkOperator();
                } catch (Throwable t) {
                    return "310260";
                }
            }
        });
        hook(cl, tm, "getSimOperatorName", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                try {
                    return SpoofConfig.get().alpha;
                } catch (Throwable t) {
                    return "T-Mobile";
                }
            }
        });
        hook(cl, tm, "getNetworkOperatorName", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                try {
                    return SpoofConfig.get().alpha;
                } catch (Throwable t) {
                    return "T-Mobile";
                }
            }
        });
        hook(cl, tm, "getSimCountryIso", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                try {
                    return SpoofConfig.get().iso;
                } catch (Throwable t) {
                    return "us";
                }
            }
        });
        hook(cl, tm, "getNetworkCountryIso", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                try {
                    return SpoofConfig.get().iso;
                } catch (Throwable t) {
                    return "us";
                }
            }
        });
        hook(cl, tm, "getSimState", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return TelephonyManager.SIM_STATE_READY;
            }
        });
        hook(cl, tm, "isNetworkRoaming", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return false;
            }
        });
        hook(cl, tm, "getPhoneType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return TelephonyManager.PHONE_TYPE_GSM;
            }
        });
        hook(cl, tm, "getNetworkType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return TelephonyManager.NETWORK_TYPE_LTE;
            }
        });
        hook(cl, tm, "getDataNetworkType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return TelephonyManager.NETWORK_TYPE_LTE;
            }
        });
        hook(cl, tm, "getVoiceNetworkType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return TelephonyManager.NETWORK_TYPE_LTE;
            }
        });
    }

    private static void hookCells(ClassLoader cl) {
        // 只“隐藏”，不伪造 CellInfoLte 对象：反射构造的半成品对象会让读取
        // getCellSignalStrength()/getCellIdentity() 的 App 直接 NPE（DevCheck 闪退就是这个原因）。
        try {
            XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", cl,
                    "getAllCellInfo", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                param.setResult(Collections.emptyList());
                            } catch (Throwable t) {
                                XposedBridge.log("TeleSpoof: getAllCellInfo hook failed: " + t);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook getAllCellInfo failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", cl,
                    "getCellLocation", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            return null;
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook getCellLocation failed: " + t);
        }
    }

    private static void hookWifi(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(WifiManager.class, "getScanResults",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            return new ArrayList<ScanResult>();
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook getScanResults failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getSSID",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            try {
                                return SpoofConfig.get().quotedSsid();
                            } catch (Throwable t) {
                                return "\"HomeNet\"";
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook getSSID failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getBSSID",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            try {
                                return SpoofConfig.get().bssid;
                            } catch (Throwable t) {
                                return "00:11:22:33:44:55";
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook getBSSID failed: " + t);
        }
    }

    private static void hook(ClassLoader cl, String className, String method, XC_MethodReplacement r) {
        try {
            XposedHelpers.findAndHookMethod(className, cl, method, r);
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook " + className + "#" + method + " failed: " + t);
        }
    }
}
