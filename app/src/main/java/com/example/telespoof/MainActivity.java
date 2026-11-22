package com.example.telespoof;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText mcc, mnc, iso, alpha, lac, cid, pci, tac, ssid, bssid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("TeleSpoof");
        title.setTextSize(22);
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("在 LSPosed 中启用本模块，并把目标 App（Google 系还要勾 GMS）加入作用域；改完保存后强制停止目标 App 再打开。\n");
        note.setTextSize(13);
        root.addView(note);

        SharedPreferences p = getSharedPreferences("config", MODE_PRIVATE);
        mcc = addField(root, "MCC", p.getString("mcc", "310"));
        mnc = addField(root, "MNC", p.getString("mnc", "260"));
        iso = addField(root, "国家代码 ISO", p.getString("iso", "us"));
        alpha = addField(root, "运营商名称", p.getString("alpha", "T-Mobile"));
        lac = addField(root, "LAC/TAC", p.getString("tac", "1234"));
        cid = addField(root, "Cell ID", p.getString("cid", "5678"));
        pci = addField(root, "PCI", p.getString("pci", "100"));
        tac = addField(root, "TAC", p.getString("tac", "1234"));
        ssid = addField(root, "Wi-Fi SSID", p.getString("ssid", "HomeNet"));
        bssid = addField(root, "Wi-Fi BSSID", p.getString("bssid", "00:11:22:33:44:55"));

        Button save = new Button(this);
        save.setText("保存");
        save.setOnClickListener(v -> {
            SpoofConfig c = new SpoofConfig();
            c.mcc = mcc.getText().toString().trim();
            c.mnc = mnc.getText().toString().trim();
            c.iso = iso.getText().toString().trim();
            c.alpha = alpha.getText().toString().trim();
            c.cid = parseInt(cid.getText().toString(), 5678);
            c.pci = parseInt(pci.getText().toString(), 100);
            c.tac = parseInt(tac.getText().toString(), 1234);
            c.ssid = ssid.getText().toString().trim();
            c.bssid = bssid.getText().toString().trim();
            SpoofConfig.save(p, c);
            Toast.makeText(this, "已保存，强制停止目标 App 后重新打开", Toast.LENGTH_LONG).show();
        });
        root.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView foot = new TextView(this);
        foot.setText("\n说明：TelephonyManager 的运营商/国家、基站列表、Wi-Fi 扫描结果与 SSID/BSSID 会被替换；"
                + "基站伪造依赖反射，失败时回退为空列表（隐藏真实值）。");
        foot.setTextSize(12);
        root.addView(foot);

        setContentView(scroll);
    }

    private EditText addField(LinearLayout parent, String hint, String value) {
        TextView label = new TextView(this);
        label.setText(hint);
        label.setTextSize(12);
        parent.addView(label);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        parent.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable t) {
            return def;
        }
    }
}
