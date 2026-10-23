package com.zed.nullshell;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fast multi-threaded TCP Port Scanner inspired by Strix network auditing suite.
 * Pure Java, works without root permissions on Android.
 */
public final class PortScanner {

    public static final class PortResult implements Comparable<PortResult> {
        public final int port;
        public final String service;
        public final boolean open;
        public final int latencyMs;

        public PortResult(int port, String service, boolean open, int latencyMs) {
            this.port = port;
            this.service = service;
            this.open = open;
            this.latencyMs = latencyMs;
        }

        @Override
        public int compareTo(PortResult o) {
            return Integer.compare(port, o.port);
        }
    }

    public interface Listener {
        void onPortDiscovered(PortResult result);
        void onProgress(int done, int total);
        void onDone(boolean cancelled, List<PortResult> openPorts);
    }

    // Common well-known security auditing and service ports
    private static final Map<Integer, String> SERVICES = new HashMap<>();
    static {
        SERVICES.put(20, "FTP-Data");
        SERVICES.put(21, "FTP");
        SERVICES.put(22, "SSH");
        SERVICES.put(23, "Telnet");
        SERVICES.put(25, "SMTP");
        SERVICES.put(53, "DNS");
        SERVICES.put(80, "HTTP");
        SERVICES.put(110, "POP3");
        SERVICES.put(111, "RPCBind");
        SERVICES.put(135, "MSRPC");
        SERVICES.put(139, "NetBIOS");
        SERVICES.put(143, "IMAP");
        SERVICES.put(443, "HTTPS");
        SERVICES.put(445, "SMB");
        SERVICES.put(993, "IMAPS");
        SERVICES.put(995, "POP3S");
        SERVICES.put(1433, "MSSQL");
        SERVICES.put(1521, "Oracle");
        SERVICES.put(2049, "NFS");
        SERVICES.put(3306, "MySQL");
        SERVICES.put(3389, "RDP");
        SERVICES.put(5432, "PostgreSQL");
        SERVICES.put(5900, "VNC");
        SERVICES.put(5901, "VNC-1");
        SERVICES.put(6379, "Redis");
        SERVICES.put(8000, "HTTP-Alt");
        SERVICES.put(8080, "HTTP-Proxy");
        SERVICES.put(8443, "HTTPS-Alt");
        SERVICES.put(8888, "HTTP-Dev");
        SERVICES.put(9000, "Sonar/PHP");
        SERVICES.put(9200, "Elasticsearch");
        SERVICES.put(27017, "MongoDB");
    }

    public static final int[] TOP_PORTS = {
            21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445, 993, 995,
            1433, 3306, 3389, 5432, 5900, 6379, 8000, 8080, 8443, 8888, 9200, 27017
    };

    public static String getService(int port) {
        String s = SERVICES.get(port);
        return s != null ? s : "Unknown";
    }

    private volatile boolean cancelled;
    private ExecutorService pool;

    public void cancel() {
        cancelled = true;
        if (pool != null) pool.shutdownNow();
    }

    public void scan(final String targetHost, final int[] ports, final int timeoutMs, final Listener listener) {
        cancelled = false;
        pool = Executors.newFixedThreadPool(16);
        final int total = ports.length;
        final AtomicInteger done = new AtomicInteger(0);
        final List<PortResult> openResults = Collections.synchronizedList(new ArrayList<PortResult>());

        new Thread(() -> {
            List<Future<?>> futures = new ArrayList<>();
            for (final int port : ports) {
                if (cancelled) break;
                futures.add(pool.submit(() -> {
                    if (cancelled) return;
                    long start = System.currentTimeMillis();
                    boolean isOpen = false;
                    int latency = -1;
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(targetHost, port), timeoutMs);
                        isOpen = true;
                        latency = (int) (System.currentTimeMillis() - start);
                    } catch (Exception ignored) {
                        // Port closed or filtered
                    }

                    if (isOpen) {
                        PortResult res = new PortResult(port, getService(port), true, latency);
                        openResults.add(res);
                        if (listener != null) listener.onPortDiscovered(res);
                    }

                    int count = done.incrementAndGet();
                    if (listener != null) listener.onProgress(count, total);
                }));
            }

            for (Future<?> f : futures) {
                try {
                    f.get(timeoutMs + 500, TimeUnit.MILLISECONDS);
                } catch (Exception ignored) { }
            }

            pool.shutdown();
            Collections.sort(openResults);
            if (listener != null) listener.onDone(cancelled, openResults);
        }, "PortScanner-Worker").start();
    }
}
