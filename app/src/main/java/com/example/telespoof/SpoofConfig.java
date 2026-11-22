package com.example.telespoof;

import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 配置读取。优先用 LSPosed 的 XSharedPreferences（模块 App 写入，目标进程读取），
 * 读取失败时退回默认值。
 */
public final class SpoofConfig {

    public String mcc = "310";
    public String mnc = "260";
    public String iso = "us";
    public String alpha = "T-Mobile";
    public int lac = 1234;
    public int cid = 5678;
    public int pci = 100;
    public int tac = 1234;
    public String ssid = "HomeNet";
    public String bssid = "00:11:22:33:44:55";

    private static volatile SpoofConfig cached;
    private static volatile long cachedAt;

    public static SpoofConfig get() {
        long now = System.currentTimeMillis();
        SpoofConfig c = cached;
        if (c != null && now - cachedAt < 5000) {
            return c;
        }
        synchronized (SpoofConfig.class) {
            if (cached == null || now - cachedAt >= 5000) {
                cached = load();
                cachedAt = now;
            }
            return cached;
        }
    }

    private static SpoofConfig load() {
        SpoofConfig c = new SpoofConfig();
        try {
            XSharedPreferences p = new XSharedPreferences("com.example.telespoof", "config");
            p.reload();
            c.mcc = p.getString("mcc", c.mcc);
            c.mnc = p.getString("mnc", c.mnc);
            c.iso = p.getString("iso", c.iso);
            c.alpha = p.getString("alpha", c.alpha);
            c.lac = parseInt(p.getString("lac", String.valueOf(c.lac)), c.lac);
            c.cid = parseInt(p.getString("cid", String.valueOf(c.cid)), c.cid);
            c.pci = parseInt(p.getString("pci", String.valueOf(c.pci)), c.pci);
            c.tac = parseInt(p.getString("tac", String.valueOf(c.tac)), c.tac);
            c.ssid = p.getString("ssid", c.ssid);
            c.bssid = p.getString("bssid", c.bssid);
        } catch (Throwable ignored) {
        }
        return c;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    public String operatorNumeric() {
        return mcc + mnc;
    }

    public String simOperator() {
        return operatorNumeric();
    }

    public String networkOperator() {
        return operatorNumeric();
    }

    public String quotedSsid() {
        return "\"" + ssid + "\"";
    }

    public static void save(SharedPreferences p, SpoofConfig c) {
        p.edit()
                .putString("mcc", c.mcc)
                .putString("mnc", c.mnc)
                .putString("iso", c.iso)
                .putString("alpha", c.alpha)
                .putString("lac", String.valueOf(c.lac))
                .putString("cid", String.valueOf(c.cid))
                .putString("pci", String.valueOf(c.pci))
                .putString("tac", String.valueOf(c.tac))
                .putString("ssid", c.ssid)
                .putString("bssid", c.bssid)
                .apply();
    }
}
