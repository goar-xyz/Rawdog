package com.zed.nullshell;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Network Security & Auditing Suite (inspired by Strix / cSploit):
 * 1. Host Discovery & Subnet Sweep
 * 2. Multi-threaded TCP Port Scanner with Service Fingerprinting
 * 3. Network Diagnostics (Ping RTT, DNS Lookup, HTTP Header Grabber, Kali Nmap Launcher)
 */
public class NetworkActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout content;
    private final List<ScanSection> scans = new ArrayList<>();

    private int activeTab = 0; // 0 = Devices, 1 = Port Scan, 2 = Audit Tools
    private TextView tabDevices, tabPorts, tabAudit;

    // Port scan state
    private EditText portTargetInput;
    private TextView portScanStatus;
    private ProgressBar portScanBar;
    private LinearLayout portScanResults;
    private TextView portScanBtn;
    private PortScanner currentPortScanner;
    private boolean portScanRunning;

    // Audit tools state
    private EditText auditTargetInput;
    private LinearLayout auditResults;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // Header Bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        TextView back = Ui.iconButton(this, "←");
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView title = new TextView(this);
        title.setText("Network & Security");
        title.setTextColor(Ui.TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(Ui.dp(this, 8), 0, 0, 0);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView refresh = Ui.key(this, "Refresh");
        refresh.setOnClickListener(v -> renderActiveTab());
        bar.addView(refresh);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        // Navigation Tabs (Strix style)
        LinearLayout tabsBar = new LinearLayout(this);
        tabsBar.setOrientation(LinearLayout.HORIZONTAL);
        tabsBar.setBackgroundColor(Ui.SURFACE);
        tabsBar.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4));

        tabDevices = createTabButton("Host Discovery", 0);
        tabPorts = createTabButton("Port Scanner", 1);
        tabAudit = createTabButton("Diagnostics", 2);

        tabsBar.addView(tabDevices, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabsBar.addView(tabPorts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabsBar.addView(tabAudit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(tabsBar);

        View line = new View(this);
        line.setBackgroundColor(Ui.LINE);
        root.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // Scrollable Body
        ScrollView sv = new ScrollView(this);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 28));
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        Ui.applyInsets(root);
        renderActiveTab();
    }

    private TextView createTabButton(String label, final int tabIndex) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
        tv.setClickable(true);
        tv.setOnClickListener(v -> switchTab(tabIndex));
        return tv;
    }

    private void switchTab(int tabIndex) {
        if (activeTab == tabIndex) return;
        activeTab = tabIndex;
        renderActiveTab();
    }

    private void updateTabStyles() {
        tabDevices.setTextColor(activeTab == 0 ? Ui.ACCENT : Ui.DIM);
        tabDevices.setTypeface(Typeface.create("sans-serif-medium", activeTab == 0 ? Typeface.BOLD : Typeface.NORMAL));
        tabDevices.setBackground(Ui.rounded(this, activeTab == 0 ? Ui.SURFACE2 : 0, 8));

        tabPorts.setTextColor(activeTab == 1 ? Ui.ACCENT : Ui.DIM);
        tabPorts.setTypeface(Typeface.create("sans-serif-medium", activeTab == 1 ? Typeface.BOLD : Typeface.NORMAL));
        tabPorts.setBackground(Ui.rounded(this, activeTab == 1 ? Ui.SURFACE2 : 0, 8));

        tabAudit.setTextColor(activeTab == 2 ? Ui.ACCENT : Ui.DIM);
        tabAudit.setTypeface(Typeface.create("sans-serif-medium", activeTab == 2 ? Typeface.BOLD : Typeface.NORMAL));
        tabAudit.setBackground(Ui.rounded(this, activeTab == 2 ? Ui.SURFACE2 : 0, 8));
    }

    private void renderActiveTab() {
        updateTabStyles();
        content.removeAllViews();
        if (activeTab == 0) {
            buildDevicesTab();
        } else if (activeTab == 1) {
            buildPortScanTab();
        } else {
            buildAuditToolsTab();
        }
    }

    @Override protected void onDestroy() {
        for (ScanSection s : scans) s.stop();
        if (currentPortScanner != null) currentPortScanner.cancel();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ------------------------------------------------------------------ TAB 1: DEVICES
    private void buildDevicesTab() {
        for (ScanSection s : scans) s.stop();
        scans.clear();

        List<NetInfo.Iface> all = NetInfo.collect(this);

        LinearLayout me = card("This phone", "Tap an address to copy it");
        if (all.isEmpty()) me.addView(text("No network connection found.", 14, Ui.DIM));
        for (NetInfo.Iface f : all) {
            if (!f.ipv4.isEmpty()) me.addView(addressRow(f.label() + "  ·  " + f.name, f.ipv4, null));
            for (String v6 : f.ipv6) me.addView(addressRow(f.label() + "  ·  IPv6", v6, null));
        }

        NetInfo.Iface wifi = null;
        List<NetInfo.Iface> hotspots = new ArrayList<>();
        for (NetInfo.Iface f : all) {
            if (f.kind == NetInfo.WIFI && wifi == null) wifi = f;
            if (f.kind == NetInfo.HOTSPOT && !f.ipv4.isEmpty()) hotspots.add(f);
        }

        LinearLayout gw = card("Wi‑Fi router", wifi == null ? null : "Network " + wifi.cidr());
        if (wifi == null) {
            gw.addView(text("Not connected to Wi‑Fi.", 14, Ui.DIM));
        } else {
            if (!wifi.gateway.isEmpty()) gw.addView(addressRow("Gateway", wifi.gateway, null));
            else gw.addView(text("The gateway address was not reported.", 14, Ui.DIM));
            for (String d : wifi.dns) gw.addView(addressRow("DNS server", d, null));
        }

        if (wifi != null && wifi.scannable()) addScan("Devices on Wi‑Fi", wifi);
        else {
            LinearLayout c = card("Devices on Wi‑Fi", null);
            c.addView(text("Connect to a Wi‑Fi network to scan it.", 14, Ui.DIM));
        }

        if (hotspots.isEmpty()) {
            LinearLayout c = card("Devices on your hotspot", null);
            c.addView(text("The hotspot is off, or Android does not show it to apps. Turn on the hotspot (or USB tethering) and tap Refresh.", 14, Ui.DIM));
        } else {
            for (NetInfo.Iface h : hotspots) addScan("Devices on your " + h.label().toLowerCase(java.util.Locale.US), h);
        }

        TextView note = text("Devices that block every connection can stay hidden. Names come from reverse DNS and are often empty. "
                + "The scan only looks at the local /24 network.", 12, Ui.DIM);
        note.setPadding(dp(4), dp(14), dp(4), 0);
        content.addView(note);
    }

    private void addScan(String heading, NetInfo.Iface f) {
        LinearLayout c = card(heading, f.name + "  ·  " + f.cidr() + "  ·  this phone is " + f.ipv4);
        ScanSection s = new ScanSection(c, f);
        scans.add(s);
    }

    // ------------------------------------------------------------------ TAB 2: PORT SCANNER
    private void buildPortScanTab() {
        LinearLayout card = card("TCP Port Scanner", "Scan for active network services and open ports");

        // Target IP row
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        portTargetInput = new EditText(this);
        portTargetInput.setHint("Target IP or hostname");
        portTargetInput.setHintTextColor(Ui.HINT);
        portTargetInput.setTextColor(Ui.TEXT);
        portTargetInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        portTargetInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        portTargetInput.setBackground(Ui.rounded(this, Ui.BG, 8));
        portTargetInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        portTargetInput.setSingleLine(true);

        // Pre-fill with gateway or local subnet host if available
        List<NetInfo.Iface> all = NetInfo.collect(this);
        for (NetInfo.Iface f : all) {
            if (f.kind == NetInfo.WIFI && !f.gateway.isEmpty()) {
                portTargetInput.setText(f.gateway);
                break;
            }
        }

        inputRow.addView(portTargetInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        portScanBtn = Ui.button(this, "Scan Top 25", Ui.PRIMARY);
        portScanBtn.setOnClickListener(v -> togglePortScan());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        btnLp.leftMargin = dp(8);
        inputRow.addView(portScanBtn, btnLp);

        card.addView(inputRow);

        // Presets chips
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(10), 0, dp(4));

        TextView chipGw = Ui.key(this, "Router");
        chipGw.setOnClickListener(v -> {
            for (NetInfo.Iface f : NetInfo.collect(this)) {
                if (!f.gateway.isEmpty()) { portTargetInput.setText(f.gateway); break; }
            }
        });
        chips.addView(chipGw);

        TextView chipLocal = Ui.key(this, "Localhost");
        LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChip.leftMargin = dp(6);
        chipLocal.setLayoutParams(lpChip);
        chipLocal.setOnClickListener(v -> portTargetInput.setText("127.0.0.1"));
        chips.addView(chipLocal);

        TextView chipGoogle = Ui.key(this, "1.1.1.1");
        chipGoogle.setLayoutParams(lpChip);
        chipGoogle.setOnClickListener(v -> portTargetInput.setText("1.1.1.1"));
        chips.addView(chipGoogle);

        card.addView(chips);

        // Progress
        portScanBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        portScanBar.setMax(100);
        portScanBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12));
        bp.topMargin = dp(8);
        card.addView(portScanBar, bp);

        portScanStatus = text("Enter target host and start port scan.", 12, Ui.DIM);
        portScanStatus.setPadding(0, dp(6), 0, dp(6));
        card.addView(portScanStatus);

        // Results list
        portScanResults = new LinearLayout(this);
        portScanResults.setOrientation(LinearLayout.VERTICAL);
        card.addView(portScanResults);

        // Kali nmap shortcut
        if (RootfsManager.isInstalled(this)) {
            TextView kaliBtn = Ui.button(this, "Deep Scan with Nmap in Kali Terminal", Ui.SECONDARY);
            kaliBtn.setOnClickListener(v -> {
                String target = portTargetInput.getText().toString().trim();
                if (target.isEmpty()) target = "127.0.0.1";
                MainActivity.runInTerminal(this, "nmap --unprivileged -sV -T4 -Pn " + target);
            });
            LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            klp.topMargin = dp(14);
            card.addView(kaliBtn, klp);
        }
    }

    private void togglePortScan() {
        if (portScanRunning) {
            if (currentPortScanner != null) currentPortScanner.cancel();
            portScanRunning = false;
            portScanBtn.setText("Scan Top 25");
            portScanStatus.setText("Scan stopped.");
            portScanBar.setVisibility(View.GONE);
            return;
        }

        final String target = portTargetInput.getText().toString().trim();
        if (target.isEmpty()) {
            Toast.makeText(this, "Please enter a target host or IP", Toast.LENGTH_SHORT).show();
            return;
        }

        portScanRunning = true;
        portScanBtn.setText("Stop");
        portScanBar.setProgress(0);
        portScanBar.setVisibility(View.VISIBLE);
        portScanStatus.setText("Scanning " + target + " ports…");
        portScanResults.removeAllViews();

        currentPortScanner = new PortScanner();
        currentPortScanner.scan(target, PortScanner.TOP_PORTS, 1500, new PortScanner.Listener() {
            @Override public void onPortDiscovered(PortScanner.PortResult r) {
                ui.post(() -> addPortRow(target, r));
            }

            @Override public void onProgress(int done, int total) {
                ui.post(() -> {
                    portScanBar.setProgress(total == 0 ? 100 : (done * 100 / total));
                    portScanStatus.setText("Scanning " + target + " (" + done + "/" + total + ")…");
                });
            }

            @Override public void onDone(boolean cancelled, List<PortScanner.PortResult> openPorts) {
                ui.post(() -> {
                    portScanRunning = false;
                    portScanBtn.setText("Scan Top 25");
                    portScanBar.setVisibility(View.GONE);
                    portScanStatus.setText((cancelled ? "Stopped. " : "Finished. ") + openPorts.size() + " open ports found on " + target);
                });
            }
        });
    }

    private void addPortRow(String target, PortScanner.PortResult r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        row.setBackground(Ui.rounded(this, Ui.BG, 8));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(4);
        row.setLayoutParams(rlp);

        TextView portBadge = new TextView(this);
        portBadge.setText(String.valueOf(r.port));
        portBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        portBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        portBadge.setTextColor(Ui.ON_ACCENT);
        portBadge.setBackground(Ui.rounded(this, Ui.ACCENT, 6));
        portBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        row.addView(portBadge);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(10), 0, 0, 0);

        TextView name = new TextView(this);
        name.setText(r.service + " · OPEN");
        name.setTextColor(Ui.TEXT);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        texts.addView(name);

        TextView latency = new TextView(this);
        latency.setText(r.latencyMs >= 0 ? r.latencyMs + " ms response" : "open");
        latency.setTextColor(Ui.DIM);
        latency.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        texts.addView(latency);

        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (r.port == 80 || r.port == 443 || r.port == 8080 || r.port == 8443) {
            TextView openWeb = Ui.key(this, "Browser");
            openWeb.setOnClickListener(v -> {
                String scheme = (r.port == 443 || r.port == 8443) ? "https://" : "http://";
                BrowserActivity.open(this, scheme + target + ":" + r.port);
            });
            row.addView(openWeb);
        } else if (r.port == 22) {
            TextView sshBtn = Ui.key(this, "SSH");
            sshBtn.setOnClickListener(v -> MainActivity.runInTerminal(this, "ssh " + target));
            row.addView(sshBtn);
        }

        portScanResults.addView(row);
    }

    // ------------------------------------------------------------------ TAB 3: DIAGNOSTICS & AUDIT
    private void buildAuditToolsTab() {
        LinearLayout card = card("Diagnostics & Recon", "Network auditing tools inspired by Strix security framework");

        auditTargetInput = new EditText(this);
        auditTargetInput.setHint("Host, domain or IP (e.g. 1.1.1.1 or example.com)");
        auditTargetInput.setHintTextColor(Ui.HINT);
        auditTargetInput.setTextColor(Ui.TEXT);
        auditTargetInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        auditTargetInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        auditTargetInput.setBackground(Ui.rounded(this, Ui.BG, 8));
        auditTargetInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        auditTargetInput.setSingleLine(true);
        auditTargetInput.setText("1.1.1.1");
        card.addView(auditTargetInput);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, dp(8));

        TextView btnPing = Ui.button(this, "Ping (ICMP)", Ui.PRIMARY);
        btnPing.setOnClickListener(v -> runPingTool());
        actions.addView(btnPing, new LinearLayout.LayoutParams(0, dp(40), 1f));

        TextView btnDns = Ui.button(this, "DNS Lookup", Ui.SECONDARY);
        btnDns.setOnClickListener(v -> runDnsTool());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.leftMargin = dp(6);
        actions.addView(btnDns, lp);

        TextView btnHttp = Ui.button(this, "HTTP Headers", Ui.SECONDARY);
        btnHttp.setOnClickListener(v -> runHttpTool());
        actions.addView(btnHttp, lp);

        card.addView(actions);

        auditResults = new LinearLayout(this);
        auditResults.setOrientation(LinearLayout.VERTICAL);
        auditResults.setPadding(0, dp(4), 0, dp(4));
        card.addView(auditResults);

        // Strix Kali presets card
        if (RootfsManager.isInstalled(this)) {
            LinearLayout kaliCard = card("Kali Security Auditing Presets", "Run non-intrusive security checks inside Kali");

            TextView nmapBanner = Ui.button(this, "Nmap Service Version & OS Fingerprint", Ui.SECONDARY);
            nmapBanner.setOnClickListener(v -> {
                String target = auditTargetInput.getText().toString().trim();
                MainActivity.runInTerminal(this, "nmap --unprivileged -sV -O --osscan-guess " + target);
            });
            kaliCard.addView(nmapBanner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

            TextView nmapScript = Ui.button(this, "Nmap Discovery & Banner Scripts", Ui.SECONDARY);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
            slp.topMargin = dp(6);
            nmapScript.setOnClickListener(v -> {
                String target = auditTargetInput.getText().toString().trim();
                MainActivity.runInTerminal(this, "nmap --unprivileged -sV --script banner,http-title " + target);
            });
            kaliCard.addView(nmapScript, slp);
        }
    }

    private void runPingTool() {
        final String host = auditTargetInput.getText().toString().trim();
        if (host.isEmpty()) return;
        auditResults.removeAllViews();
        TextView status = text("Pinging " + host + " (4 packets)…", 13, Ui.ACCENT);
        status.setPadding(0, dp(4), 0, dp(4));
        auditResults.addView(status);

        NetworkTools.runPing(host, new NetworkTools.PingCallback() {
            @Override public void onLine(String line) {
                ui.post(() -> {
                    TextView t = text(line, 11, Ui.DIM);
                    t.setTypeface(Typeface.MONOSPACE);
                    auditResults.addView(t);
                });
            }

            @Override public void onResult(NetworkTools.PingStats stats) {
                ui.post(() -> {
                    status.setText("Ping complete: " + stats.received + "/" + stats.transmitted + " received ("
                            + stats.lossPercent + "% loss), avg " + stats.avgMs + " ms");
                    status.setTextColor(stats.received > 0 ? Ui.ACCENT : Ui.DANGER);
                });
            }

            @Override public void onError(String message) {
                ui.post(() -> {
                    status.setText("Ping failed: " + message);
                    status.setTextColor(Ui.DANGER);
                });
            }
        });
    }

    private void runDnsTool() {
        final String host = auditTargetInput.getText().toString().trim();
        if (host.isEmpty()) return;
        auditResults.removeAllViews();
        TextView status = text("Resolving DNS for " + host + "…", 13, Ui.ACCENT);
        status.setPadding(0, dp(4), 0, dp(4));
        auditResults.addView(status);

        NetworkTools.lookupDns(host, new NetworkTools.DnsCallback() {
            @Override public void onResult(String query, List<String> ipList, String canonicalName) {
                ui.post(() -> {
                    status.setText("DNS Results for " + query);
                    if (!canonicalName.isEmpty() && !canonicalName.equals(query)) {
                        auditResults.addView(addressRow("Canonical Name", canonicalName, null));
                    }
                    for (String ip : ipList) {
                        auditResults.addView(addressRow("Resolved IP", ip, null));
                    }
                });
            }

            @Override public void onError(String message) {
                ui.post(() -> {
                    status.setText(message);
                    status.setTextColor(Ui.DANGER);
                });
            }
        });
    }

    private void runHttpTool() {
        final String host = auditTargetInput.getText().toString().trim();
        if (host.isEmpty()) return;
        auditResults.removeAllViews();
        TextView status = text("Fetching HTTP response headers for " + host + "…", 13, Ui.ACCENT);
        status.setPadding(0, dp(4), 0, dp(4));
        auditResults.addView(status);

        NetworkTools.grabHttpBanner(host, new NetworkTools.HttpBannerCallback() {
            @Override public void onResult(int responseCode, String statusMessage, Map<String, List<String>> headers) {
                ui.post(() -> {
                    status.setText("HTTP Response: " + responseCode + " " + statusMessage);
                    for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                        String key = e.getKey() != null ? e.getKey() : "Status";
                        String val = String.join(", ", e.getValue());
                        auditResults.addView(addressRow(key, val, null));
                    }
                });
            }

            @Override public void onError(String message) {
                ui.post(() -> {
                    status.setText(message);
                    status.setTextColor(Ui.DANGER);
                });
            }
        });
    }

    // ------------------------------------------------------------------ HELPERS
    private int dp(int v) { return Ui.dp(this, v); }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("address", text));
        Toast.makeText(this, "Copied " + text, Toast.LENGTH_SHORT).show();
    }

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout card(String heading, String sub) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.rounded(this, Ui.SURFACE, 12));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView h = text(heading, 15, Ui.TEXT);
        h.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        c.addView(h);
        if (sub != null) {
            TextView s = text(sub, 12, Ui.DIM);
            s.setPadding(0, dp(2), 0, dp(4));
            c.addView(s);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        content.addView(c, lp);
        return c;
    }

    private View addressRow(String label, final String value, String copyText) {
        final String toCopy = copyText == null ? value : copyText;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        if (label != null && !label.isEmpty()) left.addView(text(label, 12, Ui.DIM));
        TextView v = text(value, 14, Ui.TEXT);
        v.setTypeface(Typeface.MONOSPACE);
        left.addView(v);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cp = Ui.key(this, "Copy");
        cp.setOnClickListener(x -> copy(toCopy));
        row.addView(cp);
        row.setOnClickListener(x -> copy(toCopy));
        return row;
    }

    // ------------------------------------------------------------------ LAN SCAN SECTION
    private final class ScanSection {
        private final NetInfo.Iface iface;
        private final LinearLayout card, list;
        private final TextView status, scanBtn, copyAll, nmapBtn;
        private final ProgressBar bar;
        private final List<LanScanner.Host> hosts = Collections.synchronizedList(new ArrayList<LanScanner.Host>());
        private LanScanner scanner;
        private boolean running;

        ScanSection(LinearLayout card, NetInfo.Iface iface) {
            this.card = card;
            this.iface = iface;

            LinearLayout buttons = new LinearLayout(this_());
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.CENTER_VERTICAL);
            scanBtn = Ui.button(this_(), "Scan Subnet", Ui.PRIMARY);
            scanBtn.setOnClickListener(v -> toggle());
            buttons.addView(scanBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
            copyAll = Ui.key(this_(), "Copy all IPs");
            copyAll.setOnClickListener(v -> copyAll());
            LinearLayout.LayoutParams k = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
            k.leftMargin = dp(8);
            buttons.addView(copyAll, k);
            nmapBtn = Ui.key(this_(), "nmap in Kali");
            nmapBtn.setOnClickListener(v -> MainActivity.runInTerminal(this_(), "nmap --unprivileged -sn -n -PS22,80,443,445,8080 " + iface.cidr()));
            nmapBtn.setVisibility(RootfsManager.isInstalled(this_()) ? View.VISIBLE : View.GONE);
            buttons.addView(nmapBtn, k);
            card.addView(buttons);

            bar = new ProgressBar(this_(), null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(1000);
            bar.setVisibility(View.GONE);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
            bp.topMargin = dp(8);
            card.addView(bar, bp);
            status = text("Tap Scan to look for devices on " + iface.name + ".", 12, Ui.DIM);
            status.setPadding(0, dp(6), 0, dp(2));
            card.addView(status);
            list = new LinearLayout(this_());
            list.setOrientation(LinearLayout.VERTICAL);
            card.addView(list);
            copyAll.setVisibility(View.GONE);
        }

        private NetworkActivity this_() { return NetworkActivity.this; }

        void stop() {
            if (scanner != null) scanner.cancel();
        }

        private void toggle() {
            if (running) { stop(); status.setText("Stopping…"); return; }
            start();
        }

        private void start() {
            hosts.clear();
            list.removeAllViews();
            running = true;
            scanBtn.setText("Stop");
            copyAll.setVisibility(View.GONE);
            bar.setProgress(0);
            bar.setVisibility(View.VISIBLE);
            status.setText("Scanning " + iface.cidr() + "…");
            InetAddress local = null;
            NetworkInterface nif = null;
            try {
                local = InetAddress.getByName(iface.ipv4);
                nif = NetworkInterface.getByName(iface.name);
            } catch (Exception ignored) { }
            scanner = new LanScanner();
            final long[] lastProgress = {0};
            scanner.scan(LanScanner.subnetHosts(iface.ipv4, iface.prefix), iface.ipv4, iface.name, local, nif, new LanScanner.Listener() {
                @Override public void onHost(LanScanner.Host h) { ui.post(() -> addHost(h)); }
                @Override public void onUpdate(LanScanner.Host h) { ui.post(() -> updateHost(h)); }
                @Override public void onProgress(int done, int total) {
                    long now = System.currentTimeMillis();
                    if (now - lastProgress[0] < 100 && done < total) return;
                    lastProgress[0] = now;
                    ui.post(() -> bar.setProgress(total == 0 ? 1000 : done * 1000 / total));
                }
                @Override public void onDone(boolean cancelled) { ui.post(() -> finished(cancelled)); }
            });
        }

        private void finished(boolean cancelled) {
            running = false;
            scanBtn.setText("Scan again");
            bar.setVisibility(View.GONE);
            int n = hosts.size();
            status.setText((cancelled ? "Stopped. " : "Done. ") + n + (n == 1 ? " device found." : " devices found.")
                    + (n == 0 ? " If you expected some, check that both are on the same network." : ""));
            copyAll.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        }

        private final java.util.Map<String, TextView[]> rows = new java.util.HashMap<>();

        private void addHost(LanScanner.Host h) {
            if (isFinishing() || rows.containsKey(h.ip) && hosts.contains(h)) return;
            hosts.add(h);
            Collections.sort(hosts);
            LinearLayout row = new LinearLayout(this_());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            TextView ip = text(h.ip, 15, Ui.TEXT);
            ip.setTypeface(Typeface.MONOSPACE);
            TextView sub = text("", 12, Ui.DIM);
            row.addView(ip);
            row.addView(sub);
            row.setOnClickListener(v -> deviceMenu(h));
            rows.put(h.ip, new TextView[]{ip, sub});
            int index = hosts.indexOf(h);
            list.addView(row, Math.min(index, list.getChildCount()));
            describe(h);
        }

        private void updateHost(LanScanner.Host h) { if (rows.containsKey(h.ip)) describe(h); }

        private void describe(LanScanner.Host h) {
            TextView[] t = rows.get(h.ip);
            if (t == null) return;
            StringBuilder s = new StringBuilder();
            if (h.ip.equals(iface.gateway)) s.append("gateway");
            if (!h.name.isEmpty()) s.append(s.length() > 0 ? "  ·  " : "").append(h.name);
            if (!h.mac.isEmpty()) s.append(s.length() > 0 ? "  ·  " : "").append(h.mac);
            if (h.ms >= 0 && !h.fromArp) s.append(s.length() > 0 ? "  ·  " : "").append(h.ms).append(" ms");
            if (h.fromArp) s.append(s.length() > 0 ? "  ·  " : "").append("seen in ARP table");
            t[1].setText(s.length() == 0 ? "tap for options" : s.toString());
        }

        private void copyAll() {
            StringBuilder sb = new StringBuilder();
            synchronized (hosts) { for (LanScanner.Host h : hosts) sb.append(h.ip).append('\n'); }
            copy(sb.toString().trim());
        }

        private void deviceMenu(final LanScanner.Host h) {
            List<String> items = new ArrayList<>();
            items.add("Port Scan in Raw");
            items.add("Ping device");
            items.add("Copy IP address");
            if (!h.name.isEmpty()) items.add("Copy IP and name");
            items.add("Open http://" + h.ip + " in Browser");
            final boolean kali = RootfsManager.isInstalled(NetworkActivity.this);
            if (kali) { items.add("Ping in Kali Terminal"); items.add("Nmap in Kali Terminal"); }
            new Dlg(NetworkActivity.this).setTitle(h.ip + (h.name.isEmpty() ? "" : "  " + h.name))
                    .setItems(items.toArray(new String[0]), (d, which) -> {
                        String it = items.get(which);
                        if (it.equals("Port Scan in Raw")) {
                            switchTab(1);
                            portTargetInput.setText(h.ip);
                            togglePortScan();
                        } else if (it.equals("Ping device")) {
                            switchTab(2);
                            auditTargetInput.setText(h.ip);
                            runPingTool();
                        } else if (it.equals("Copy IP address")) copy(h.ip);
                        else if (it.equals("Copy IP and name")) copy(h.ip + "  " + h.name);
                        else if (it.startsWith("Open http")) BrowserActivity.open(NetworkActivity.this, "http://" + h.ip);
                        else if (it.startsWith("Ping in Kali")) MainActivity.runInTerminal(NetworkActivity.this, "ping -c 4 " + h.ip);
                        else MainActivity.runInTerminal(NetworkActivity.this, "nmap --unprivileged -Pn -sT -sV -T4 " + h.ip);
                    }).show();
        }
    }
}
