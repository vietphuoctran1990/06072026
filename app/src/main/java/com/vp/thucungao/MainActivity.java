package com.vp.thucungao;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

/**
 * Thú Cưng Ảo — bản native, vẽ thuần Canvas (không WebView).
 * Activity chỉ nhận phím từ remote và chuyển cho GameView.
 */
public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int k = -1;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: k = GameView.K_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: k = GameView.K_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: k = GameView.K_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: k = GameView.K_RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A: k = GameView.K_OK; break;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BUTTON_B: k = GameView.K_BACK; break;
            default: return super.onKeyDown(keyCode, event);
        }
        boolean handled = gameView.key(k);
        if (gameView.exitRequested) { finish(); return true; }
        if (k == GameView.K_BACK && !handled) { finish(); return true; }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.saveAll();
    }
}
