package com.example.myapp;

import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class AdBlocker {
    private static final Set<String> AD_HOSTS = new HashSet<>();
    private static final Pattern AD_URL_PATTERNS = Pattern.compile(
        ".*(?:/ad/|/ads/|/pop/|/banner/|/click/|/track/|/pixel/|/affiliate/|/promo/|/sponsor/|" +
        "adsbygoogle|pagead|popunder|popup|clickunder|adsystem|adserver|adservice|analytics|telemetry).*",
        Pattern.CASE_INSENSITIVE
    );

    static {
        String[] domains = new String[]{
            // Major Ad Networks
            "doubleclick.net", "googleads.g.doubleclick.net", "pagead2.googlesyndication.com",
            "adservice.google.com", "googlesyndication.com", "google-analytics.com",
            "googletagmanager.com", "googletagservices.com", "adnxs.com", "amazon-adsystem.com",
            "adroll.com", "criteo.com", "outbrain.com", "taboola.com", "rubiconproject.com",
            "pubmatic.com", "openx.net", "adform.net", "casalemedia.com", "scorecardresearch.com",
            "zedo.com",

            // Popups, Redirects & Aggressive Ad Networks
            "popads.net", "popcash.net", "propellerads.com", "exoclick.com", "juicyads.com",
            "popunder.net", "clickadu.com", "adsterra.com", "hilltopads.com", "bidvertiser.com",
            "revcontent.com", "mgid.com", "infolinks.com", "zeropark.com", "adcash.com",
            "popmyads.com", "revenuehits.com", "adblade.com", "chitmika.com", "media.net",
            "clickbank.net", "trafficfactory.biz", "adreactor.com", "activerevenue.com",
            "adbooth.com", "adcolony.com", "adcombo.com", "adkmob.com", "ad-maven.com",
            "adtrue.com", "richads.com", "trafficjunky.net", "trafficstars.com",

            // Analytics & Trackers
            "mc.yandex.ru", "an.yandex.ru", "adfox.ru", "ad.mail.ru", "target.my.com",
            "hotjar.com", "mixpanel.com", "amplitude.com", "segment.io", "crazyegg.com",
            "mouseflow.com", "quantserve.com", "statcounter.com", "histats.com"
        };
        for (String d : domains) {
            AD_HOSTS.add(d.toLowerCase());
        }
    }

    public static boolean isAdOrRedirect(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return false;
        }

        // 1. Check direct scheme filters (data/blob ads or javascript popups)
        String lowerUrl = urlString.toLowerCase();
        if (lowerUrl.startsWith("intent:") || lowerUrl.startsWith("market:") || lowerUrl.startsWith("about:blank#blocked")) {
            return true;
        }

        // 2. Check host against blocklist
        try {
            String host = getHost(urlString);
            if (isAdHost(host)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // 3. Check URL path patterns
        return AD_URL_PATTERNS.matcher(urlString).matches();
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
}
