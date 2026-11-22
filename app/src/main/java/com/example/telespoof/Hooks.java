package com.example.telespoof;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 所有 hook 都只作用于 LSPosed 里勾选的作用域进程。
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
                return SpoofConfig.get().simOperator();
            }
        });
        hook(cl, tm, "getNetworkOperator", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return SpoofConfig.get().networkOperator();
            }
        });
        hook(cl, tm, "getSimOperatorName", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return SpoofConfig.get().alpha;
            }
        });
        hook(cl, tm, "getNetworkOperatorName", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return SpoofConfig.get().alpha;
            }
        });
        hook(cl, tm, "getSimCountryIso", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return SpoofConfig.get().iso;
            }
        });
        hook(cl, tm, "getNetworkCountryIso", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return SpoofConfig.get().iso;
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
        try {
            XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", cl,
                    "getAllCellInfo", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(fakeCells());
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
                            // 隐藏真实基站；返回 null 比返回中国基站安全。
                            return null;
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("TeleSpoof: hook getCellLocation failed: " + t);
        }
    }

    private static List<CellInfo> fakeCells() {
        SpoofConfig c = SpoofConfig.get();
        try {
            Constructor<?> ctor = null;
            for (Constructor<?> k : CellIdentityLte.class.getDeclaredConstructors()) {
                Class<?>[] p = k.getParameterTypes();
                if (p.length == 5
                        && p[0] == int.class && p[1] == int.class && p[2] == int.class
                        && p[3] == int.class && p[4] == int.class) {
                    ctor = k;
                    break;
                }
            }
            if (ctor != null) {
                ctor.setAccessible(true);
                Object id = ctor.newInstance(
                        Integer.parseInt(c.mcc), Integer.parseInt(c.mnc), c.cid, c.pci, c.tac);
                CellInfoLte info = new CellInfoLte();
                Method set = CellInfoLte.class.getMethod("setCellIdentity", CellIdentityLte.class);
                set.setAccessible(true);
                set.invoke(info, id);
                List<CellInfo> out = new ArrayList<>();
                out.add(info);
                return out;
            }
        } catch (Throwable ignored) {
        }
        // 兜底：返回空列表，至少不把真实的中国基站暴露出去。
        return Collections.emptyList();
    }

    private static void hookWifi(ClassLoader cl) {
        // Wi-Fi 扫描结果：直接清空，避免暴露周围中国热点。
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

        // 当前连接信息：只改 SSID / BSSID，其余字段保留真实对象，避免 NPE。
        try {
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getSSID",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            return SpoofConfig.get().quotedSsid();
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
                            return SpoofConfig.get().bssid;
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
