package com.example.myapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://www.vdomov.com";
    private static final String ALLOWED_DOMAIN = "vdomov.com";
    private WebView webView;
    private ProgressBar progressBar;
    private final AtomicInteger blockedCount = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Strict anti-redirect and anti-popup flags
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " OriginGuardStrongShield/2.0");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    return handleUrlNavigation(view, url);
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlNavigation(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (AdBlocker.isAdOrRedirect(url)) {
                        blockedCount.incrementAndGet();
                        return AdBlocker.createEmptyResource();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (AdBlocker.isAdOrRedirect(url)) {
                    blockedCount.incrementAndGet();
                    return AdBlocker.createEmptyResource();
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                injectAntiRedirectShield(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                injectAntiRedirectShield(view);
                injectAdBlockCSS(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                return false;
            }
        });

        webView.loadUrl(TARGET_URL);

        // Auto-check for new app updates in background
        AppUpdater.checkForUpdates(this, false);
    }

    private boolean handleUrlNavigation(WebView view, String url) {
        if (url == null) return true;

        if (AdBlocker.isAdOrRedirect(url)) {
            blockedCount.incrementAndGet();
            return true;
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith("intent:") || lowerUrl.startsWith("market:") || lowerUrl.startsWith("play.google.com")) {
            return true;
        }

        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host != null && host.toLowerCase().contains(ALLOWED_DOMAIN)) {
                view.loadUrl(url);
                return true;
            }
        } catch (Exception ignored) {
        }

        view.loadUrl(url);
        return true;
    }

    private void injectAntiRedirectShield(WebView view) {
        String jsShield = "javascript:(function() { " +
                "window.open = function() { console.log('Blocked popup window.open'); return null; }; " +
                "window.alert = function() {}; " +
                "window.confirm = function() { return true; }; " +
                "var originalAssign = window.location.assign; " +
                "window.onbeforeunload = null; " +
                "document.addEventListener('click', function(e) { " +
                "  var target = e.target; " +
                "  while (target && target.tagName !== 'A') { target = target.parentElement; } " +
                "  if (target && target.target === '_blank') { target.target = '_self'; } " +
                "}, true); " +
                "})()";
        view.evaluateJavascript(jsShield, null);
    }

    private void injectAdBlockCSS(WebView view) {
        String cssHideRules = "iframe[src*='ad'], .adsbygoogle, .ad-banner, [id*='google_ads'], " +
                "[class*='ad-container'], [class*='sponsored'], [class*='popup'], [id*='pop-'], " +
                "div[style*='position: fixed'][style*='z-index: 99999'], " +
                "div[style*='position:absolute'][style*='z-index: 9999'] { " +
                "display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; }";
        
        String js = "javascript:(function() { " +
                "var style = document.createElement('style'); " +
                "style.type = 'text/css'; " +
                "style.appendChild(document.createTextNode('" + cssHideRules + "')); " +
                "document.head.appendChild(style); " +
                "})()";
        view.evaluateJavascript(js, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
