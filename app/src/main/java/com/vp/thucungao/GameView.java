package com.vp.thucungao;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Thú Cưng Ảo — game nuôi thú cho Android TV, vẽ thuần Canvas (không WebView).
 * Toạ độ thiết kế 1600x900, tự scale + căn giữa vào màn hình thật.
 */
public class GameView extends View {

    /* ---- phím ---- */
    public static final int K_UP = 0, K_RIGHT = 1, K_DOWN = 2, K_LEFT = 3, K_OK = 4, K_BACK = 5;

    /* ---- màn hình ---- */
    private static final int SC_TITLE = 0, SC_PICK = 1, SC_EGG = 2, SC_HOME = 3,
            SC_FRUIT = 4, SC_MEMORY = 5, SC_EVO = 6, SC_HATCH = 7, SC_CALIB = 8;

    private static final float DW = 1600f, DH = 900f;

    /* ---- dữ liệu loài ---- */
    private static final String[][] STAGE_NAMES = {
            {"Lửa Nhí", "Hỏa Hổ", "Hỏa Long"},
            {"Giọt Nhí", "Thủy Ngư", "Thủy Long"},
            {"Mầm Nhí", "Lá Thỏ", "Rồng Lá"}};
    private static final String[] SPECIES_EL = {"🔥 Hệ Lửa", "💧 Hệ Nước", "🍀 Hệ Lá"};
    private static final String[] EGG_NAMES = {"Trứng Lửa", "Trứng Nước", "Trứng Lá"};
    // body1, body2, belly, accent, dark
    private static final int[][] SP_COLORS = {
            {0xFFFFB35C, 0xFFFF6B5C, 0xFFFFE9C2, 0xFFFF3D2E, 0xFFB33A2B},
            {0xFF79D2FF, 0xFF3E8EF0, 0xFFE2F6FF, 0xFF2E6BD6, 0xFF22509E},
            {0xFF9CE86B, 0xFF4FBF4F, 0xFFEDFBD8, 0xFF2E9E4F, 0xFF237A3C}};

    private static final String[] FOOD_ICON = {"🍎", "🍚", "🥛", "🍰", "🍬"};
    private static final String[] FOOD_NAME = {"Táo", "Cơm", "Sữa", "Bánh kem", "Kẹo"};
    private static final int[] FOOD_COST = {4, 10, 8, 14, 6};
    private static final int[] FOOD_HUNGER = {15, 40, 22, 30, 8};
    private static final int[] FOOD_HAPPY = {2, 3, 4, 12, 10};

    private static final String[] CHAT_IDLE = {"Tớ yêu cậu lắm! 💖", "Chơi với tớ nhé!",
            "Hôm nay vui quá!", "Cậu giỏi nhất! ⭐", "Hihi~ 😊", "Mình là bạn thân nhé!"};

    /* ---- trạng thái lưu ---- */
    private int species = 1, level = 1, xp = 0, coins = 40;
    private float hunger = 80, happy = 80, energy = 90, clean = 90, warm = 0;
    private boolean egg = true, hasSave = false;
    private long lastSeen = 0;
    private String lastGift = "";
    /* căn chỉnh màn hình */
    private float calS = 1f, calDx = 0, calDy = 0;

    /* ---- trạng thái phiên ---- */
    private int screen = SC_TITLE;
    private int focus = 0;
    private long lastNs = 0;
    private double now = 0; // giây trong phiên
    private final Random rnd = new Random();

    /* hộp thoại */
    private String dlgTitle = null;
    private String[] dlgLines, dlgBtns;
    private int[] dlgActs;
    private int dlgFocus = 0;
    /* overlay menu thức ăn / chọn game */
    private boolean ovFood = false, ovPlay = false;
    private int ovFocus = 0;

    /* hiệu ứng */
    private static class Fx { float x, y; double t0; String txt; int kind; float a, b; }
    private final ArrayList<Fx> fxs = new ArrayList<>();
    private String chat = null; private double chatUntil = 0, nextChat = 6;
    private boolean sleeping = false, washing = false;
    private double actTimer = 0;
    private double petAnimT = -9; private int petAnim = 0; // 1 ăn 2 nhảy 3 lắc

    /* mini game hứng trái cây */
    private static class Item { float x, y, v; int type; } // type -1 = đá, >=0 = trái cây
    private final ArrayList<Item> items = new ArrayList<>();
    private static final String[] FRUITS = {"🍎", "🍌", "🍇", "🍓", "🍊", "🍉"};
    private float frX, frTx; private int frScore; private double frTime, frSpawn, frStun;
    private boolean frOver = false;

    /* mini game nhớ màu */
    private final ArrayList<Integer> seq = new ArrayList<>();
    private int memInput, memRound, memCoins, litPad = -1;
    private boolean memShowing = true, memOver = false;
    private double memT; private int memStep; private String memMsg = "";
    private double litUntil = 0;

    /* tiến hóa / nở trứng */
    private int evoStage = 0;
    private static class Conf { float x, vy, rot, vr; double t0; String e; }
    private final ArrayList<Conf> confs = new ArrayList<>();

    /* căn chỉnh */
    private int calMode = 0;

    /* paints dùng lại */
    private final Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ps = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Path path = new Path();

    private final SharedPreferences prefs;

    public GameView(Context ctx) {
        super(ctx);
        prefs = ctx.getSharedPreferences("thucungao", Context.MODE_PRIVATE);
        ps.setStyle(Paint.Style.STROKE);
        pt.setTypeface(Typeface.DEFAULT_BOLD);
        loadAll();
        setBackgroundColor(0xFF000000);
    }

    /* ================= LƯU / TẢI ================= */
    private void loadAll() {
        hasSave = prefs.getBoolean("hasSave", false);
        species = prefs.getInt("species", 1);
        level = prefs.getInt("level", 1);
        xp = prefs.getInt("xp", 0);
        coins = prefs.getInt("coins", 40);
        hunger = prefs.getFloat("hunger", 80);
        happy = prefs.getFloat("happy", 80);
        energy = prefs.getFloat("energy", 90);
        clean = prefs.getFloat("clean", 90);
        egg = prefs.getBoolean("egg", true);
        warm = prefs.getFloat("warm", 0);
        lastSeen = prefs.getLong("lastSeen", 0);
        lastGift = prefs.getString("lastGift", "");
        calS = prefs.getFloat("calS", 1f);
        calDx = prefs.getFloat("calDx", 0);
        calDy = prefs.getFloat("calDy", 0);
        if (calS < 0.4f || calS > 1.4f) calS = 1f;
    }

    public void saveAll() {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("hasSave", hasSave);
        e.putInt("species", species).putInt("level", level).putInt("xp", xp).putInt("coins", coins);
        e.putFloat("hunger", hunger).putFloat("happy", happy).putFloat("energy", energy).putFloat("clean", clean);
        e.putBoolean("egg", egg).putFloat("warm", warm);
        e.putLong("lastSeen", System.currentTimeMillis());
        e.putString("lastGift", lastGift);
        e.putFloat("calS", calS).putFloat("calDx", calDx).putFloat("calDy", calDy);
        e.apply();
    }

    private void newPet(int sp) {
        species = sp; level = 1; xp = 0; coins = 40;
        hunger = 80; happy = 80; energy = 90; clean = 90;
        egg = true; warm = 0; hasSave = true;
        saveAll();
    }

    /* ================= TIỆN ÍCH ================= */
    private static float clampF(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private int stageOf() { return level >= 10 ? 2 : level >= 5 ? 1 : 0; }
    private int xpNeed() { return 40 + level * 25; }
    private String petName() { return STAGE_NAMES[species][stageOf()]; }
    private int mood() { // 0 vui 1 thường 2 buồn
        float avg = (hunger + happy + energy + clean) / 4f;
        return avg >= 60 ? 0 : avg >= 32 ? 1 : 2;
    }
    private boolean isNight() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= 18 || h < 6;
    }
    private void floatFx(String txt) {
        Fx f = new Fx(); f.x = 800; f.y = 330; f.t0 = now; f.txt = txt; f.kind = 0;
        fxs.add(f);
    }

    /* ================= PHÍM ================= */
    /** Trả về true nếu đã xử lý (riêng BACK: false = cho phép thoát app). */
    public boolean key(int k) {
        if (dlgTitle != null) { keyDialog(k); return true; }
        if (ovFood) { keyFood(k); return true; }
        if (ovPlay) { keyPlay(k); return true; }
        switch (screen) {
            case SC_TITLE: return keyTitle(k);
            case SC_PICK: keyPick(k); return true;
            case SC_EGG: keyEgg(k); return true;
            case SC_HOME: keyHome(k); return true;
            case SC_FRUIT: keyFruit(k); return true;
            case SC_MEMORY: keyMemory(k); return true;
            case SC_EVO: case SC_HATCH:
                if (k == K_OK || k == K_BACK) { Synth.play("select"); saveAll(); goHome(); }
                return true;
            case SC_CALIB: keyCalib(k); return true;
        }
        return true;
    }

    /* ---------- hộp thoại ---------- */
    private void dialog(String title, String[] lines, String[] btns, int[] acts) {
        dlgTitle = title; dlgLines = lines; dlgBtns = btns; dlgActs = acts; dlgFocus = 0;
    }
    private void keyDialog(int k) {
        if (k == K_LEFT || k == K_UP) { if (dlgFocus > 0) { dlgFocus--; Synth.play("move"); } }
        else if (k == K_RIGHT || k == K_DOWN) { if (dlgFocus < dlgBtns.length - 1) { dlgFocus++; Synth.play("move"); } }
        else if (k == K_OK) { int act = dlgActs[dlgFocus]; dlgTitle = null; Synth.play("select"); doAct(act); }
        else if (k == K_BACK) { dlgTitle = null; }
    }
    private static final int A_NONE = 0, A_PICK = 1, A_EXIT = 2, A_TITLE = 3, A_ADOPT = 4,
            A_FRUIT = 5, A_MEMORY = 6, A_HOME = 7, A_FRUIT_END = 8, A_MEM_END = 9,
            A_FRUIT_RESUME = 10, A_MEM_RESUME = 11;
    private int pickSp = 1;
    private void doAct(int a) {
        switch (a) {
            case A_PICK: screen = SC_PICK; focus = 1; break;
            case A_EXIT: saveAll(); exitRequested = true; break;
            case A_TITLE: saveAll(); goTitle(); break;
            case A_ADOPT: newPet(pickSp); Synth.play("win"); screen = SC_EGG; break;
            case A_FRUIT: startFruit(); break;
            case A_MEMORY: startMemory(); break;
            case A_HOME: goHome(); break;
            case A_FRUIT_END: finishFruit(); break;
            case A_MEM_END: finishMemory(false); break;
            case A_FRUIT_RESUME: break;
            case A_MEM_RESUME: memShowing = true; memStep = 0; memT = now + 0.6; memInput = 0; break;
            default: break;
        }
    }
    public boolean exitRequested = false;

    /* ---------- tiêu đề ---------- */
    private int titleCount() { return hasSave ? 4 : 3; }
    private boolean keyTitle(int k) {
        int n = titleCount();
        if (k == K_UP || k == K_LEFT) { if (focus > 0) { focus--; Synth.play("move"); } return true; }
        if (k == K_DOWN || k == K_RIGHT) { if (focus < n - 1) { focus++; Synth.play("move"); } return true; }
        if (k == K_OK) {
            Synth.play("select");
            int i = focus + (hasSave ? 0 : 1); // 0 tiếp tục, 1 mới, 2 căn, 3 thoát
            if (i == 0) { // chơi tiếp
                loadAll();
                if (egg) screen = SC_EGG;
                else { applyOffline(); goHome(); dailyGift(); }
            } else if (i == 1) {
                if (hasSave) dialog("🐣 Nuôi thú mới?",
                        new String[]{"Bé thú hiện tại sẽ tạm biệt em đó!", "Em chắc chưa?"},
                        new String[]{"Quay lại", "Nuôi thú mới!"}, new int[]{A_NONE, A_PICK});
                else doAct(A_PICK);
            } else if (i == 2) { screen = SC_CALIB; calMode = 0; }
            else doAct(A_EXIT);
            return true;
        }
        if (k == K_BACK) return false; // thoát app
        return true;
    }
    private void goTitle() { screen = SC_TITLE; focus = 0; chat = null; }
    private void goHome() {
        if (egg) { screen = SC_EGG; return; }
        screen = SC_HOME; focus = 0; ovFood = false; ovPlay = false;
        sleeping = false; washing = false;
        nextChat = now + 4;
    }

    /* ---------- chọn trứng ---------- */
    private void keyPick(int k) {
        if (k == K_LEFT) { if (focus > 0) { focus--; Synth.play("move"); } }
        else if (k == K_RIGHT) { if (focus < 2) { focus++; Synth.play("move"); } }
        else if (k == K_OK) {
            pickSp = focus;
            Synth.play("happy");
            dialog("🥚 " + EGG_NAMES[focus],
                    new String[]{"Hãy ấp và vuốt ve để trứng", "nở ra bé thú nhé!"},
                    new String[]{"💖 Nhận trứng!", "Chọn lại"}, new int[]{A_ADOPT, A_NONE});
        } else if (k == K_BACK) goTitle();
    }

    /* ---------- ấp trứng ---------- */
    private int crackOf() { return warm >= 75 ? 2 : warm >= 40 ? 1 : 0; }
    private double eggWob = -9;
    private void keyEgg(int k) {
        if (k == K_OK) {
            if (warm >= 100) return;
            int before = crackOf();
            warm = clampF(warm + 7 + rnd.nextFloat() * 5, 0, 100);
            eggWob = now;
            if (crackOf() != before) {
                Synth.play("crack");
                floatFx(crackOf() == 1 ? "✨ Có vết nứt rồi!" : "💥 Sắp nở rồi!!");
            } else {
                Synth.play("warm");
                if (rnd.nextFloat() < 0.35f) floatFx(new String[]{"💖", "🔥", "✨", "🥰"}[rnd.nextInt(4)]);
            }
            saveAll();
            if (warm >= 100) { hatchAt = now + 0.8; }
        } else if (k == K_BACK) {
            dialog("🚪 Tạm biệt?", new String[]{"Quả trứng sẽ chờ em quay lại ấp tiếp đó!"},
                    new String[]{"💖 Ấp tiếp", "👋 Màn hình chính", "🚪 Tắt game"},
                    new int[]{A_NONE, A_TITLE, A_EXIT});
        }
    }
    private double hatchAt = -1;

    private void startHatch() {
        egg = false; coins += 20; saveAll();
        Synth.play("evolve");
        screen = SC_HATCH;
        spawnConfetti();
    }
    private void spawnConfetti() {
        confs.clear();
        String[] emo = {"🎉", "⭐", "✨", "🎊", "💖", "🌟"};
        for (int i = 0; i < 30; i++) {
            Conf c = new Conf();
            c.x = rnd.nextFloat() * DW; c.vy = 180 + rnd.nextFloat() * 220;
            c.rot = rnd.nextFloat() * 360; c.vr = 120 + rnd.nextFloat() * 240;
            c.t0 = now + rnd.nextFloat() * 1.5; c.e = emo[i % emo.length];
            confs.add(c);
        }
    }

    /* ---------- nhà ---------- */
    private void keyHome(int k) {
        if (sleeping) { if (k == K_OK || k == K_BACK) wakeUp(); return; }
        if (washing) return;
        if (k == K_LEFT) { if (focus > 0) { focus--; Synth.play("move"); } }
        else if (k == K_RIGHT) { if (focus < 3) { focus++; Synth.play("move"); } }
        else if (k == K_OK) {
            Synth.play("select");
            if (focus == 0) { ovFood = true; ovFocus = 0; }
            else if (focus == 1) { ovPlay = true; ovFocus = 0; }
            else if (focus == 2) doWash();
            else doSleep();
        } else if (k == K_BACK) {
            dialog("🚪 Tạm biệt?", new String[]{"Em muốn nghỉ chơi chưa?", "Bé thú sẽ chờ em quay lại đó!"},
                    new String[]{"💖 Chơi tiếp", "👋 Màn hình chính", "🚪 Tắt game"},
                    new int[]{A_NONE, A_TITLE, A_EXIT});
        }
    }

    private void keyFood(int k) {
        if (k == K_LEFT) { if (ovFocus > 0) { ovFocus--; Synth.play("move"); } }
        else if (k == K_RIGHT) { if (ovFocus < 4) { ovFocus++; Synth.play("move"); } }
        else if (k == K_BACK) ovFood = false;
        else if (k == K_OK) {
            int i = ovFocus;
            if (coins < FOOD_COST[i]) { Synth.play("no"); floatFx("Chưa đủ xu! Chơi game kiếm xu nhé! 🎮"); return; }
            coins -= FOOD_COST[i];
            ovFood = false;
            hunger = clampF(hunger + FOOD_HUNGER[i], 0, 100);
            happy = clampF(happy + FOOD_HAPPY[i], 0, 100);
            Synth.play("eat");
            petAnim = 1; petAnimT = now;
            floatFx(FOOD_ICON[i] + " +" + FOOD_HUNGER[i] + " 🍗");
            addXp(6);
        }
    }

    private void keyPlay(int k) {
        if (k == K_LEFT) { if (ovFocus > 0) { ovFocus--; Synth.play("move"); } }
        else if (k == K_RIGHT) { if (ovFocus < 1) { ovFocus++; Synth.play("move"); } }
        else if (k == K_BACK) ovPlay = false;
        else if (k == K_OK) {
            Synth.play("select"); ovPlay = false;
            if (ovFocus == 0) startFruit(); else startMemory();
        }
    }

    private void doWash() {
        if (clean > 95) { floatFx("Đang sạch bong rồi! ✨"); return; }
        Synth.play("wash");
        washing = true; actTimer = now + 2.4;
        petAnim = 3; petAnimT = now;
        for (int i = 0; i < 12; i++) {
            Fx f = new Fx(); f.kind = 1;
            f.x = 660 + rnd.nextFloat() * 280; f.y = 480 + rnd.nextFloat() * 120;
            f.t0 = now + rnd.nextFloat() * 1.2; f.a = 14 + rnd.nextFloat() * 22;
            fxs.add(f);
        }
    }

    private void doSleep() {
        if (energy > 95) { floatFx("Khỏe re, chưa buồn ngủ! ⚡"); return; }
        Synth.play("sleep");
        sleeping = true; actTimer = now;
    }
    private void wakeUp() {
        sleeping = false;
        Synth.play("happy");
        floatFx("⚡ Khỏe khoắn!");
        addXp(5);
    }

    private void addXp(int amount) {
        xp += amount;
        boolean leveled = false;
        while (xp >= xpNeed()) {
            xp -= xpNeed();
            int oldStage = stageOf();
            level++;
            coins += level * 5;
            leveled = true;
            if (stageOf() != oldStage) {
                evoStage = stageOf();
                saveAll();
                Synth.play("evolve");
                screen = SC_EVO;
                spawnConfetti();
                return;
            }
        }
        if (leveled) { Synth.play("levelup"); floatFx("⭐ Lên cấp " + level + "!"); }
        saveAll();
    }

    private void applyOffline() {
        if (lastSeen <= 0) return;
        float mins = Math.min((System.currentTimeMillis() - lastSeen) / 60000f, 720);
        if (mins > 1) {
            hunger = clampF(hunger - mins * 0.5f, 25, 100);
            happy = clampF(happy - mins * 0.35f, 25, 100);
            energy = clampF(energy + mins * 0.8f, 0, 100);
            clean = clampF(clean - mins * 0.25f, 25, 100);
        }
    }
    private void dailyGift() {
        Calendar c = Calendar.getInstance();
        String today = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
        if (!today.equals(lastGift)) {
            lastGift = today; coins += 30; saveAll();
            Synth.play("coin");
            dialog("🎁 Quà mỗi ngày!", new String[]{"Em nhận được +30 xu!", "Hãy chăm bé thú thật tốt nhé!"},
                    new String[]{"💛 Cảm ơn!"}, new int[]{A_NONE});
        }
    }

    /* ---------- mini game: hứng trái cây ---------- */
    private void startFruit() {
        screen = SC_FRUIT; items.clear();
        frX = frTx = DW / 2; frScore = 0; frTime = 45; frSpawn = 0; frStun = 0; frOver = false;
    }
    private void keyFruit(int k) {
        if (frOver) return;
        float step = DW * 0.09f, half = 110;
        if (k == K_LEFT) frTx = clampF(frTx - step, half, DW - half);
        else if (k == K_RIGHT) frTx = clampF(frTx + step, half, DW - half);
        else if (k == K_BACK) {
            dialog("Thoát game?", new String[]{"Em muốn dừng chơi Hứng Trái Cây?"},
                    new String[]{"Chơi tiếp", "Thoát"}, new int[]{A_FRUIT_RESUME, A_FRUIT_END});
        }
    }
    private void finishFruit() {
        frOver = true;
        int c = frScore, x = 10 + Math.min(20, frScore / 2);
        coins += c;
        happy = clampF(happy + 12, 0, 100);
        energy = clampF(energy - 6, 0, 100);
        Synth.play("win");
        String praise = frScore >= 25 ? "TUYỆT VỜI! 🏆" : frScore >= 12 ? "Giỏi lắm! 🌟" : "Cố lên nhé! 💪";
        addXp(x);
        if (screen == SC_EVO) return; // tiến hóa chen ngang
        dialog("🍎 Hết giờ!", new String[]{praise, "Em hứng được " + frScore + " trái cây!",
                        "+" + c + " xu  •  +" + x + " XP"},
                new String[]{"🔁 Chơi lại", "🏠 Về nhà"}, new int[]{A_FRUIT, A_HOME});
    }

    /* ---------- mini game: nhớ màu ---------- */
    private void startMemory() {
        screen = SC_MEMORY; seq.clear();
        memInput = 0; memRound = 1; memCoins = 0; memOver = false; memMsg = "Nhìn nè!";
        newMemStep();
    }
    private void newMemStep() {
        seq.add(rnd.nextInt(4));
        memShowing = true; memStep = 0; memT = now + 0.8; memInput = 0;
    }
    private void keyMemory(int k) {
        if (memOver) return;
        if (k == K_BACK) {
            dialog("Thoát game?", new String[]{"Em muốn dừng chơi Bé Nhớ Giỏi?"},
                    new String[]{"Chơi tiếp", "Thoát"}, new int[]{A_MEM_RESUME, A_MEM_END});
            return;
        }
        if (memShowing) return;
        int dir = k == K_UP ? 0 : k == K_RIGHT ? 1 : k == K_DOWN ? 2 : k == K_LEFT ? 3 : -1;
        if (dir < 0) return;
        litPad = dir; litUntil = now + 0.26;
        Synth.play("pad" + dir);
        if (dir == seq.get(memInput)) {
            memInput++;
            if (memInput >= seq.size()) {
                memCoins += 3;
                Synth.play("coin");
                if (memRound >= 8) { finishMemory(true); return; }
                memRound++;
                memMsg = "Đúng rồi! 🎉";
                memShowing = true; memStep = -1; memT = now + 1.0;
            }
        } else {
            Synth.play("wrong");
            memMsg = "Ôi sai rồi!";
            memShowing = true; memStep = -2; memT = now + 1.2;
        }
    }
    private void finishMemory(boolean winAll) {
        memOver = true;
        int c = memCoins, x = 8 + memCoins;
        coins += c;
        happy = clampF(happy + 12, 0, 100);
        energy = clampF(energy - 6, 0, 100);
        if (winAll) Synth.play("win");
        String praise = winAll ? "SIÊU TRÍ NHỚ! 🏆👑" : memRound >= 5 ? "Trí nhớ tuyệt vời! 🌟"
                : memRound >= 3 ? "Giỏi lắm! 💪" : "Lần sau cố lên nhé! 🍀";
        addXp(x);
        if (screen == SC_EVO) return;
        dialog("🌈 Kết quả!", new String[]{praise, "Em nhớ được " + (memRound - 1) + " vòng!",
                        "+" + c + " xu  •  +" + x + " XP"},
                new String[]{"🔁 Chơi lại", "🏠 Về nhà"}, new int[]{A_MEMORY, A_HOME});
    }

    /* ---------- căn chỉnh ---------- */
    private void keyCalib(int k) {
        if (k == K_OK) { calMode = (calMode + 1) % 4; Synth.play("select"); saveAll(); return; }
        if (k == K_BACK) { saveAll(); Synth.play("select"); goTitle(); return; }
        float step = 8;
        if (calMode == 0) {
            if (k == K_LEFT) calS = Math.max(0.5f, calS - 0.02f);
            if (k == K_RIGHT) calS = Math.min(1.3f, calS + 0.02f);
        } else if (calMode == 1) {
            if (k == K_LEFT) calDx = Math.max(-800, calDx - step);
            if (k == K_RIGHT) calDx = Math.min(800, calDx + step);
        } else if (calMode == 2) {
            if (k == K_UP) calDy = Math.max(-800, calDy - step);
            if (k == K_DOWN) calDy = Math.min(800, calDy + step);
        } else if (calMode == 3 && k == K_RIGHT) { calS = 1; calDx = 0; calDy = 0; }
        Synth.play("move");
        saveAll();
    }

    /* ================= CẬP NHẬT ================= */
    private void update(double dt) {
        now += dt;
        if (screen == SC_HOME && dlgTitle == null) {
            float m = (float) (dt / 60.0);
            if (!sleeping) {
                hunger = clampF(hunger - m * 1.4f, 0, 100);
                happy = clampF(happy - m * 1.0f, 0, 100);
                energy = clampF(energy - m * 0.8f, 0, 100);
                clean = clampF(clean - m * 0.7f, 0, 100);
            } else {
                energy = clampF(energy + (float) dt * 7f, 0, 100);
                if (energy >= 100 || now - actTimer > 15) wakeUp();
            }
            if (washing && now > actTimer) {
                washing = false;
                clean = 100; happy = clampF(happy + 5, 0, 100);
                Synth.play("happy");
                floatFx("🫧 Sạch bong! ✨");
                addXp(5);
            }
            if (!sleeping && !washing && now > nextChat) {
                String[] pool = CHAT_IDLE;
                if (hunger < 35) pool = new String[]{"Tớ đói quá! 🍚", "Bụng tớ kêu ròi... 🍎"};
                else if (clean < 35) pool = new String[]{"Tắm cho tớ nhé! 🛁", "Tớ hơi bẩn rồi..."};
                else if (energy < 35) pool = new String[]{"Tớ buồn ngủ... 💤", "Cho tớ ngủ chút nha 😴"};
                else if (happy < 35) pool = new String[]{"Chơi game với tớ đi! 🎮", "Tớ buồn quá à..."};
                chat = pool[rnd.nextInt(pool.length)];
                chatUntil = now + 3.5;
                nextChat = now + 9 + rnd.nextFloat() * 8;
            }
        }
        if (screen == SC_EGG && hatchAt > 0 && now >= hatchAt) { hatchAt = -1; startHatch(); }

        if (screen == SC_FRUIT && !frOver && dlgTitle == null) {
            frTime -= dt;
            if (frTime <= 0) { finishFruit(); }
            else {
                frSpawn += dt;
                if (frSpawn > 0.75) {
                    frSpawn = 0;
                    Item it = new Item();
                    it.x = 60 + rnd.nextFloat() * (DW - 120); it.y = -60;
                    it.v = DH * 0.22f + rnd.nextFloat() * DH * 0.14f;
                    it.type = rnd.nextFloat() < 0.18f ? -1 : rnd.nextInt(FRUITS.length);
                    items.add(it);
                }
                if (frStun > 0) frStun -= dt;
                frX += (frTx - frX) * Math.min(dt * 10, 1);
                float petW = 210, py = DH - petW * 1.02f;
                for (int i = items.size() - 1; i >= 0; i--) {
                    Item it = items.get(i);
                    it.y += it.v * dt;
                    if (it.y > DH + 60) { items.remove(i); continue; }
                    if (Math.abs(it.x - frX) < petW * 0.42f && Math.abs(it.y - (py + petW * 0.45f)) < petW * 0.42f) {
                        items.remove(i);
                        if (it.type < 0) { frStun = 1.2; frScore = Math.max(0, frScore - 1); Synth.play("rock"); }
                        else if (frStun <= 0) { frScore++; Synth.play("catch"); }
                    }
                }
            }
        }

        if (screen == SC_MEMORY && memShowing && !memOver && dlgTitle == null && now >= memT) {
            if (memStep == -2) { // sai → kết thúc
                finishMemory(false);
            } else if (memStep == -1) { // vòng mới
                newMemStep(); memMsg = "Nhìn nè!";
            } else if (memStep < seq.size()) {
                litPad = seq.get(memStep);
                Synth.play("pad" + litPad);
                double gap = Math.max(0.65 - memRound * 0.04, 0.42);
                litUntil = now + gap * 0.6;
                memT = now + gap;
                memStep++;
            } else {
                memShowing = false; memMsg = "Đến em!";
            }
        }
        if (litPad >= 0 && now > litUntil) litPad = -1;

        for (int i = fxs.size() - 1; i >= 0; i--) {
            Fx f = fxs.get(i);
            double life = f.kind == 1 ? 2.4 : 1.5;
            if (now - f.t0 > life) fxs.remove(i);
        }
    }

    /* ================= VẼ ================= */
    @Override
    protected void onDraw(Canvas c) {
        long ns = System.nanoTime();
        double dt = lastNs == 0 ? 0.016 : Math.min((ns - lastNs) / 1e9, 0.05);
        lastNs = ns;
        update(dt);

        c.drawColor(0xFF000000);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) { postInvalidateOnAnimation(); return; }
        float s = Math.min(w / DW, h / DH) * calS;
        float tx = (w - DW * s) / 2f + calDx;
        float ty = (h - DH * s) / 2f + calDy;
        c.save();
        c.translate(tx, ty);
        c.scale(s, s);
        c.clipRect(0, 0, DW, DH);

        switch (screen) {
            case SC_TITLE: drawTitle(c); break;
            case SC_PICK: drawPick(c); break;
            case SC_EGG: drawEggScr(c); break;
            case SC_HOME: drawHome(c); break;
            case SC_FRUIT: drawFruit(c); break;
            case SC_MEMORY: drawMemory(c); break;
            case SC_EVO: drawEvo(c, "✨ TIẾN HÓA RỒI! ✨", STAGE_NAMES[species][evoStage], evoStage); break;
            case SC_HATCH: drawEvo(c, "🎉 TRỨNG NỞ RỒI! 🎉", "Chào em, tớ là " + STAGE_NAMES[species][0] + "!", 0); break;
            case SC_CALIB: drawCalib(c); break;
        }
        if (ovFood) drawFood(c);
        if (ovPlay) drawPlayMenu(c);
        if (dlgTitle != null) drawDialog(c);
        drawFxs(c);

        c.restore();
        postInvalidateOnAnimation();
    }

    /* ---------- vẽ tiện ích ---------- */
    private void text(Canvas c, String t, float x, float y, float size, int color, Paint.Align align) {
        pt.setTextSize(size); pt.setColor(color); pt.setTextAlign(align);
        c.drawText(t, x, y, pt);
    }
    private void textC(Canvas c, String t, float x, float y, float size, int color) {
        text(c, t, x, y, size, color, Paint.Align.CENTER);
    }
    private void rr(Canvas c, float x, float y, float w, float h, float r, int color) {
        pf.setShader(null); pf.setColor(color);
        rf.set(x, y, x + w, y + h);
        c.drawRoundRect(rf, r, r, pf);
    }
    private void button(Canvas c, float x, float y, float w, float h, String label, boolean focusd, float textSize) {
        if (focusd) {
            float g = 7;
            rr(c, x - g, y - g, w + g * 2, h + g * 2, (h + g * 2) / 2, 0xFFFFD54D);
        }
        rr(c, x, y + 5, w, h, h / 2, 0x33000000);
        rr(c, x, y, w, h, h / 2, 0xFFFFFFFF);
        textC(c, label, x + w / 2, y + h / 2 + textSize * 0.35f, textSize, 0xFF3A2E55);
    }
    private void skyGrass(Canvas c, boolean night) {
        int sky1 = night ? 0xFF20305E : 0xFF8ED9FF, sky2 = night ? 0xFF3D4E86 : 0xFFC9F0FF;
        int gr1 = night ? 0xFF3E7D4E : 0xFFA5E88B, gr2 = night ? 0xFF2C5E3B : 0xFF7BD35F;
        pf.setShader(new LinearGradient(0, 0, 0, DH * 0.52f, sky1, sky2, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, DW, DH * 0.53f, pf);
        pf.setShader(new LinearGradient(0, DH * 0.52f, 0, DH, gr1, gr2, Shader.TileMode.CLAMP));
        c.drawRect(0, DH * 0.52f, DW, DH, pf);
        pf.setShader(null);
        // mặt trời / trăng
        pf.setColor(night ? 0xFFF3EFC8 : 0xFFFFE04D);
        c.drawCircle(DW * 0.9f, DH * 0.13f, 62, pf);
        // mây trôi
        pf.setColor(0xE6FFFFFF);
        float cx = (float) ((now * 28) % (DW + 500)) - 250;
        c.drawOval(rrSet(cx, 90, 170, 52), pf);
        c.drawOval(rrSet(cx + 40, 62, 90, 55), pf);
        float cx2 = (float) ((now * 18 + 700) % (DW + 500)) - 250;
        c.drawOval(rrSet(cx2, 190, 130, 42), pf);
    }
    private RectF rrSet(float x, float y, float w, float h) { rf.set(x, y, x + w, y + h); return rf; }

    /* ---------- màn hình tiêu đề ---------- */
    private void drawTitle(Canvas c) {
        pf.setShader(new LinearGradient(0, 0, 0, DH, 0xFF7B6CF6, 0xFFFFC9E2, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, DW, DH, pf);
        pf.setShader(null);
        float bob = (float) Math.sin(now * 2) * 10;
        textC(c, "🐾 THÚ CƯNG ẢO 🐾", DW / 2, 165 + bob, 92, 0xFFFFFFFF);
        textC(c, "Nuôi bé thú đáng yêu của riêng em!", DW / 2, 235, 34, 0xFFFFF3D6);
        for (int i = 0; i < 3; i++)
            drawPet(c, i, 0, 0, false, DW / 2 + (i - 1) * 190, 350 + bob * (i == 1 ? -1 : 1), 130);
        String[] labels = hasSave
                ? new String[]{"▶️  Chơi tiếp", "🐣  Nuôi thú mới", "📐  Căn màn hình", "🚪  Thoát"}
                : new String[]{"🐣  Bắt đầu", "📐  Căn màn hình", "🚪  Thoát"};
        float bw = 480, bh = 74, by = 470;
        for (int i = 0; i < labels.length; i++)
            button(c, DW / 2 - bw / 2, by + i * (bh + 22), bw, bh, labels[i], focus == i, 36);
        textC(c, "🔼 🔽 Chọn  •  OK Xác nhận", DW / 2, DH - 28, 26, 0xE6FFFFFF);
    }

    /* ---------- chọn trứng ---------- */
    private void drawPick(Canvas c) {
        pf.setShader(new LinearGradient(0, 0, 0, DH, 0xFF59C2FF, 0xFFD9FBD0, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, DW, DH, pf);
        pf.setShader(null);
        textC(c, "🥚 Chọn quả trứng bí ẩn! 🥚", DW / 2, 130, 60, 0xFFFFFFFF);
        float cw = 400, ch = 480, cy = 210;
        for (int i = 0; i < 3; i++) {
            float cx = DW / 2 + (i - 1) * 460 - cw / 2;
            boolean f = focus == i;
            if (f) rr(c, cx - 8, cy - 8, cw + 16, ch + 16, 56, 0xFFFFD54D);
            rr(c, cx, cy + 8, cw, ch, 48, 0x33000000);
            rr(c, cx, cy, cw, ch, 48, 0xF2FFFFFF);
            drawEgg(c, i, 0, cx + cw / 2, cy + 160, 240);
            textC(c, EGG_NAMES[i], cx + cw / 2, cy + 330, 40, 0xFF3A2E55);
            textC(c, SPECIES_EL[i], cx + cw / 2, cy + 385, 32, 0xFF6A4FB6);
            textC(c, "Bé thú gì sẽ nở ra nhỉ?", cx + cw / 2, cy + 435, 26, 0xFF8A7BAF);
        }
        textC(c, "◀ ▶ Chọn  •  OK Nhận trứng  •  BACK Quay lại", DW / 2, DH - 28, 26, 0xFF2E6BA6);
    }

    /* ---------- ấp trứng ---------- */
    private void drawEggScr(Canvas c) {
        skyGrass(c, isNight());
        float wob = (now - eggWob < 0.4) ? (float) Math.sin((now - eggWob) * 24) * 6 : 0;
        c.save();
        c.rotate(wob, DW / 2, 470);
        drawEgg(c, species, crackOf(), DW / 2, 380, 420);
        c.restore();
        // thanh ấp
        float mw = 480, mx = DW / 2 - mw / 2, my = 640;
        rr(c, mx, my, mw, 110, 28, 0xE6FFFFFF);
        textC(c, "🔥 Ấp trứng: " + Math.round(warm) + "%", DW / 2, my + 42, 30, 0xFF3A2E55);
        rr(c, mx + 24, my + 62, mw - 48, 26, 13, 0x2438265E);
        pf.setShader(new LinearGradient(mx + 24, 0, mx + mw - 24, 0, 0xFFFFC94D, 0xFFFF8A3D, Shader.TileMode.CLAMP));
        rf.set(mx + 24, my + 62, mx + 24 + (mw - 48) * warm / 100f, my + 88);
        c.drawRoundRect(rf, 13, 13, pf);
        pf.setShader(null);
        textC(c, "Bấm OK thật nhiều để ấp và vuốt ve trứng! 🥚💖", DW / 2, 810, 34, 0xFFFFFFFF);
        textC(c, "OK Ấp trứng  •  BACK Thoát", DW / 2, DH - 24, 26, 0xF0FFFFFF);
    }

    /* ---------- nhà ---------- */
    private void drawHome(Canvas c) {
        boolean night = isNight() || sleeping;
        skyGrass(c, night);
        // chỉ số
        String[] icons = {"🍗", "😊", "⚡", "🫧"};
        String[] labels = {"No bụng", "Vui vẻ", "Sức khỏe", "Sạch sẽ"};
        float[] vals = {hunger, happy, energy, clean};
        for (int i = 0; i < 4; i++) {
            float bx = 40 + (i % 2) * 360, by = 34 + (i / 2) * 92;
            rr(c, bx, by, 340, 78, 22, 0xE0FFFFFF);
            int v = Math.round(vals[i]);
            text(c, icons[i] + " " + labels[i], bx + 18, by + 32, 24, 0xFF3A2E55, Paint.Align.LEFT);
            text(c, String.valueOf(v), bx + 322, by + 32, 24, 0xFF3A2E55, Paint.Align.RIGHT);
            rr(c, bx + 18, by + 46, 304, 18, 9, 0x2438265E);
            int col = v >= 60 ? 0xFF58D68D : v >= 30 ? 0xFFFFC94D : 0xFFFF6B6B;
            if (v > 0) rr(c, bx + 18, by + 46, 304 * v / 100f, 18, 9, col);
        }
        // xu + cấp + XP
        rr(c, DW - 300, 34, 260, 58, 29, 0xE6FFFFFF);
        textC(c, "🪙 " + coins + " xu", DW - 170, 74, 30, 0xFF3A2E55);
        rr(c, DW - 300, 104, 260, 58, 29, 0xE6FFFFFF);
        textC(c, "⭐ Cấp " + level, DW - 170, 144, 30, 0xFF3A2E55);
        rr(c, DW - 300, 174, 260, 40, 20, 0xE6FFFFFF);
        text(c, "XP", DW - 284, 202, 20, 0xFF3A2E55, Paint.Align.LEFT);
        rr(c, DW - 240, 184, 186, 20, 10, 0x2438265E);
        float xr = Math.min(1f, xp / (float) xpNeed());
        if (xr > 0) rr(c, DW - 240, 184, 186 * xr, 20, 10, 0xFFB77BFF);

        // thú
        int stage = stageOf();
        float size = stage == 0 ? 350 : stage == 1 ? 410 : 470;
        float bob = (float) Math.sin(now * 2.4) * 12;
        float px = DW / 2, py = 400 + bob;
        if (petAnim != 0 && now - petAnimT > 1.1) petAnim = 0;
        c.save();
        if (petAnim == 1) { float k = 1 + 0.05f * (float) Math.sin((now - petAnimT) * 18); c.scale(1 / k, k, px, py); }
        if (petAnim == 3) c.rotate((float) Math.sin((now - petAnimT) * 14) * 5, px, py);
        drawPet(c, species, stage, sleeping ? 0 : mood(), sleeping, px, py, size);
        c.restore();
        textC(c, petName(), DW / 2, 660, 38, 0xFFFFFFFF);

        if (sleeping) {
            for (int i = 0; i < 3; i++) {
                float t = (float) ((now * 0.7 + i * 0.6) % 1.8) / 1.8f;
                textC(c, "💤", px + 130 + t * 70 + i * 26, py - 120 - t * 130, 40 + t * 22, Color.argb((int) (255 * (1 - t)), 255, 255, 255));
            }
        }
        if (chat != null && now < chatUntil && !sleeping) {
            pt.setTextSize(28);
            float tw = pt.measureText(chat);
            rr(c, px + 130, py - 260, tw + 48, 62, 26, 0xFAFFFFFF);
            text(c, chat, px + 154, py - 219, 28, 0xFF3A2E55, Paint.Align.LEFT);
        }

        // hàng nút
        String[] bIcons = {"🍎", "🎮", "🛁", "💤"};
        String[] bLabels = {"Cho ăn", "Chơi game", "Tắm", "Đi ngủ"};
        float bw = 230, bh = 130, gap = 26;
        float x0 = DW / 2 - (bw * 4 + gap * 3) / 2;
        for (int i = 0; i < 4; i++) {
            float bx = x0 + i * (bw + gap), by = 700;
            boolean f = focus == i && !sleeping && !washing;
            if (f) rr(c, bx - 7, by - 7, bw + 14, bh + 14, 40, 0xFFFFD54D);
            rr(c, bx, by + 6, bw, bh, 34, 0x33234E1E);
            rr(c, bx, by, bw, bh, 34, 0xF0FFFFFF);
            textC(c, bIcons[i], bx + bw / 2, by + 62, 52, 0xFF3A2E55);
            textC(c, bLabels[i], bx + bw / 2, by + 110, 28, 0xFF3A2E55);
        }
        textC(c, "◀ ▶ Chọn  •  OK Làm  •  BACK Thoát", DW / 2, DH - 14, 24, 0xF0FFFFFF);
    }

    /* ---------- overlay: thức ăn ---------- */
    private void drawFood(Canvas c) {
        rr(c, 0, 0, DW, DH, 0, 0x8C141432);
        float pw = 1240, ph = 520, px = DW / 2 - pw / 2, py = DH / 2 - ph / 2;
        rr(c, px, py, pw, ph, 44, 0xFFFFF8E8);
        textC(c, "🍽️ Cho bé ăn gì nào?", DW / 2, py + 76, 44, 0xFF6A4FB6);
        float cw = 216, ch = 280, gap = 24;
        float x0 = DW / 2 - (cw * 5 + gap * 4) / 2, cy = py + 120;
        for (int i = 0; i < 5; i++) {
            float cx = x0 + i * (cw + gap);
            boolean locked = coins < FOOD_COST[i];
            boolean f = ovFocus == i;
            if (f) rr(c, cx - 7, cy - 7, cw + 14, ch + 14, 36, 0xFFFFD54D);
            rr(c, cx, cy + 6, cw, ch, 30, 0x2A38265E);
            rr(c, cx, cy, cw, ch, 30, locked ? 0xFFE9E4EF : 0xFFFFFFFF);
            int tcol = locked ? 0xFFB0A6C4 : 0xFF3A2E55;
            textC(c, FOOD_ICON[i], cx + cw / 2, cy + 92, 68, tcol);
            textC(c, FOOD_NAME[i], cx + cw / 2, cy + 158, 28, tcol);
            textC(c, "🪙 " + FOOD_COST[i] + " xu", cx + cw / 2, cy + 202, 24, locked ? 0xFFB0A6C4 : 0xFF8A7BAF);
            textC(c, "🍗 +" + FOOD_HUNGER[i], cx + cw / 2, cy + 244, 24, locked ? 0xFFB0A6C4 : 0xFF8A7BAF);
        }
        textC(c, "🪙 Em có " + coins + " xu  •  BACK quay lại", DW / 2, py + ph - 36, 26, 0xFF8A7BAF);
    }

    /* ---------- overlay: chọn game ---------- */
    private void drawPlayMenu(Canvas c) {
        rr(c, 0, 0, DW, DH, 0, 0x8C141432);
        float pw = 1000, ph = 520, px = DW / 2 - pw / 2, py = DH / 2 - ph / 2;
        rr(c, px, py, pw, ph, 44, 0xFFFFF8E8);
        textC(c, "🎮 Chơi gì nào?", DW / 2, py + 76, 44, 0xFF6A4FB6);
        String[][] cards = {{"🍎🧺", "Hứng Trái Cây", "◀ ▶ hứng trái cây rơi", "Né hòn đá nhé!"},
                {"🌈🧠", "Bé Nhớ Giỏi", "Nhìn màu nhấp nháy rồi", "bấm lại đúng thứ tự!"}};
        float cw = 400, ch = 290, cy = py + 120;
        for (int i = 0; i < 2; i++) {
            float cx = DW / 2 + (i == 0 ? -cw - 20 : 20);
            boolean f = ovFocus == i;
            if (f) rr(c, cx - 7, cy - 7, cw + 14, ch + 14, 36, 0xFFFFD54D);
            rr(c, cx, cy + 6, cw, ch, 30, 0x2A38265E);
            rr(c, cx, cy, cw, ch, 30, 0xFFFFFFFF);
            textC(c, cards[i][0], cx + cw / 2, cy + 96, 64, 0xFF3A2E55);
            textC(c, cards[i][1], cx + cw / 2, cy + 160, 34, 0xFF3A2E55);
            textC(c, cards[i][2], cx + cw / 2, cy + 210, 24, 0xFF8A7BAF);
            textC(c, cards[i][3], cx + cw / 2, cy + 248, 24, 0xFF8A7BAF);
        }
        textC(c, "Chơi game được XU và XP!  •  BACK quay lại", DW / 2, py + ph - 36, 26, 0xFF8A7BAF);
    }

    /* ---------- hộp thoại ---------- */
    private void drawDialog(Canvas c) {
        rr(c, 0, 0, DW, DH, 0, 0x8C141432);
        float pw = 860, ph = 200 + dlgLines.length * 44 + 110;
        float px = DW / 2 - pw / 2, py = DH / 2 - ph / 2;
        rr(c, px, py, pw, ph, 44, 0xFFFFF8E8);
        textC(c, dlgTitle, DW / 2, py + 84, 44, 0xFF6A4FB6);
        for (int i = 0; i < dlgLines.length; i++)
            textC(c, dlgLines[i], DW / 2, py + 150 + i * 44, 30, 0xFF3A2E55);
        int n = dlgBtns.length;
        float bh = 72;
        float totalW = 0;
        pt.setTextSize(28);
        float[] bws = new float[n];
        for (int i = 0; i < n; i++) { bws[i] = pt.measureText(dlgBtns[i]) + 84; totalW += bws[i]; }
        totalW += (n - 1) * 24;
        float bx = DW / 2 - totalW / 2, by = py + ph - bh - 40;
        for (int i = 0; i < n; i++) {
            button(c, bx, by, bws[i], bh, dlgBtns[i], dlgFocus == i, 28);
            bx += bws[i] + 24;
        }
    }

    /* ---------- mini game: hứng trái cây ---------- */
    private void drawFruit(Canvas c) {
        pf.setShader(new LinearGradient(0, 0, 0, DH * 0.6f, 0xFFFFD98E, 0xFFFFC46B, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, DW, DH * 0.6f, pf);
        pf.setShader(new LinearGradient(0, DH * 0.6f, 0, DH, 0xFF9CDB77, 0xFF6FC24F, Shader.TileMode.CLAMP));
        c.drawRect(0, DH * 0.6f, DW, DH, pf);
        pf.setShader(null);
        // HUD
        rr(c, 40, 30, 200, 60, 30, 0xE6FFFFFF);
        textC(c, "⏱️ " + Math.max(0, (int) frTime) + "s", 140, 71, 30, 0xFF3A2E55);
        rr(c, DW / 2 - 260, 30, 520, 60, 30, 0xE6FFFFFF);
        textC(c, "🍎🧺 Hứng Trái Cây", DW / 2, 71, 32, 0xFF3A2E55);
        rr(c, DW - 240, 30, 200, 60, 30, 0xE6FFFFFF);
        textC(c, "⭐ " + frScore, DW - 140, 71, 30, 0xFF3A2E55);
        // vật rơi
        for (Item it : items)
            textC(c, it.type < 0 ? "🪨" : FRUITS[it.type], it.x, it.y, 68, 0xFF000000);
        // thú + giỏ
        float petW = 210, py = DH - petW * 1.02f;
        float shake = frStun > 0 ? (float) Math.sin(now * 34) * 8 : 0;
        drawPet(c, species, stageOf(), 0, false, frX + shake, py + petW * 0.45f, petW);
        textC(c, "🧺", frX + shake, py + petW * 0.86f, 116, 0xFF000000);
        if (frStun > 0) textC(c, "💫", frX + shake, py - petW * 0.2f, 62, 0xFF000000);
        textC(c, "◀ ▶ Di chuyển  •  BACK Thoát", DW / 2, DH - 18, 24, 0xFF5A4520);
    }

    /* ---------- mini game: nhớ màu ---------- */
    private void drawMemory(Canvas c) {
        pf.setShader(new LinearGradient(0, 0, 0, DH, 0xFF6A5AE0, 0xFF8F7BFF, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, DW, DH, pf);
        pf.setShader(null);
        rr(c, 40, 30, 220, 60, 30, 0xE6FFFFFF);
        textC(c, "🪙 +" + memCoins, 150, 71, 30, 0xFF3A2E55);
        rr(c, DW / 2 - 240, 30, 480, 60, 30, 0xE6FFFFFF);
        textC(c, "🌈🧠 Bé Nhớ Giỏi", DW / 2, 71, 32, 0xFF3A2E55);
        rr(c, DW - 280, 30, 240, 60, 30, 0xE6FFFFFF);
        textC(c, "Vòng " + memRound + "/8", DW - 160, 71, 30, 0xFF3A2E55);

        float cx = DW / 2, cy = DH / 2 + 30, R = 250, pad = 210;
        int[] cols = {0xFFFFD34D, 0xFFFF6B81, 0xFF58D68D, 0xFF5DA9FF};
        String[] icons = {"☀️", "🍓", "🍀", "💧"};
        float[][] pos = {{cx, cy - R}, {cx + R, cy}, {cx, cy + R}, {cx - R, cy}};
        for (int i = 0; i < 4; i++) {
            boolean lit = litPad == i;
            float half = pad / 2f * (lit ? 1.1f : 1f);
            int col = cols[i];
            if (!lit) col = (col & 0x00FFFFFF) | 0xC8000000;
            if (lit) rr(c, pos[i][0] - half - 10, pos[i][1] - half - 10, half * 2 + 20, half * 2 + 20, 44, 0x80FFFFFF);
            rr(c, pos[i][0] - half, pos[i][1] - half + 6, half * 2, half * 2, 36, 0x40000000);
            rr(c, pos[i][0] - half, pos[i][1] - half, half * 2, half * 2, 36, col);
            textC(c, icons[i], pos[i][0], pos[i][1] + 26, 74, 0xFF000000);
        }
        pf.setColor(0xF7FFFFFF);
        c.drawCircle(cx, cy, 105, pf);
        textC(c, memMsg, cx, cy + 12, 32, 0xFF6A4FB6);
        textC(c, "Bấm ▲ ▶ ▼ ◀ theo đúng thứ tự nhấp nháy!  •  BACK Thoát", DW / 2, DH - 18, 24, 0xF0FFFFFF);
    }

    /* ---------- tiến hóa / nở trứng ---------- */
    private void drawEvo(Canvas c, String title, String name, int stage) {
        pf.setShader(new RadialGradient(DW / 2, DH * 0.45f, DW * 0.7f,
                new int[]{0xFFFFF8D6, 0xFFFFD86B, 0xFFB77BFF}, new float[]{0, 0.35f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, DW, DH, pf);
        pf.setShader(null);
        textC(c, title, DW / 2, 130, 64, 0xFFFFFFFF);
        drawPet(c, species, stage, 0, false, DW / 2, 440, 480);
        textC(c, name, DW / 2, 750, 48, 0xFF6A2FB6);
        textC(c, "Bấm OK để tiếp tục 🎉", DW / 2, DH - 30, 30, 0xFFFFFFFF);
        for (Conf cf : confs) {
            double t = now - cf.t0;
            if (t < 0 || t > 4.5) continue;
            float y = (float) (t * cf.vy) - 60;
            if (y > DH + 60) continue;
            c.save();
            c.rotate((float) (cf.rot + t * cf.vr), cf.x, y);
            textC(c, cf.e, cf.x, y, 44, 0xFF000000);
            c.restore();
        }
    }

    /* ---------- căn chỉnh ---------- */
    private void drawCalib(Canvas c) {
        pf.setColor(0xFF1C2A50);
        c.drawRect(0, 0, DW, DH, pf);
        pf.setColor(0xFF24335F);
        for (int i = -8; i < 24; i++) {
            path.reset();
            path.moveTo(i * 110, 0); path.lineTo(i * 110 + 55, 0);
            path.lineTo(i * 110 + 55 - DH, DH); path.lineTo(i * 110 - DH, DH);
            path.close();
            c.drawPath(path, pf);
        }
        ps.setColor(0xFFFFD34D); ps.setStrokeWidth(12);
        rf.set(8, 8, DW - 8, DH - 8);
        c.drawRoundRect(rf, 14, 14, ps);
        int corn = 0xFFFFD34D;
        textC(c, "◤", 56, 78, 56, corn); textC(c, "◥", DW - 56, 78, 56, corn);
        textC(c, "◣", 56, DH - 28, 56, corn); textC(c, "◢", DW - 56, DH - 28, 56, corn);

        float pw = 980, ph = 330, px = DW / 2 - pw / 2, py = DH / 2 - ph / 2;
        rr(c, px, py, pw, ph, 36, 0xF5FFFFFF);
        String[] modes = {"🔍 TO / NHỎ — bấm ◀ ▶", "↔️ DỊCH NGANG — bấm ◀ ▶",
                "↕️ DỊCH DỌC — bấm ▲ ▼", "🔄 ĐẶT LẠI — bấm ▶"};
        textC(c, modes[calMode], DW / 2, py + 84, 44, 0xFF6A4FB6);
        textC(c, "Cỡ " + Math.round(calS * 100) + "%  •  Ngang " + (int) calDx + "  •  Dọc " + (int) calDy,
                DW / 2, py + 150, 32, 0xFF3A2E55);
        textC(c, "Chỉnh đến khi thấy ĐỦ 4 GÓC VÀNG của khung trên tivi", DW / 2, py + 220, 26, 0xFF7B6CA8);
        textC(c, "OK: đổi chế độ  •  BACK: lưu & quay lại", DW / 2, py + 268, 26, 0xFF7B6CA8);
    }

    /* ---------- hiệu ứng nổi ---------- */
    private void drawFxs(Canvas c) {
        for (Fx f : fxs) {
            double t = now - f.t0;
            if (t < 0) continue;
            if (f.kind == 0) { // chữ bay lên
                float p = (float) (t / 1.5);
                int a = (int) (255 * (p < 0.2f ? p / 0.2f : 1 - (p - 0.2f) / 0.8f));
                if (a < 0) a = 0;
                textC(c, f.txt, f.x, f.y - p * 130, 40, Color.argb(a, 255, 255, 255));
            } else { // bong bóng
                float p = (float) (t / 2.4);
                int a = (int) (200 * (p < 0.25f ? p / 0.25f : 1 - (p - 0.25f) / 0.75f));
                if (a < 0) a = 0;
                pf.setColor(Color.argb(a, 200, 235, 255));
                c.drawCircle(f.x, f.y - p * 220, f.a * (0.5f + p), pf);
            }
        }
    }

    /* ================= VẼ THÚ & TRỨNG ================= */
    private void drawEgg(Canvas c, int sp, int crack, float cx, float cy, float size) {
        float k = size / 200f;
        c.save();
        c.translate(cx - 100 * k, cy - 100 * k);
        c.scale(k, k);
        int[] col = SP_COLORS[sp];
        pf.setColor(0xFFC89B57); c.drawOval(rrSet(38, 156, 124, 32), pf);
        pf.setColor(0xFFE8C173); c.drawOval(rrSet(48, 154, 104, 24), pf);
        pf.setShader(new RadialGradient(80, 60, 140, 0xFFFFFDF2, 0xFFF2E3C2, Shader.TileMode.CLAMP));
        path.reset();
        path.moveTo(100, 34);
        path.cubicTo(138, 34, 152, 86, 152, 118);
        path.cubicTo(152, 148, 130, 170, 100, 170);
        path.cubicTo(70, 170, 48, 148, 48, 118);
        path.cubicTo(48, 86, 62, 34, 100, 34);
        path.close();
        c.drawPath(path, pf);
        pf.setShader(null);
        pf.setColor(col[0]);
        c.drawCircle(82, 100, 9, pf); c.drawCircle(120, 82, 6, pf);
        c.drawCircle(112, 128, 11, pf); c.drawCircle(78, 140, 5, pf);
        if (crack >= 1) {
            ps.setColor(col[4]); ps.setStrokeWidth(3); ps.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(88, 52, 96, 62, ps); c.drawLine(96, 62, 89, 70, ps); c.drawLine(89, 70, 98, 79, ps);
        }
        if (crack >= 2) {
            c.drawLine(118, 60, 112, 71, ps); c.drawLine(112, 71, 121, 78, ps); c.drawLine(121, 78, 115, 88, ps);
            c.drawLine(70, 90, 80, 96, ps); c.drawLine(80, 96, 76, 106, ps);
        }
        pf.setColor(0x8CFFFFFF);
        c.drawOval(rrSet(70, 50, 24, 36), pf);
        c.restore();
    }

    /** Vẽ thú: hệ toạ độ gốc 200x200 (như SVG cũ), tâm (cx,cy), cao size. */
    private void drawPet(Canvas c, int sp, int stage, int moodV, boolean sleep, float cx, float cy, float size) {
        float k = size / 200f;
        c.save();
        c.translate(cx - 100 * k, cy - 100 * k);
        c.scale(k, k);
        int[] col = SP_COLORS[sp];
        int body1 = col[0], body2 = stage == 2 ? col[3] : col[1], belly = col[2], accent = col[3], dark = col[4];
        RadialGradient grad = new RadialGradient(76, 60, 160, body1, body2, Shader.TileMode.CLAMP);

        // cánh (cấp 2)
        if (stage >= 2) {
            pf.setShader(null); pf.setColor(body1);
            ps.setColor(dark); ps.setStrokeWidth(2);
            for (int side = 0; side < 2; side++) {
                c.save();
                if (side == 1) { c.scale(-1, 1, 100, 100); }
                path.reset();
                path.moveTo(34, 96);
                path.cubicTo(8, 78, 12, 60, 12, 52);
                path.lineTo(26, 66); path.lineTo(30, 50); path.lineTo(40, 62);
                path.close();
                c.drawPath(path, pf); c.drawPath(path, ps);
                c.restore();
            }
        }
        // đuôi (cấp >=1)
        if (stage >= 1) {
            pf.setShader(null);
            if (sp == 0) { // đuôi lửa
                pf.setColor(0xFFFF8A3D);
                path.reset();
                path.moveTo(158, 138); path.quadTo(184, 132, 182, 108);
                path.quadTo(178, 118, 168, 118); path.quadTo(176, 102, 164, 92);
                path.quadTo(166, 106, 154, 112); path.quadTo(148, 128, 158, 138);
                path.close();
                c.drawPath(path, pf);
            } else if (sp == 1) { // đuôi vây
                pf.setColor(accent);
                path.reset();
                path.moveTo(156, 142); path.quadTo(182, 144, 186, 124);
                path.quadTo(178, 130, 172, 128); path.quadTo(182, 118, 176, 106);
                path.quadTo(172, 118, 160, 118); path.quadTo(148, 128, 156, 142);
                path.close();
                c.drawPath(path, pf);
            } else { // đuôi lá
                pf.setColor(accent);
                c.save(); c.rotate(-30, 168, 130);
                c.drawOval(rrSet(152, 121, 32, 18), pf);
                c.restore();
            }
        }
        // chân
        pf.setShader(null); pf.setColor(body2);
        c.drawOval(rrSet(59, 167, 30, 18), pf);
        c.drawOval(rrSet(111, 167, 30, 18), pf);
        // tai
        pf.setShader(grad);
        if (stage == 0) {
            c.drawCircle(66, 62, 12, pf); c.drawCircle(134, 62, 12, pf);
        } else {
            for (int side = 0; side < 2; side++) {
                c.save();
                if (side == 1) c.scale(-1, 1, 100, 100);
                c.rotate(-18, 62, 56);
                c.drawOval(rrSet(50, 36, 24, 40), pf);
                pf.setShader(null); pf.setColor(belly);
                c.drawOval(rrSet(56, 47, 12, 22), pf);
                pf.setShader(grad);
                c.restore();
            }
        }
        // sừng (cấp 2)
        if (stage >= 2) {
            pf.setShader(null); pf.setColor(0xFFFFE59A);
            ps.setColor(dark); ps.setStrokeWidth(2);
            for (int side = 0; side < 2; side++) {
                c.save();
                if (side == 1) c.scale(-1, 1, 100, 100);
                path.reset();
                path.moveTo(76, 52); path.quadTo(70, 34, 80, 26);
                path.quadTo(86, 38, 86, 48); path.close();
                c.drawPath(path, pf); c.drawPath(path, ps);
                c.restore();
            }
        }
        // đặc điểm trên đầu (to dần theo cấp)
        float hs = stage == 0 ? 0.85f : stage == 1 ? 1.15f : 1.4f;
        c.save();
        c.scale(hs, hs, 100, 52);
        pf.setShader(null);
        if (sp == 0) { // ngọn lửa
            pf.setColor(0xFFFF8A3D);
            path.reset();
            path.moveTo(100, 40); path.quadTo(92, 54, 100, 62);
            path.quadTo(110, 56, 106, 44); path.quadTo(114, 52, 110, 64);
            path.quadTo(124, 54, 116, 36); path.quadTo(108, 24, 100, 40);
            path.close();
            c.drawPath(path, pf);
            pf.setColor(0xFFFFD34D);
            path.reset();
            path.moveTo(100, 48); path.quadTo(96, 56, 100, 61);
            path.quadTo(106, 57, 104, 49); path.close();
            c.drawPath(path, pf);
        } else if (sp == 1) { // giọt nước
            pf.setColor(accent);
            path.reset();
            path.moveTo(100, 38); path.quadTo(86, 54, 100, 64);
            path.quadTo(114, 54, 100, 38); path.close();
            c.drawPath(path, pf);
            pf.setColor(0xFFBFE9FF);
            c.drawCircle(100, 55, 4, pf);
        } else { // mầm lá
            ps.setColor(accent); ps.setStrokeWidth(6); ps.setStrokeCap(Paint.Cap.ROUND);
            path.reset(); path.moveTo(100, 62); path.quadTo(98, 40, 78, 36); c.drawPath(path, ps);
            path.reset(); path.moveTo(100, 62); path.quadTo(102, 40, 122, 36); c.drawPath(path, ps);
            pf.setColor(accent);
            c.save(); c.rotate(-35, 78, 38); c.drawOval(rrSet(68, 32, 20, 12), pf); c.restore();
            c.save(); c.rotate(35, 122, 38); c.drawOval(rrSet(112, 32, 20, 12), pf); c.restore();
        }
        c.restore();
        // thân
        float bodyR = stage == 0 ? 56 : stage == 1 ? 62 : 66;
        pf.setShader(grad);
        c.drawCircle(100, 118, bodyR, pf);
        pf.setShader(null); pf.setColor(belly);
        c.drawOval(rrSet(100 - bodyR * 0.62f, 140 - bodyR * 0.5f, bodyR * 1.24f, bodyR), pf);
        // tay (cấp >=1)
        if (stage >= 1) {
            pf.setShader(grad);
            for (int side = 0; side < 2; side++) {
                c.save();
                if (side == 1) c.scale(-1, 1, 100, 100);
                c.rotate(20, 42, 132);
                c.drawOval(rrSet(32, 116, 20, 32), pf);
                c.restore();
            }
            pf.setShader(null);
        }
        // mặt
        ps.setStrokeCap(Paint.Cap.ROUND);
        if (sleep) {
            ps.setColor(dark); ps.setStrokeWidth(4);
            path.reset(); path.moveTo(72, 106); path.quadTo(82, 114, 92, 106); c.drawPath(path, ps);
            path.reset(); path.moveTo(108, 106); path.quadTo(118, 114, 128, 106); c.drawPath(path, ps);
            pf.setColor(dark);
            c.drawOval(rrSet(94, 121, 12, 9), pf);
        } else {
            if (moodV == 2) { // buồn
                ps.setColor(dark); ps.setStrokeWidth(4);
                c.drawLine(72, 96, 90, 91, ps);
                c.drawLine(128, 96, 110, 91, ps);
            }
            pf.setColor(0xFFFFFFFF);
            c.drawCircle(82, 102, 10.5f, pf); c.drawCircle(118, 102, 10.5f, pf);
            pf.setColor(0xFF33224E);
            c.drawCircle(83, 103, 5.7f, pf); c.drawCircle(119, 103, 5.7f, pf);
            pf.setColor(0xFFFFFFFF);
            c.drawCircle(85.5f, 100, 2.6f, pf); c.drawCircle(121.5f, 100, 2.6f, pf);
            ps.setColor(dark); ps.setStrokeWidth(4.5f);
            path.reset();
            if (moodV == 0) { path.moveTo(88, 124); path.quadTo(100, 136, 112, 124); }
            else if (moodV == 1) { path.moveTo(92, 128); path.quadTo(100, 132, 108, 128); }
            else { path.moveTo(90, 132); path.quadTo(100, 123, 110, 132); }
            c.drawPath(path, ps);
        }
        pf.setColor(0xB3FF9DB0);
        c.drawOval(rrSet(61, 113, 14, 10), pf);
        c.drawOval(rrSet(125, 113, 14, 10), pf);
        c.restore();
    }
}
