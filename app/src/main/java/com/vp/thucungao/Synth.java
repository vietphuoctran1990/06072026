package com.vp.thucungao;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bộ tổng hợp âm thanh nhỏ — sinh tiếng bíp/giai điệu vui cho game. */
public class Synth {
    private static final int RATE = 22050;
    private static final ScheduledThreadPoolExecutor POOL = new ScheduledThreadPoolExecutor(2);

    /** Phát 1 nốt: tần số (Hz), độ dài (s), âm lượng 0..1, trễ (s), dạng sóng 0=sin 1=tam giác 2=vuông. */
    public static void tone(final double freq, final double dur, final double vol,
                            final double delay, final int wave) {
        POOL.schedule(new Runnable() {
            @Override public void run() {
                try { playBuf(freq, dur, vol, wave); } catch (Throwable ignored) {}
            }
        }, (long) (delay * 1000), TimeUnit.MILLISECONDS);
    }

    private static void playBuf(double freq, double dur, double vol, int wave) {
        int n = (int) (RATE * dur);
        if (n < 16) n = 16;
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double ph = 2 * Math.PI * freq * t;
            double v;
            if (wave == 2) v = Math.signum(Math.sin(ph)) * 0.55;
            else if (wave == 1) v = 2.0 / Math.PI * Math.asin(Math.sin(ph));
            else v = Math.sin(ph);
            double env = Math.min(1, (n - i) / (RATE * 0.05)) * Math.min(1, i / (RATE * 0.008));
            buf[i] = (short) (v * env * vol * 32767 * 0.5);
        }
        final AudioTrack tr = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                buf.length * 2, AudioTrack.MODE_STATIC);
        tr.write(buf, 0, buf.length);
        tr.play();
        POOL.schedule(new Runnable() {
            @Override public void run() { try { tr.release(); } catch (Throwable ignored) {} }
        }, (long) (dur * 1000) + 120, TimeUnit.MILLISECONDS);
    }

    public static void play(String name) {
        double[][] seq;
        int wave = 1;
        switch (name) {
            case "move":   tone(520, .07, .35, 0, 2); return;
            case "select": tone(660, .09, .7, 0, 1); tone(990, .12, .7, .07, 1); return;
            case "no":     tone(180, .18, .6, 0, 2); return;
            case "eat":    tone(220, .08, .7, 0, 2); tone(150, .08, .7, .12, 2); tone(200, .1, .7, .24, 2); return;
            case "coin":   tone(880, .08, .5, 0, 2); tone(1318, .16, .5, .08, 2); return;
            case "happy":  seq = new double[][]{{523,0},{659,.09},{784,.18},{1046,.27}}; break;
            case "sad":    tone(392, .18, .7, 0, 1); tone(311, .3, .7, .18, 1); return;
            case "wash":   seq = new double[][]{{900,0},{1100,.12},{1000,.24},{1250,.36}}; wave = 0; break;
            case "sleep":  seq = new double[][]{{660,0},{550,.3},{440,.6},{330,.9}}; wave = 0; break;
            case "levelup":seq = new double[][]{{523,0},{659,.1},{784,.2},{1046,.3},{1318,.4}}; break;
            case "evolve": seq = new double[][]{{392,0},{523,.13},{659,.26},{784,.39},{1046,.52},{784,.65},{1046,.78},{1318,.91},{1568,1.04}}; break;
            case "win":    seq = new double[][]{{784,0},{784,.11},{1046,.22},{1318,.33},{1568,.44}}; break;
            case "wrong":  tone(160, .35, .7, 0, 2); return;
            case "catch":  tone(740, .07, .5, 0, 2); tone(988, .1, .5, .05, 2); return;
            case "rock":   tone(120, .2, .8, 0, 2); return;
            case "pad0":   tone(523, .3, .9, 0, 1); return;
            case "pad1":   tone(659, .3, .9, 0, 1); return;
            case "pad2":   tone(784, .3, .9, 0, 1); return;
            case "pad3":   tone(440, .3, .9, 0, 1); return;
            case "warm":   tone(620, .07, .7, 0, 0); tone(930, .1, .6, .06, 0); return;
            case "crack":  tone(300, .06, .8, 0, 2); tone(180, .1, .7, .07, 2); return;
            default: return;
        }
        for (double[] nt : seq) tone(nt[0], .15, .8, nt[1], wave);
    }
}
