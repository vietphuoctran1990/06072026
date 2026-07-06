package com.vp.thucungao;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        // Ghim tỉ lệ 1:1 — không dùng wide viewport / overview mode vì WebView
        // trên một số TV xử lý sai khiến trang bị phóng to lệch khỏi màn hình.
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        webView.setInitialScale(0);
        webView.setBackgroundColor(0xFF12224A);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new TvBridge(), "AndroidTV");

        // Nền cửa sổ đen — không dùng cờ immersive (một số firmware TV
        // xử lý sai các cờ này khiến cửa sổ bị dịch lệch khỏi màn hình).
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        webView.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Give the game a chance to navigate back internally.
            webView.evaluateJavascript(
                    "(window.handleBack ? window.handleBack() : false)",
                    value -> {
                        if (!"true".equals(value)) finish();
                    });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.evaluateJavascript("window.gamePause && window.gamePause()", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.evaluateJavascript("window.gameResume && window.gameResume()", null);
    }

    private class TvBridge {
        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(MainActivity.this::finish);
        }
    }
}
