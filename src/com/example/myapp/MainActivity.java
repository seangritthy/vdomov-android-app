package com.example.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://www.vdomov.com";
    private static final String ALLOWED_DOMAIN = "vdomov.com";
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        try {
            AdBlocker.init(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webSettings.setSupportMultipleWindows(false);
            webSettings.setUserAgentString(webSettings.getUserAgentString() + " OriginGuardStrongShield/2.0");
        } catch (Exception e) {
            e.printStackTrace();
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    return handleUrlNavigationWithPrompt(view, request.getUrl().toString());
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlNavigationWithPrompt(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (AdBlocker.isAdOrRedirect(url)) {
                        return AdBlocker.createEmptyResource();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (AdBlocker.isAdOrRedirect(url)) {
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

        try {
            AppUpdater.checkForUpdates(this, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handleUrlNavigationWithPrompt(final WebView view, final String targetUrl) {
        if (targetUrl == null) return true;

        // 1. Immediately block known ad/tracker domains
        if (AdBlocker.isAdOrRedirect(targetUrl)) {
            return true;
        }

        // 2. Block app store / intent links
        String lowerUrl = targetUrl.toLowerCase();
        if (lowerUrl.startsWith("intent:") || lowerUrl.startsWith("market:") || lowerUrl.startsWith("play.google.com")) {
            return true;
        }

        // 3. Check if target URL belongs to main domain
        try {
            Uri targetUri = Uri.parse(targetUrl);
            String currentUrl = view.getUrl();
            String targetHost = targetUri.getHost();

            // If same host, navigate directly
            if (targetHost != null && targetHost.toLowerCase().contains(ALLOWED_DOMAIN)) {
                return false; // Let WebView load it normally
            }

            // External or redirect attempt: Ask user first via popup!
            showRedirectConfirmationDialog(view, targetUrl);
            return true; // Cancel automatic redirect until user confirms

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showRedirectConfirmationDialog(final WebView view, final String targetUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("External Redirect Alert")
                        .setMessage("The webpage is attempting to redirect or open an external link:\n\n" + targetUrl + "\n\nDo you want to proceed?")
                        .setPositiveButton("Open Link", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                view.loadUrl(targetUrl);
                            }
                        })
                        .setNegativeButton("Block & Cancel", null)
                        .setCancelable(true)
                        .show();
            }
        });
    }

    private void injectAntiRedirectShield(WebView view) {
        try {
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
        } catch (Exception ignored) {
        }
    }

    private void injectAdBlockCSS(WebView view) {
        try {
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
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView != null && webView.canGoBack()) {
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
