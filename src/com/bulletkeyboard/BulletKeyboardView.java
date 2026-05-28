package com.bulletkeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BulletKeyboardView extends View {

    // ── Listener ──────────────────────────────────────────────────────────────
    public interface KeyboardActionListener {
        void onKeyPress(int primaryCode);
        void onPredictionSelected(String word);
    }

    // ── Key codes ─────────────────────────────────────────────────────────────
    public static final int KEYCODE_SHIFT   = -1;
    public static final int KEYCODE_SYMBOLS = -2;
    public static final int KEYCODE_DONE    = -4;
    public static final int KEYCODE_DELETE  = -5;
    public static final int KEYCODE_SWITCH  = -6;
    public static final int KEYCODE_SPACE   = 32;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float CORNER_DP        = 5f;
    private static final float GAP_DP           = 3f;
    private static final float PRED_HEIGHT_DP   = 44f;
    private static final float SHADOW_DP        = 2f;

    // ── QWERTY data ───────────────────────────────────────────────────────────
    private static final char[] ROW1 = {'q','w','e','r','t','y','u','i','o','p'};
    private static final char[] ROW2 = {'a','s','d','f','g','h','j','k','l'};
    private static final char[] ROW3 = {'z','x','c','v','b','n','m'};

    // ── Symbols data ──────────────────────────────────────────────────────────
    private static final String[] SYM1_L = {"1","2","3","4","5","6","7","8","9","0"};
    private static final int[]    SYM1_C = {'1','2','3','4','5','6','7','8','9','0'};

    private static final String[] SYM2_L = {"@","#","$","%","^","&","*","(",")"};
    private static final int[]    SYM2_C = {'@','#','$','%','^','&','*','(',')'};

    // Row 3 in symbols: first key is wider (shift slot), last key fills period slot
    // 9 symbols: index 0 = wide "shift" slot, 1-7 = letter slots, 8 = "period" slot
    private static final String[] SYM3_L = {"!", "?", "'", "\"", "/", ";", ":", "\\", "."};
    private static final int[]    SYM3_C = {'!', '?', '\'', '"', '/', ';', ':', '\\', '.'};

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isShifted     = false;
    private boolean isSymbolsMode = false;
    private int     pressedIndex  = -1;

    private Key[] keys;
    private String[] predictions = {"","",""};

    private KeyboardActionListener listener;

    // ── Theme ─────────────────────────────────────────────────────────────────
    private int colKey, colKeyPressed, colSpecial, colKeyText;
    private int colPredBg, colKbBg;

    // ── Paints ────────────────────────────────────────────────────────────────
    private Paint pKey, pSpecial, pPressed, pKeyText, pSmallText;
    private Paint pKbBg, pPredBg, pPredText, pPredChip, pSeparator, pShadow;

    // ── Dimensions ────────────────────────────────────────────────────────────
    private float density;
    private float gap, corner, predH, shadowH;
    private int   keyHeight;   // pixels

    // ── Key data class ────────────────────────────────────────────────────────
    static class Key {
        float x, y, w, h;
        String label;
        int code;
        boolean isSpecial;
    }

    // ═════════════════════════════════════════════════════════════════════════
    public BulletKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public BulletKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        density  = context.getResources().getDisplayMetrics().density;
        gap      = GAP_DP    * density;
        corner   = CORNER_DP * density;
        predH    = PRED_HEIGHT_DP * density;
        shadowH  = SHADOW_DP * density;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int heightDp = prefs.getInt("key_height", 55);
        keyHeight = (int)(heightDp * density);
        loadTheme(prefs.getString("color_theme", "black"));

        buildPaints();
    }

    // ── Theme loading ─────────────────────────────────────────────────────────
    private void loadTheme(String theme) {
        if ("brown".equals(theme)) {
            colKey        = 0xFF3E2723;
            colKeyPressed = 0xFF5D4037;
            colSpecial    = 0xFF2A1A17;
            colKeyText    = 0xFFEFEBE9;
            colPredBg     = 0xFF4E342E;
            colKbBg       = 0xFF1C0F0C;
        } else if ("burgundy".equals(theme)) {
            colKey        = 0xFF4A0010;
            colKeyPressed = 0xFF7B0026;
            colSpecial    = 0xFF30000A;
            colKeyText    = 0xFFFCE4EC;
            colPredBg     = 0xFF5D0018;
            colKbBg       = 0xFF1E0008;
        } else if ("gray".equals(theme)) {
            colKey        = 0xFF424242;
            colKeyPressed = 0xFF616161;
            colSpecial    = 0xFF2C2C2C;
            colKeyText    = 0xFFF5F5F5;
            colPredBg     = 0xFF4A4A4A;
            colKbBg       = 0xFF1E1E1E;
        } else { // black (default)
            colKey        = 0xFF212121;
            colKeyPressed = 0xFF424242;
            colSpecial    = 0xFF171717;
            colKeyText    = 0xFFFFFFFF;
            colPredBg     = 0xFF2C2C2C;
            colKbBg       = 0xFF141414;
        }
    }

    private void buildPaints() {
        pKbBg = new Paint();
        pKbBg.setColor(colKbBg);

        pPredBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPredBg.setColor(colPredBg);

        pKey = makeFillPaint(colKey);
        pSpecial = makeFillPaint(colSpecial);
        pPressed = makeFillPaint(colKeyPressed);

        pShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        pShadow.setColor(0x44000000);
        pShadow.setStyle(Paint.Style.FILL);

        pKeyText = makeTextPaint(colKeyText, 0f, true);

        pSmallText = makeTextPaint(colKeyText, 0f, false);

        pPredText = makeTextPaint(colKeyText, 0f, false);

        pPredChip = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPredChip.setColor(0x33FFFFFF);
        pPredChip.setStyle(Paint.Style.FILL);

        pSeparator = new Paint();
        pSeparator.setColor(0x22FFFFFF);
        pSeparator.setStrokeWidth(1f);
    }

    private Paint makeFillPaint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    private Paint makeTextPaint(int color, float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        if (size > 0) p.setTextSize(size);
        return p;
    }

    // ── Measurement ───────────────────────────────────────────────────────────
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        if (w == 0) w = getResources().getDisplayMetrics().widthPixels;
        int h = totalHeight();
        setMeasuredDimension(resolveSize(w, widthSpec), h);
    }

    private int totalHeight() {
        return (int)(predH) + 4 * keyHeight + 5 * (int)gap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        buildKeys(w);
    }

    // ── Key layout ────────────────────────────────────────────────────────────
    private void buildKeys(int W) {
        java.util.ArrayList<Key> list = new java.util.ArrayList<Key>();
        if (isSymbolsMode) {
            buildSymbolsLayout(list, W);
        } else {
            buildQwertyLayout(list, W);
        }
        keys = (Key[]) list.toArray(new Key[list.size()]);

        // Scale text sizes based on actual key dimensions
        float ts = keyHeight * 0.38f;
        pKeyText.setTextSize(ts);
        pSmallText.setTextSize(ts * 0.75f);
        pPredText.setTextSize(predH * 0.38f);
    }

    private void buildQwertyLayout(java.util.ArrayList<Key> L, int W) {
        float y = predH;

        // ── Row 1: QWERTYUIOP (10 equal keys) ─────────────────────────────
        y += gap;
        float kw1 = (W - 11f * gap) / 10f;
        for (int i = 0; i < ROW1.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw1 + gap);
            k.y = y;
            k.w = kw1;
            k.h = keyHeight;
            k.label = String.valueOf(ROW1[i]).toUpperCase();
            k.code = ROW1[i];
            L.add(k);
        }

        // ── Row 2: ASDFGHJKL + backspace ──────────────────────────────────
        // 9 letter keys (kw2) + backspace (1.5*kw2), all from left gap to right gap:
        //   gap + 9*(kw2+gap) + bsW = W - gap  =>  10.5*kw2 = W - 11*gap
        y += keyHeight + gap;
        float kw2 = (W - 11f * gap) / 10.5f;
        float bsW = 1.5f * kw2;
        for (int i = 0; i < ROW2.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw2 + gap);
            k.y = y;
            k.w = kw2;
            k.h = keyHeight;
            k.label = String.valueOf(ROW2[i]).toUpperCase();
            k.code = ROW2[i];
            L.add(k);
        }
        // Backspace
        addSpecialKey(L, gap + ROW2.length * (kw2 + gap), y, bsW, keyHeight, "DEL", KEYCODE_DELETE);

        // ── Row 3: Shift + ZXCVBNM + period ───────────────────────────────
        y += keyHeight + gap;
        // shift=1.5u, 7 letters=7u, period=1u → 9.5u + 10 gaps = W
        float u3 = (W - 10f * gap) / 9.5f;
        float shiftW = 1.5f * u3;
        float lx3 = gap + shiftW + gap;

        addSpecialKey(L, gap, y, shiftW, keyHeight, "⇧", KEYCODE_SHIFT);
        for (int i = 0; i < ROW3.length; i++) {
            Key k = new Key();
            k.x = lx3 + i * (u3 + gap);
            k.y = y;
            k.w = u3;
            k.h = keyHeight;
            k.label = String.valueOf(ROW3[i]).toUpperCase();
            k.code = ROW3[i];
            L.add(k);
        }
        // Period key (normal 1x width)
        float dotX = lx3 + ROW3.length * (u3 + gap);
        Key dotKey = new Key();
        dotKey.x = dotX; dotKey.y = y; dotKey.w = u3; dotKey.h = keyHeight;
        dotKey.label = "."; dotKey.code = '.';
        L.add(dotKey);

        // ── Row 4: ?123  IME  SPACE  ENTER ────────────────────────────────
        y += keyHeight + gap;
        float toggleW = kw1 * 1.8f;
        float switchW = kw1 * 1.2f;
        float enterW  = kw1 * 1.8f;
        // gap + toggleW + gap + switchW + gap + spaceW + gap + enterW + gap = W
        float spaceW  = W - 5f * gap - toggleW - switchW - enterW;

        float x4 = gap;
        addSpecialKey(L, x4, y, toggleW, keyHeight, "?123", KEYCODE_SYMBOLS);
        x4 += toggleW + gap;
        addSpecialKey(L, x4, y, switchW, keyHeight, "IME", KEYCODE_SWITCH);
        x4 += switchW + gap;
        Key spaceKey = new Key();
        spaceKey.x = x4; spaceKey.y = y; spaceKey.w = spaceW; spaceKey.h = keyHeight;
        spaceKey.label = ""; spaceKey.code = KEYCODE_SPACE;
        L.add(spaceKey);
        x4 += spaceW + gap;
        addSpecialKey(L, x4, y, W - x4 - gap, keyHeight, "↵", KEYCODE_DONE);
    }

    private void buildSymbolsLayout(java.util.ArrayList<Key> L, int W) {
        float y = predH;

        // ── Row 1: 1 2 3 4 5 6 7 8 9 0 (10 equal keys) ────────────────────
        y += gap;
        float kw1 = (W - 11f * gap) / 10f;
        for (int i = 0; i < SYM1_L.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw1 + gap);
            k.y = y; k.w = kw1; k.h = keyHeight;
            k.label = SYM1_L[i]; k.code = SYM1_C[i];
            L.add(k);
        }

        // ── Row 2: @ # $ % ^ & * ( )  + backspace ─────────────────────────
        // Same math as QWERTY row 2: 9*(kw2s) + 1.5*(kw2s) = W - 11*gap
        y += keyHeight + gap;
        float kw2s = (W - 11f * gap) / 10.5f;
        float bsWs = 1.5f * kw2s;
        for (int i = 0; i < SYM2_L.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw2s + gap);
            k.y = y; k.w = kw2s; k.h = keyHeight;
            k.label = SYM2_L[i]; k.code = SYM2_C[i];
            L.add(k);
        }
        addSpecialKey(L, gap + SYM2_L.length * (kw2s + gap), y, bsWs, keyHeight, "DEL", KEYCODE_DELETE);

        // ── Row 3: ! ? ' " / ; : \ .  (same slots as QWERTY row 3) ────────
        y += keyHeight + gap;
        // shift slot=1.5u, 7 middle=7u, period slot=1u → 9.5u + 10 gaps = W
        float u3s    = (W - 10f * gap) / 9.5f;
        float shiftWs= 1.5f * u3s;
        float lx3s   = gap + shiftWs + gap;

        // First sym in shift slot (wider)
        Key f = new Key();
        f.x = gap; f.y = y; f.w = shiftWs; f.h = keyHeight;
        f.label = SYM3_L[0]; f.code = SYM3_C[0];
        L.add(f);

        for (int i = 1; i < SYM3_L.length - 1; i++) {
            Key k = new Key();
            k.x = lx3s + (i - 1) * (u3s + gap);
            k.y = y; k.w = u3s; k.h = keyHeight;
            k.label = SYM3_L[i]; k.code = SYM3_C[i];
            L.add(k);
        }
        // Last sym in period slot (normal 1x width)
        float lastX = lx3s + (SYM3_L.length - 2) * (u3s + gap);
        Key last = new Key();
        last.x = lastX; last.y = y; last.w = u3s; last.h = keyHeight;
        last.label = SYM3_L[SYM3_L.length - 1]; last.code = SYM3_C[SYM3_C.length - 1];
        L.add(last);

        // ── Row 4: ABC  IME  SPACE  ENTER ─────────────────────────────────
        y += keyHeight + gap;
        float kw4     = (W - 11f * gap) / 10f;
        float toggleW = kw4 * 1.8f;
        float switchW = kw4 * 1.2f;
        float enterW  = kw4 * 1.8f;
        float spaceW  = W - 5f * gap - toggleW - switchW - enterW;

        float x4 = gap;
        addSpecialKey(L, x4, y, toggleW, keyHeight, "ABC", KEYCODE_SYMBOLS);
        x4 += toggleW + gap;
        addSpecialKey(L, x4, y, switchW, keyHeight, "IME", KEYCODE_SWITCH);
        x4 += switchW + gap;
        Key sp = new Key();
        sp.x = x4; sp.y = y; sp.w = spaceW; sp.h = keyHeight;
        sp.label = ""; sp.code = KEYCODE_SPACE;
        L.add(sp);
        x4 += spaceW + gap;
        addSpecialKey(L, x4, y, W - x4 - gap, keyHeight, "↵", KEYCODE_DONE);
    }

    private void addSpecialKey(java.util.ArrayList<Key> L, float x, float y, float w, float h, String label, int code) {
        Key k = new Key();
        k.x = x; k.y = y; k.w = w; k.h = h;
        k.label = label; k.code = code; k.isSpecial = true;
        L.add(k);
    }

    // ── Drawing ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        if (keys == null) return;

        int W = getWidth();

        // Keyboard background
        canvas.drawRect(0, 0, W, getHeight(), pKbBg);

        // Prediction area background
        canvas.drawRect(0, 0, W, predH, pPredBg);

        // Draw prediction chips aligned over Q(0), W(1), E(2) key positions
        drawPredictions(canvas);

        // Separator line
        canvas.drawLine(0, predH, W, predH, pSeparator);

        // Keys
        RectF r = new RectF();
        for (int i = 0; i < keys.length; i++) {
            Key k = keys[i];
            r.set(k.x, k.y, k.x + k.w, k.y + k.h);

            // Key shadow
            RectF shadow = new RectF(r.left + shadowH, r.top + shadowH, r.right + shadowH, r.bottom + shadowH);
            canvas.drawRoundRect(shadow, corner, corner, pShadow);

            // Key face
            Paint face;
            if (i == pressedIndex) {
                face = pPressed;
            } else if (k.isSpecial) {
                // Shift key glows when active
                if (k.code == KEYCODE_SHIFT && isShifted) {
                    Paint sp = new Paint(pPressed);
                    sp.setColor(blendColor(colSpecial, 0xFF6699FF, 0.55f));
                    face = sp;
                } else {
                    face = pSpecial;
                }
            } else {
                face = pKey;
            }
            canvas.drawRoundRect(r, corner, corner, face);

            // Key label
            if (k.label.length() > 0) {
                String lbl = k.label;
                if (!k.isSpecial && isShifted && !isSymbolsMode) {
                    lbl = lbl.toUpperCase();
                }
                Paint tp = k.isSpecial ? pSmallText : pKeyText;
                float tx = k.x + k.w / 2f;
                float ty = k.y + k.h / 2f + tp.getTextSize() * 0.36f;
                canvas.drawText(lbl, tx, ty, tp);
            }
        }
    }

    private void drawPredictions(Canvas canvas) {
        if (keys == null || keys.length < 3) return;

        float chipMargin = 4 * density;
        float chipTop    = chipMargin;
        float chipBottom = predH - chipMargin;

        for (int i = 0; i < 3; i++) {
            String pred = (i < predictions.length) ? predictions[i] : "";
            if (pred == null || pred.length() == 0) continue;

            // Align chip over key index i (Q=0, W=1, E=2)
            Key k = keys[i];
            float chipLeft  = k.x + 1 * density;
            float chipRight = k.x + k.w - 1 * density;

            RectF chipR = new RectF(chipLeft, chipTop, chipRight, chipBottom);
            canvas.drawRoundRect(chipR, 5 * density, 5 * density, pPredChip);

            float tx = chipLeft + (chipRight - chipLeft) / 2f;
            float ty = chipTop  + (chipBottom - chipTop) / 2f + pPredText.getTextSize() * 0.36f;

            // Clip text to chip width
            String display = clipText(pred, (int)(chipRight - chipLeft - 4 * density), pPredText);
            canvas.drawText(display, tx, ty, pPredText);
        }
    }

    private String clipText(String text, int maxWidth, Paint p) {
        if (p.measureText(text) <= maxWidth) return text;
        while (text.length() > 1 && p.measureText(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }

    // ── Touch ─────────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Check prediction area tap
                if (y < predH) {
                    int pi = predictionIndexAt(x);
                    if (pi >= 0 && pi < predictions.length) {
                        String pred = predictions[pi];
                        if (pred != null && pred.length() > 0 && listener != null) {
                            listener.onPredictionSelected(pred);
                        }
                    }
                    return true;
                }
                pressedIndex = keyAt(x, y);
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                pressedIndex = keyAt(x, y);
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int idx = keyAt(x, y);
                pressedIndex = -1;
                invalidate();
                if (idx >= 0 && listener != null) {
                    listener.onKeyPress(keys[idx].code);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                pressedIndex = -1;
                invalidate();
                break;
        }
        return true;
    }

    private int keyAt(float x, float y) {
        if (keys == null) return -1;
        for (int i = 0; i < keys.length; i++) {
            Key k = keys[i];
            if (x >= k.x && x <= k.x + k.w && y >= k.y && y <= k.y + k.h) return i;
        }
        return -1;
    }

    private int predictionIndexAt(float x) {
        if (keys == null || keys.length < 3) return -1;
        for (int i = 0; i < 3; i++) {
            Key k = keys[i];
            if (x >= k.x && x <= k.x + k.w) return i;
        }
        return -1;
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void setPredictions(String[] preds) {
        this.predictions = preds;
        invalidate();
    }

    public void setShifted(boolean shifted) {
        this.isShifted = shifted;
        invalidate();
    }

    public boolean isShifted() {
        return isShifted;
    }

    public void setSymbolsMode(boolean sym) {
        if (isSymbolsMode != sym) {
            isSymbolsMode = sym;
            if (getWidth() > 0) buildKeys(getWidth());
            invalidate();
        }
    }

    public boolean isSymbolsMode() {
        return isSymbolsMode;
    }

    public void setKeyboardActionListener(KeyboardActionListener l) {
        this.listener = l;
    }

    public void reloadPreferences(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int heightDp = prefs.getInt("key_height", 55);
        keyHeight = (int)(heightDp * density);
        loadTheme(prefs.getString("color_theme", "black"));
        buildPaints();
        if (getWidth() > 0) buildKeys(getWidth());
        requestLayout();
        invalidate();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int blendColor(int c1, int c2, float ratio) {
        int r = (int)(Color.red(c1)   * (1 - ratio) + Color.red(c2)   * ratio);
        int g = (int)(Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio);
        int b = (int)(Color.blue(c1)  * (1 - ratio) + Color.blue(c2)  * ratio);
        return Color.rgb(r, g, b);
    }
}
