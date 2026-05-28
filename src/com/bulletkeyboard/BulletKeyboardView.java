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

    public interface KeyboardActionListener {
        void onKeyPress(int primaryCode);
        void onPredictionSelected(String word);
    }

    public static final int KEYCODE_SHIFT   = -1;
    public static final int KEYCODE_SYMBOLS = -2;
    public static final int KEYCODE_DONE    = -4;
    public static final int KEYCODE_DELETE  = -5;
    public static final int KEYCODE_SWITCH  = -6;
    public static final int KEYCODE_SPACE   = 32;

    private static final float CORNER_DP      = 5f;
    private static final float GAP_DP         = 3f;
    private static final float PRED_HEIGHT_DP = 44f;
    private static final float SHADOW_DP      = 2f;

    private static final char[] ROW1 = {'q','w','e','r','t','y','u','i','o','p'};
    private static final char[] ROW2 = {'a','s','d','f','g','h','j','k','l'};
    private static final char[] ROW3 = {'z','x','c','v','b','n','m'};

    private static final String[] SYM1_L = {"1","2","3","4","5","6","7","8","9","0"};
    private static final int[]    SYM1_C = {'1','2','3','4','5','6','7','8','9','0'};
    private static final String[] SYM2_L = {"@","#","$","%","^","&","*","(",")"};
    private static final int[]    SYM2_C = {'@','#','$','%','^','&','*','(',')'};
    // 9 symbols: index 0 = wide shift slot, 1-7 = letter slots, 8 = period slot
    private static final String[] SYM3_L = {"!", "?", "'", "\"", "/", ";", ":", "\\", "."};
    private static final int[]    SYM3_C = {'!', '?', '\'', '"', '/', ';', ':', '\\', '.'};

    private boolean isShifted     = false;
    private boolean isSymbolsMode = false;
    private int     pressedIndex  = -1;

    private Key[]    keys;
    private String[] predictions = {"","",""};
    private KeyboardActionListener listener;

    private int colKey, colKeyPressed, colSpecial, colKeyText, colPredBg, colKbBg;
    private Paint pKey, pSpecial, pPressed, pKeyText, pSmallText;
    private Paint pKbBg, pPredBg, pPredText, pPredChip, pSeparator, pShadow;

    private float density, gap, corner, predH, shadowH;
    private int   keyHeight;

    static class Key {
        float x, y, w, h;
        String label;
        int code;
        boolean isSpecial;
    }

    public BulletKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public BulletKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        gap     = GAP_DP    * density;
        corner  = CORNER_DP * density;
        predH   = PRED_HEIGHT_DP * density;
        shadowH = SHADOW_DP * density;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        keyHeight = (int)(prefs.getInt("key_height", 55) * density);
        loadTheme(prefs.getString("color_theme", "black"));
        buildPaints();
    }

    private void loadTheme(String theme) {
        if ("brown".equals(theme)) {
            colKey = 0xFF3E2723; colKeyPressed = 0xFF5D4037; colSpecial = 0xFF2A1A17;
            colKeyText = 0xFFEFEBE9; colPredBg = 0xFF4E342E; colKbBg = 0xFF1C0F0C;
        } else if ("burgundy".equals(theme)) {
            colKey = 0xFF4A0010; colKeyPressed = 0xFF7B0026; colSpecial = 0xFF30000A;
            colKeyText = 0xFFFCE4EC; colPredBg = 0xFF5D0018; colKbBg = 0xFF1E0008;
        } else if ("gray".equals(theme)) {
            colKey = 0xFF424242; colKeyPressed = 0xFF616161; colSpecial = 0xFF2C2C2C;
            colKeyText = 0xFFF5F5F5; colPredBg = 0xFF4A4A4A; colKbBg = 0xFF1E1E1E;
        } else {
            colKey = 0xFF212121; colKeyPressed = 0xFF424242; colSpecial = 0xFF171717;
            colKeyText = 0xFFFFFFFF; colPredBg = 0xFF2C2C2C; colKbBg = 0xFF141414;
        }
    }

    private void buildPaints() {
        pKbBg    = new Paint(); pKbBg.setColor(colKbBg);
        pPredBg  = new Paint(Paint.ANTI_ALIAS_FLAG); pPredBg.setColor(colPredBg);
        pKey     = makeFill(colKey);
        pSpecial = makeFill(colSpecial);
        pPressed = makeFill(colKeyPressed);
        pShadow  = new Paint(Paint.ANTI_ALIAS_FLAG);
        pShadow.setColor(0x44000000); pShadow.setStyle(Paint.Style.FILL);
        pKeyText   = makeText(colKeyText, true);
        pSmallText = makeText(colKeyText, false);
        pPredText  = makeText(colKeyText, false);
        pPredChip  = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPredChip.setColor(0x33FFFFFF); pPredChip.setStyle(Paint.Style.FILL);
        pSeparator = new Paint(); pSeparator.setColor(0x22FFFFFF); pSeparator.setStrokeWidth(1f);
    }

    private Paint makeFill(int c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(c); p.setStyle(Paint.Style.FILL); return p;
    }

    private Paint makeText(int c, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(c); p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        if (w == 0) w = getResources().getDisplayMetrics().widthPixels;
        setMeasuredDimension(resolveSize(w, ws), totalH());
    }

    private int totalH() { return (int)predH + 4 * keyHeight + 5 * (int)gap; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        buildKeys(w);
    }

    private void buildKeys(int W) {
        java.util.ArrayList<Key> list = new java.util.ArrayList<Key>();
        if (isSymbolsMode) buildSymbolsLayout(list, W);
        else               buildQwertyLayout(list, W);
        keys = (Key[]) list.toArray(new Key[list.size()]);
        float ts = keyHeight * 0.38f;
        pKeyText.setTextSize(ts);
        pSmallText.setTextSize(ts * 0.75f);
        pPredText.setTextSize(predH * 0.38f);
    }

    private void buildQwertyLayout(java.util.ArrayList<Key> L, int W) {
        float y = predH + gap;
        float kw1 = (W - 11f * gap) / 10f;

        // Row 1: QWERTYUIOP
        for (int i = 0; i < ROW1.length; i++)
            addKey(L, gap + i*(kw1+gap), y, kw1, keyHeight,
                   String.valueOf(ROW1[i]).toUpperCase(), ROW1[i], false);

        // Row 2: ASDFGHJKL + DEL
        // kw2 = (W-11*gap)/10.5  =>  9*kw2 + 1.5*kw2 + 10*gap = W
        y += keyHeight + gap;
        float kw2 = (W - 11f * gap) / 10.5f;
        float bsW = 1.5f * kw2;
        for (int i = 0; i < ROW2.length; i++)
            addKey(L, gap + i*(kw2+gap), y, kw2, keyHeight,
                   String.valueOf(ROW2[i]).toUpperCase(), ROW2[i], false);
        addKey(L, gap + ROW2.length*(kw2+gap), y, bsW, keyHeight, "DEL", KEYCODE_DELETE, true);

        // Row 3: Shift + ZXCVBNM + period
        y += keyHeight + gap;
        float u3 = (W - 12f * gap) / 10f;
        float shiftW = 1.5f * u3;
        float lx3 = gap + shiftW + gap;
        addKey(L, gap, y, shiftW, keyHeight, "⇧", KEYCODE_SHIFT, true);
        for (int i = 0; i < ROW3.length; i++)
            addKey(L, lx3 + i*(u3+gap), y, u3, keyHeight,
                   String.valueOf(ROW3[i]).toUpperCase(), ROW3[i], false);
        float dotX = lx3 + ROW3.length * (u3 + gap);
        addKey(L, dotX, y, W - dotX - gap, keyHeight, ".", '.', false);

        // Row 4: ?123  IME  SPACE  GO
        y += keyHeight + gap;
        float toggleW = kw1 * 1.8f;
        float switchW = kw1 * 1.2f;
        float enterW  = kw1 * 1.8f;
        float spaceW  = W - 5f*gap - toggleW - switchW - enterW;
        float x4 = gap;
        addKey(L, x4, y, toggleW, keyHeight, "?123", KEYCODE_SYMBOLS, true);
        x4 += toggleW + gap;
        addKey(L, x4, y, switchW, keyHeight, "IME", KEYCODE_SWITCH, true);
        x4 += switchW + gap;
        addKey(L, x4, y, spaceW, keyHeight, "", KEYCODE_SPACE, false);
        x4 += spaceW + gap;
        addKey(L, x4, y, W - x4 - gap, keyHeight, "GO", KEYCODE_DONE, true);
    }

    private void buildSymbolsLayout(java.util.ArrayList<Key> L, int W) {
        float y = predH + gap;
        float kw1 = (W - 11f * gap) / 10f;

        // Row 1: 1-0
        for (int i = 0; i < SYM1_L.length; i++)
            addKey(L, gap + i*(kw1+gap), y, kw1, keyHeight, SYM1_L[i], SYM1_C[i], false);

        // Row 2: @ # $ % ^ & * ( )  + DEL
        y += keyHeight + gap;
        float kw2s = (W - 11f * gap) / 10.5f;
        float bsWs = 1.5f * kw2s;
        for (int i = 0; i < SYM2_L.length; i++)
            addKey(L, gap + i*(kw2s+gap), y, kw2s, keyHeight, SYM2_L[i], SYM2_C[i], false);
        addKey(L, gap + SYM2_L.length*(kw2s+gap), y, bsWs, keyHeight, "DEL", KEYCODE_DELETE, true);

        // Row 3: ! ? ' " / ; : \ .
        y += keyHeight + gap;
        float u3 = (W - 12f * gap) / 10f;
        float shiftW = 1.5f * u3;
        float lx3 = gap + shiftW + gap;
        addKey(L, gap, y, shiftW, keyHeight, SYM3_L[0], SYM3_C[0], false);
        for (int i = 1; i < SYM3_L.length - 1; i++)
            addKey(L, lx3 + (i-1)*(u3+gap), y, u3, keyHeight, SYM3_L[i], SYM3_C[i], false);
        float lastX = lx3 + (SYM3_L.length - 2) * (u3 + gap);
        addKey(L, lastX, y, W - lastX - gap, keyHeight,
               SYM3_L[SYM3_L.length-1], SYM3_C[SYM3_C.length-1], false);

        // Row 4: ABC  IME  SPACE  GO
        y += keyHeight + gap;
        float toggleW = kw1 * 1.8f;
        float switchW = kw1 * 1.2f;
        float enterW  = kw1 * 1.8f;
        float spaceW  = W - 5f*gap - toggleW - switchW - enterW;
        float x4 = gap;
        addKey(L, x4, y, toggleW, keyHeight, "ABC", KEYCODE_SYMBOLS, true);
        x4 += toggleW + gap;
        addKey(L, x4, y, switchW, keyHeight, "IME", KEYCODE_SWITCH, true);
        x4 += switchW + gap;
        addKey(L, x4, y, spaceW, keyHeight, "", KEYCODE_SPACE, false);
        x4 += spaceW + gap;
        addKey(L, x4, y, W - x4 - gap, keyHeight, "GO", KEYCODE_DONE, true);
    }

    private void addKey(java.util.ArrayList<Key> L, float x, float y,
                        float w, float h, String lbl, int code, boolean special) {
        Key k = new Key();
        k.x = x; k.y = y; k.w = w; k.h = h;
        k.label = lbl; k.code = code; k.isSpecial = special;
        L.add(k);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (keys == null) return;
        int W = getWidth();
        canvas.drawRect(0, 0, W, getHeight(), pKbBg);
        canvas.drawRect(0, 0, W, predH, pPredBg);
        drawPredictions(canvas);
        canvas.drawLine(0, predH, W, predH, pSeparator);

        RectF r = new RectF(), sh = new RectF();
        for (int i = 0; i < keys.length; i++) {
            Key k = keys[i];
            r.set(k.x, k.y, k.x+k.w, k.y+k.h);
            sh.set(r.left+shadowH, r.top+shadowH, r.right+shadowH, r.bottom+shadowH);
            canvas.drawRoundRect(sh, corner, corner, pShadow);

            Paint face;
            if (i == pressedIndex) {
                face = pPressed;
            } else if (k.isSpecial && k.code == KEYCODE_SHIFT && isShifted) {
                Paint sp = new Paint(pPressed);
                sp.setColor(blendColor(colSpecial, 0xFF6699FF, 0.55f));
                face = sp;
            } else {
                face = k.isSpecial ? pSpecial : pKey;
            }
            canvas.drawRoundRect(r, corner, corner, face);

            if (k.label != null && k.label.length() > 0) {
                String lbl = k.label;
                if (!k.isSpecial && isShifted && !isSymbolsMode) lbl = lbl.toUpperCase();
                Paint tp = k.isSpecial ? pSmallText : pKeyText;
                canvas.drawText(lbl, k.x + k.w/2f, k.y + k.h/2f + tp.getTextSize()*0.36f, tp);
            }
        }
    }

    private void drawPredictions(Canvas canvas) {
        if (keys == null || keys.length < 3) return;
        float mt = 4 * density;
        float ct = mt, cb = predH - mt;
        for (int i = 0; i < 3; i++) {
            String pred = (i < predictions.length) ? predictions[i] : "";
            if (pred == null || pred.length() == 0) continue;
            Key k = keys[i];
            float cl = k.x + density, cr = k.x + k.w - density;
            RectF chipR = new RectF(cl, ct, cr, cb);
            canvas.drawRoundRect(chipR, 5*density, 5*density, pPredChip);
            float tx = cl + (cr - cl) / 2f;
            float ty = ct + (cb - ct) / 2f + pPredText.getTextSize() * 0.36f;
            canvas.drawText(clip(pred, (int)(cr-cl-4*density), pPredText), tx, ty, pPredText);
        }
    }

    private String clip(String t, int maxW, Paint p) {
        if (p.measureText(t) <= maxW) return t;
        while (t.length() > 1 && p.measureText(t + "…") > maxW)
            t = t.substring(0, t.length()-1);
        return t + "…";
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (y < predH) {
                    int pi = predAt(x);
                    if (pi >= 0 && pi < predictions.length
                            && predictions[pi] != null && predictions[pi].length() > 0
                            && listener != null)
                        listener.onPredictionSelected(predictions[pi]);
                    return true;
                }
                pressedIndex = keyAt(x, y); invalidate(); break;
            case MotionEvent.ACTION_MOVE:
                pressedIndex = keyAt(x, y); invalidate(); break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int idx = keyAt(x, y); pressedIndex = -1; invalidate();
                if (idx >= 0 && listener != null) listener.onKeyPress(keys[idx].code);
                break;
            case MotionEvent.ACTION_CANCEL:
                pressedIndex = -1; invalidate(); break;
        }
        return true;
    }

    private int keyAt(float x, float y) {
        if (keys == null) return -1;
        for (int i = 0; i < keys.length; i++) {
            Key k = keys[i];
            if (x >= k.x && x <= k.x+k.w && y >= k.y && y <= k.y+k.h) return i;
        }
        return -1;
    }

    private int predAt(float x) {
        if (keys == null || keys.length < 3) return -1;
        for (int i = 0; i < 3; i++) { Key k = keys[i]; if (x >= k.x && x <= k.x+k.w) return i; }
        return -1;
    }

    public void setPredictions(String[] p) { predictions = p; invalidate(); }
    public void setShifted(boolean s)       { isShifted = s; invalidate(); }
    public boolean isShifted()              { return isShifted; }
    public void setKeyboardActionListener(KeyboardActionListener l) { listener = l; }

    public void setSymbolsMode(boolean sym) {
        if (isSymbolsMode != sym) {
            isSymbolsMode = sym;
            if (getWidth() > 0) buildKeys(getWidth());
            invalidate();
        }
    }
    public boolean isSymbolsMode() { return isSymbolsMode; }

    public void reloadPreferences(Context ctx) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        keyHeight = (int)(p.getInt("key_height", 55) * density);
        loadTheme(p.getString("color_theme", "black"));
        buildPaints();
        if (getWidth() > 0) buildKeys(getWidth());
        requestLayout(); invalidate();
    }

    private int blendColor(int c1, int c2, float r) {
        return Color.rgb(
            (int)(Color.red(c1)*(1-r)   + Color.red(c2)*r),
            (int)(Color.green(c1)*(1-r) + Color.green(c2)*r),
            (int)(Color.blue(c1)*(1-r)  + Color.blue(c2)*r));
    }
}
