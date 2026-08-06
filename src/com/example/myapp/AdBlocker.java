package com.example.myapp;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {
    private static final Set<String> AD_HOSTS = new HashSet<>();

    static {
        // High-frequency Ad, Tracker, and Analytics domains
        String[] defaultAdDomains = new String[]{
            "doubleclick.net",
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "adservice.google.com",
            "googlesyndication.com",
            "google-analytics.com",
            "googletagmanager.com",
            "googletagservices.com",
            "adnxs.com",
            "amazon-adsystem.com",
            "adroll.com",
            "criteo.com",
            "outbrain.com",
            "taboola.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "exoclick.com",
            "juicyads.com",
            "adform.net",
            "casalemedia.com",
            "scorecardresearch.com",
            "zedo.com",
            "yandex.ru/ads",
            "an.yandex.ru",
            "mc.yandex.ru",
            "adfox.ru",
            "ad.mail.ru",
            "target.my.com"
        };
        for (String domain : defaultAdDomains) {
            AD_HOSTS.add(domain.toLowerCase());
        }
    }

    public static boolean isAd(String urlString) {
        try {
            return isAdHost(getHost(urlString));
        } catch (MalformedURLException e) {
            return false;
        }
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
