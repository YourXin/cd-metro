package cn.chengdurail.cdmetro.h5;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String APP_ORIGIN = "https://cdmetro.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure solid white status bar with dark icons (prevents header overlap)
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Create root layout that reserves exact status bar height
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.WHITE);

        // Standard status bar height fallback
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
        rootLayout.setPadding(0, statusBarHeight, 0, 0);

        // Dynamic WindowInsets listener for Android 11 ~ 16+ edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
                int top = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    top = insets.getInsets(WindowInsets.Type.statusBars()).top;
                } else {
                    top = insets.getSystemWindowInsetTop();
                }
                if (top > 0) {
                    view.setPadding(0, top, 0, 0);
                }
                // Consume top inset so child WebView does not duplicate status bar padding
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return insets.inset(0, top, 0, 0);
                } else {
                    return insets.replaceSystemWindowInsets(
                            insets.getSystemWindowInsetLeft(),
                            0,
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom()
                    );
                }
            });
        }

        webView = new WebView(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            webView.setOnApplyWindowInsetsListener((view, insets) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    int top = insets.getInsets(WindowInsets.Type.statusBars()).top;
                    return insets.inset(0, top, 0, 0);
                } else {
                    return insets.replaceSystemWindowInsets(
                            insets.getSystemWindowInsetLeft(),
                            0,
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom()
                    );
                }
            });
        }
        rootLayout.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(rootLayout);

        // Android 13+ / 14 / 15 / 16 Predictive Back Gesture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () -> {
                    if (webView != null && webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                }
            );
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " CDMetroNative/1.0.4");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String urlStr = request.getUrl().toString();

                // 1. Intercept API requests -> Forward directly to Official Chengdu Metro API natively (0 CORS, 50ms)
                if (urlStr.contains("/api/") || urlStr.contains("app.cdmetro.chengdurail.cn/op/")) {
                    try {
                        String targetUrl;
                        if (urlStr.contains("app.cdmetro.chengdurail.cn/op/")) {
                            targetUrl = urlStr;
                        } else {
                            int apiIdx = urlStr.indexOf("/api/");
                            String apiSubPath = urlStr.substring(apiIdx + 5);
                            targetUrl = "https://app.cdmetro.chengdurail.cn/op/" + apiSubPath;
                        }

                        URL target = new URL(targetUrl);
                        HttpURLConnection conn = (HttpURLConnection) target.openConnection();
                        conn.setRequestMethod(request.getMethod());
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36");

                        int statusCode = conn.getResponseCode();
                        InputStream stream = statusCode < 400 ? conn.getInputStream() : conn.getErrorStream();

                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                        headers.put("Content-Type", "application/json; charset=utf-8");

                        return new WebResourceResponse(
                                "application/json",
                                "UTF-8",
                                statusCode,
                                conn.getResponseMessage(),
                                headers,
                                stream
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 2. Intercept local assets from https://cdmetro.app/* (zero network, instant offline loading)
                if (urlStr.startsWith(APP_ORIGIN)) {
                    String path = urlStr.substring(APP_ORIGIN.length());
                    if (path.startsWith("/")) path = path.substring(1);
                    if (path.isEmpty() || path.startsWith("?")) path = "index.html";
                    if (path.contains("?")) path = path.substring(0, path.indexOf("?"));

                    try {
                        InputStream is = getAssets().open(path);
                        String mime = "text/html";
                        if (path.endsWith(".json")) mime = "application/json";
                        else if (path.endsWith(".js")) mime = "application/javascript";
                        else if (path.endsWith(".css")) mime = "text/css";
                        else if (path.endsWith(".png")) mime = "image/png";

                        Map<String, String> headers = new HashMap<>();
                        headers.put("Access-Control-Allow-Origin", "*");
                        return new WebResourceResponse(mime, "UTF-8", 200, "OK", headers, is);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }
        });

        // Load through virtual HTTPS domain so fetch() and all modern browser APIs work flawlessly!
        webView.loadUrl(APP_ORIGIN + "/index.html");
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
