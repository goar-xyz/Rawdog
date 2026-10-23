package com.zed.nullshell;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic & security audit helpers inspired by the Strix security suite:
 * Ping / Latency check, DNS resolution, and HTTP Header / Banner inspection.
 */
public final class NetworkTools {
    private NetworkTools() { }

    public static final class PingStats {
        public final int transmitted;
        public final int received;
        public final float lossPercent;
        public final float avgMs;
        public final List<String> rawLines;

        public PingStats(int transmitted, int received, float lossPercent, float avgMs, List<String> rawLines) {
            this.transmitted = transmitted;
            this.received = received;
            this.lossPercent = lossPercent;
            this.avgMs = avgMs;
            this.rawLines = rawLines;
        }
    }

    public interface PingCallback {
        void onLine(String line);
        void onResult(PingStats stats);
        void onError(String message);
    }

    /** Runs native ping command for 4 packets. */
    public static void runPing(final String host, final PingCallback callback) {
        new Thread(() -> {
            List<String> lines = new ArrayList<>();
            int tx = 0, rx = 0;
            float loss = 100f, avg = 0f;
            try {
                Process p = new ProcessBuilder("ping", "-c", "4", "-W", "2", host).redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                        if (callback != null) callback.onLine(line);
                        if (line.contains("packets transmitted")) {
                            String[] parts = line.split(",");
                            for (String part : parts) {
                                part = part.trim();
                                if (part.contains("transmitted")) tx = Integer.parseInt(part.split(" ")[0]);
                                if (part.contains("received")) rx = Integer.parseInt(part.split(" ")[0]);
                                if (part.contains("% packet loss")) {
                                    String lp = part.replace("% packet loss", "").trim();
                                    try { loss = Float.parseFloat(lp); } catch (Exception ignored) { }
                                }
                            }
                        } else if (line.contains("rtt min/avg/max")) {
                            String[] slash = line.split("=")[1].trim().split("/");
                            if (slash.length >= 2) {
                                try { avg = Float.parseFloat(slash[1]); } catch (Exception ignored) { }
                            }
                        }
                    }
                }
                p.waitFor();
                if (callback != null) callback.onResult(new PingStats(tx, rx, loss, avg, lines));
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage() != null ? e.getMessage() : "Ping failed");
            }
        }, "Ping-Worker").start();
    }

    public interface DnsCallback {
        void onResult(String query, List<String> ipList, String canonicalName);
        void onError(String message);
    }

    /** Performs DNS resolution and reverse lookup. */
    public static void lookupDns(final String host, final DnsCallback callback) {
        new Thread(() -> {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                List<String> ips = new ArrayList<>();
                String canonical = "";
                for (InetAddress a : addresses) {
                    ips.add(a.getHostAddress());
                    if (canonical.isEmpty()) canonical = a.getCanonicalHostName();
                }
                if (callback != null) callback.onResult(host, ips, canonical);
            } catch (Exception e) {
                if (callback != null) callback.onError("DNS resolution failed: " + e.getMessage());
            }
        }, "DNS-Worker").start();
    }

    public interface HttpBannerCallback {
        void onResult(int responseCode, String statusMessage, Map<String, List<String>> headers);
        void onError(String message);
    }

    /** Fetches HTTP headers and banner fingerprint. */
    public static void grabHttpBanner(final String urlString, final HttpBannerCallback callback) {
        new Thread(() -> {
            try {
                String target = urlString;
                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                    target = "http://" + target;
                }
                URL url = new URL(target);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; Raw-Audit/1.0)");
                int code = conn.getResponseCode();
                String msg = conn.getResponseMessage();
                Map<String, List<String>> headers = new LinkedHashMap<>(conn.getHeaderFields());
                conn.disconnect();
                if (callback != null) callback.onResult(code, msg != null ? msg : "", headers);
            } catch (Exception e) {
                if (callback != null) callback.onError("HTTP request failed: " + e.getMessage());
            }
        }, "HTTP-Banner-Worker").start();
    }
}
