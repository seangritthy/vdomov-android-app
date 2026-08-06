package com.example.myapp;

import android.content.Context;
import android.os.AsyncTask;
import android.webkit.WebResourceResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();
    private static boolean isInitialized = false;
    private static final AtomicInteger BLOCKED_COUNT = new AtomicInteger(0);

    private static final Pattern AD_URL_PATTERNS = Pattern.compile(
        ".*(?:/ad/|/ads/|/pop/|/banner/|/click/|/track/|/pixel/|/affiliate/|/promo/|/sponsor/|" +
        "adsbygoogle|pagead|popunder|popup|clickunder|adsystem|adserver|adservice|analytics|telemetry|" +
        "bet365|1xbet|mostbet|melbet|exoclick|juicyads|propellerads|popcash|popads|clickadu|hilltopads|" +
        "adsterra|zeropark|bidvertiser|mgid|revcontent|infolinks|adcash|revenuehits|adblade).*",
        Pattern.CASE_INSENSITIVE
    );

    // Reliable Ad / Tracker Blocklist URL (StevenBlack hosts)
    private static final String BLOCKLIST_URL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts";

    static {
        loadDefaultDomains();
    }

    private static void loadDefaultDomains() {
        String[] domains = new String[]{
            "doubleclick.net", "googleads.g.doubleclick.net", "pagead2.googlesyndication.com",
            "adservice.google.com", "googlesyndication.com", "google-analytics.com",
            "googletagmanager.com", "googletagservices.com", "adnxs.com", "amazon-adsystem.com",
            "adroll.com", "criteo.com", "outbrain.com", "taboola.com", "rubiconproject.com",
            "pubmatic.com", "openx.net", "adform.net", "casalemedia.com", "scorecardresearch.com",
            "zedo.com", "popads.net", "popcash.net", "propellerads.com", "exoclick.com",
            "juicyads.com", "popunder.net", "clickadu.com", "adsterra.com", "hilltopads.com",
            "bidvertiser.com", "revcontent.com", "mgid.com", "infolinks.com", "zeropark.com",
            "adcash.com", "popmyads.com", "revenuehits.com", "adblade.com", "media.net",
            "mc.yandex.ru", "an.yandex.ru", "adfox.ru", "ad.mail.ru", "target.my.com",
            "bet365.com", "1xbet.com", "mostbet.com", "melbet.com", "trafficjunky.com",
            "syndicated.exoclick.com", "main.exoclick.com", "ad.propellerads.com"
        };
        for (String d : domains) {
            AD_HOSTS.add(d.toLowerCase());
        }
    }

    public static void init(Context context) {
        if (isInitialized) return;
        isInitialized = true;
        loadLocalBlocklist(context);
        downloadAndInstallBlocklist(context, null);
    }

    public static void loadLocalBlocklist(Context context) {
        File file = new File(context.getFilesDir(), "adblock_hosts.txt");
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String host = parts[1].toLowerCase().trim();
                        if (!host.equals("localhost")) {
                            AD_HOSTS.add(host);
                        }
                    }
                } else {
                    AD_HOSTS.add(line.toLowerCase());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadAndInstallBlocklist(final Context context, final Runnable onComplete) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    URL url = new URL(BLOCKLIST_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    if (conn.getResponseCode() == 200) {
                        File file = new File(context.getFilesDir(), "adblock_hosts.txt");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        FileWriter writer = new FileWriter(file, false);

                        String line;
                        int count = 0;
                        while ((line = reader.readLine()) != null && count < 15000) { // Limit to 15,000 domains for optimal performance
                            line = line.trim();
                            if (line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) {
                                    String host = parts[1].toLowerCase().trim();
                                    if (!host.equals("localhost")) {
                                        AD_HOSTS.add(host);
                                        writer.write(host + "\n");
                                        count++;
                                    }
                                }
                            }
                        }
                        reader.close();
                        writer.flush();
                        writer.close();
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }.execute();
    }

    public static int getBlockedDomainsCount() {
        return AD_HOSTS.size();
    }

    public static int getBlockedCount() {
        return BLOCKED_COUNT.get();
    }

    public static boolean isAdOrRedirect(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return false;
        }

        String lowerUrl = urlString.toLowerCase();
        
        // Whitelist main domain, video streams, HLS, and media content
        if (lowerUrl.contains("vdomov.com") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".mp4") ||
            lowerUrl.contains(".webm") || lowerUrl.contains(".ts") || lowerUrl.contains("blob:") ||
            lowerUrl.contains("hls") || lowerUrl.contains("stream") || lowerUrl.contains("cdn")) {
            return false;
        }

        if (lowerUrl.startsWith("intent:") || lowerUrl.startsWith("market:") || lowerUrl.startsWith("about:blank#blocked")) {
            BLOCKED_COUNT.incrementAndGet();
            return true;
        }

        try {
            String host = getHost(urlString);
            if (isAdHost(host)) {
                BLOCKED_COUNT.incrementAndGet();
                return true;
            }
        } catch (Exception ignored) {
        }

        if (AD_URL_PATTERNS.matcher(urlString).matches()) {
            BLOCKED_COUNT.incrementAndGet();
            return true;
        }

        return false;
    }

    public static boolean isAdHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        host = host.toLowerCase();
        int index = 0;
        while (index >= 0) {
            if (AD_HOSTS.contains(host.substring(index))) {
                return true;
            }
            index = host.indexOf('.', index + 1);
        }
        return false;
    }

    public static String getHost(String url) throws MalformedURLException {
        return new URL(url).getHost();
    }

    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }

    public static String getAdBlockCSS() {
        return ".adsbygoogle, .ad-banner, .ad-box, .ad-container, [id*='google_ads'], " +
                "iframe[src*='doubleclick.net'], iframe[src*='exoclick.com'], iframe[src*='popads.net'], " +
                "iframe[src*='popunder'], iframe[src*='propellerads.com'], iframe[src*='juicyads.com'], " +
                "iframe[src*='bet365.com'], iframe[src*='1xbet.com'] { " +
                "display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; }";
    }

    public static String getAntiRedirectShieldScript() {
        return "javascript:(function() { " +
                "try { " +
                "  window.open = function() { console.log('OriginGuard Shield: Blocked popup window.open'); return null; }; " +
                "  window.alert = function() {}; " +
                "  window.confirm = function() { return true; }; " +
                "  window.onbeforeunload = null; " +
                "  document.addEventListener('click', function(e) { " +
                "    var target = e.target; " +
                "    while (target && target.tagName !== 'A') { target = target.parentElement; } " +
                "    if (target) { " +
                "      if (target.target === '_blank') { target.target = '_self'; } " +
                "    } " +
                "  }, true); " +
                "} catch(e) {} " +
                "})()";
    }
}

