package com.example.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://www.vdomov.com";
    private static final String ALLOWED_DOMAIN = "vdomov.com";
    private WebView webView;
    private ProgressBar progressBar;
    private ProgressBar horizontalProgressBar;
    private FrameLayout fullscreenContainer;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        horizontalProgressBar = findViewById(R.id.horizontalProgressBar);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        // TV Remote D-Pad Focus configuration
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);

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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                webSettings.setMediaPlaybackRequiresUserGesture(false);
            }
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            // Enable Cookie & Storage persistence
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
            }
            
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webSettings.setSupportMultipleWindows(false);
            webSettings.setUserAgentString(webSettings.getUserAgentString() + " OriginGuardStrongShield/3.0 AndroidTV VDOmov");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // File Download Handler
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).getLastPathSegment());
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Downloading file...", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

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
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (horizontalProgressBar != null) horizontalProgressBar.setVisibility(View.VISIBLE);
                injectAntiRedirectShield(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (horizontalProgressBar != null) horizontalProgressBar.setVisibility(View.GONE);
                injectAntiRedirectShield(view);
                injectAdBlockCSS(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (horizontalProgressBar != null) {
                    horizontalProgressBar.setProgress(newProgress);
                }
                if (newProgress == 100) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (horizontalProgressBar != null) horizontalProgressBar.setVisibility(View.GONE);
                } else {
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    if (horizontalProgressBar != null) horizontalProgressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                return false;
            }

            // HTML5 Fullscreen Video Support
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                if (fullscreenContainer != null) {
                    fullscreenContainer.addView(view);
                    fullscreenContainer.setVisibility(View.VISIBLE);
                }
                if (webView != null) {
                    webView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onHideCustomView() {
                onHideCustomViewInternal();
            }
        });

        webView.loadUrl(TARGET_URL);

        try {
            AppUpdater.checkForUpdates(this, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            AppUpdater.checkResumeInstall(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onHideCustomViewInternal() {
        if (customView == null) return;
        if (fullscreenContainer != null) {
            fullscreenContainer.removeView(customView);
            fullscreenContainer.setVisibility(View.GONE);
        }
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
        }
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
        }
        customView = null;
        customViewCallback = null;
    }

    private boolean handleUrlNavigationWithPrompt(final WebView view, final String targetUrl) {
        if (targetUrl == null) return true;

        if (AdBlocker.isAdOrRedirect(targetUrl)) {
            return true;
        }

        String lowerUrl = targetUrl.toLowerCase();
        if (lowerUrl.startsWith("intent:") || lowerUrl.startsWith("market:") || lowerUrl.startsWith("play.google.com")) {
            return true;
        }

        try {
            Uri targetUri = Uri.parse(targetUrl);
            String targetHost = targetUri.getHost();

            if (targetHost != null && targetHost.toLowerCase().contains(ALLOWED_DOMAIN)) {
                return false;
            }

            showRedirectConfirmationDialog(view, targetUrl);
            return true;

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
                        .setTitle("External Link Shield")
                        .setMessage("VDOmov OriginGuard Shield detected an external link:\n\n" + targetUrl + "\n\nDo you want to proceed?")
                        .setPositiveButton("Open External Link", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                view.loadUrl(targetUrl);
                            }
                        })
                        .setNegativeButton("Block & Stay", null)
                        .setCancelable(true)
                        .show();
            }
        });
    }

    private void injectAntiRedirectShield(WebView view) {
        try {
            view.evaluateJavascript(AdBlocker.getAntiRedirectShieldScript(), null);
        } catch (Exception ignored) {
        }
    }

    private void injectAdBlockCSS(WebView view) {
        try {
            String cssHideRules = AdBlocker.getAdBlockCSS();
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customView != null) {
                onHideCustomViewInternal();
                return true;
            }
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            onHideCustomViewInternal();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
