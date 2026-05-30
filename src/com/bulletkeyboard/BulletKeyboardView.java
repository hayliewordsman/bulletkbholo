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
        void onTextInsert(String text);
    }

    // ── Key codes ─────────────────────────────────────────────────────────────
    public static final int KEYCODE_SHIFT      = -1;
    public static final int KEYCODE_SYMBOLS    = -2;
    public static final int KEYCODE_DONE       = -4;
    public static final int KEYCODE_DELETE     = -5;
    public static final int KEYCODE_SWITCH     = -6;
    public static final int KEYCODE_SPACE      = 32;
    public static final int KEYCODE_EMOJI      = -7;
    public static final int KEYCODE_EMOJI_PREV = -8;
    public static final int KEYCODE_EMOJI_NEXT = -9;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float CORNER_DP      = 5f;
    private static final float GAP_DP         = 3f;
    private static final float SHADOW_DP      = 2f;
    private static final float SWIPE_UP_DP    = 10f;
    private static final float SWIPE_HORIZ_DP = 40f;

    // ── QWERTY data ───────────────────────────────────────────────────────────
    private static final char[] ROW1 = {'q','w','e','r','t','y','u','i','o','p'};
    private static final char[] ROW2 = {'a','s','d','f','g','h','j','k','l'};
    private static final char[] ROW3 = {'z','x','c','v','b','n','m'};

    // ── Symbols data ──────────────────────────────────────────────────────────
    private static final String[] SYM1_L = {"1","2","3","4","5","6","7","8","9","0"};
    private static final int[]    SYM1_C = {'1','2','3','4','5','6','7','8','9','0'};

    private static final String[] SYM2_L = {"@","#","$","%","^","&","*","(",")"};
    private static final int[]    SYM2_C = {'@','#','$','%','^','&','*','(',')'};

    private static final String[] SYM3_L = {"!", "?", "'", "\"", "/", ";", ":", "\\", "."};
    private static final int[]    SYM3_C = {'!', '?', '\'', '"', '/', ';', ':', '\\', '.'};

    // ── Emoji data (11 pages × 30 emoji) ─────────────────────────────────────
    private static final String[][] EMOJI_PAGES = {
        // 0: Happy faces
        {"😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃",
         "😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😋",
         "😛","😜","🤪","😝","🤑","🤗","😎","🥸","🧐","😬"},
        // 1: Sad / other faces
        {"😐","😑","😶","😏","😒","😞","😔","😟","😕","🙁",
         "☹️","😣","😖","😫","😩","🥺","😢","😭","😤","😠",
         "😡","🤬","😈","👿","💀","☠️","💩","🤡","👻","👽"},
        // 2: Hands
        {"👋","🤚","🖐","✋","🖖","👌","🤌","🤏","✌️","🤞",
         "🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","🫵",
         "👍","👎","✊","👊","🤛","🤜","👏","🙌","🫶","🙏"},
        // 3: People & body
        {"💪","🦾","🦵","🦶","👂","🦻","👃","🫀","🫁","🧠",
         "🦴","🦷","👀","👁","💋","👅","👄","👶","🧒","👦",
         "👧","🧑","👨","👩","🧓","👴","👵","👼","🎅","🤶"},
        // 4: Animals 1 — mammals & birds
        {"🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯",
         "🦁","🐮","🐷","🐽","🐸","🐵","🙈","🙉","🙊","🐒",
         "🐔","🐧","🐦","🦆","🦅","🦉","🦇","🐺","🐗","🐴"},
        // 5: Animals 2 — bugs, sea & big cats
        {"🦄","🐝","🐛","🦋","🐌","🐞","🐜","🦟","🦗","🕷",
         "🦂","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠",
         "🐟","🐬","🐳","🐋","🦈","🐊","🐅","🐆","🦓","🦍"},
        // 6: Nature & weather
        {"🌸","🌺","🌻","🌹","🌷","🌼","💐","🍀","🌿","🌾",
         "🌵","🌴","🌳","🌲","🍁","🍂","🍃","🌱","🌏","🌍",
         "🌎","🌕","🌙","⭐","🌟","💫","⚡","🌈","❄️","🔥"},
        // 7: Fruit & vegetables
        {"🍎","🍊","🍋","🍇","🍓","🫐","🍉","🍑","🥭","🍍",
         "🥥","🥝","🍅","🫒","🥑","🥦","🧄","🧅","🥕","🌽",
         "🌶️","🥒","🥬","🫑","🥗","🍄","🌰","🍞","🥐","🫓"},
        // 8: Cooked food & sweets
        {"🍔","🍟","🍕","🌭","🥪","🥙","🌮","🌯","🍜","🍝",
         "🍛","🍣","🍱","🥟","🍤","🍙","🍚","🍢","🥮","🧁",
         "🍰","🎂","🍮","🍭","🍬","🍫","🍿","🍩","🍪","🍦"},
        // 9: Drinks & activities
        {"☕","🍵","🧃","🥤","🧋","🍺","🍻","🥂","🍷","🥃",
         "🎮","🎯","⚽","🏀","🏈","🎸","🎵","🎶","🎤","🎧",
         "🎨","📱","💻","📺","✈️","🚂","🚗","🚀","🎁","💌"},
        // 10: Hearts & symbols
        {"❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔",
         "💕","💞","💓","💗","💖","💘","💝","💟","❣️","✨",
         "🌀","♻️","✅","❌","⭕","🔴","🟡","🟢","🔵","🟣"}
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isShifted     = false;
    private boolean isSymbolsMode = false;
    private boolean isEmojiMode   = false;
    private int     emojiPage     = 0;
    private int     pressedIndex  = -1;
    private float   touchDownY    = 0f;
    private float   touchDownX    = 0f;
    private int     touchDownKey  = -1;

    private Key[]    keys;
    private String[] predictions      = {"","",""};
    private int      currentWordLength = 0;
    private int[]    predKeyIndices    = {-1, -1, -1};

    private KeyboardActionListener listener;

    // ── Theme ─────────────────────────────────────────────────────────────────
    private int colKey, colKeyPressed, colSpecial, colKeyText;
    private int colKbBg;

    // ── Paints ────────────────────────────────────────────────────────────────
    private Paint pKey, pSpecial, pPressed, pKeyText, pSmallText;
    private Paint pKbBg, pPredText, pPredChip, pShadow;

    // ── Dimensions ────────────────────────────────────────────────────────────
    private float density;
    private float gap, corner, shadowH;
    private int   keyHeight;

    // ── Key data class ────────────────────────────────────────────────────────
    static class Key {
        float x, y, w, h;
        String label;
        int code;
        boolean isSpecial;
        String insert;
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
        density = context.getResources().getDisplayMetrics().density;
        gap     = GAP_DP    * density;
        corner  = CORNER_DP * density;
        shadowH = SHADOW_DP * density;

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
            colKbBg       = 0xFF1C0F0C;
        } else if ("burgundy".equals(theme)) {
            colKey        = 0xFF4A0010;
            colKeyPressed = 0xFF7B0026;
            colSpecial    = 0xFF30000A;
            colKeyText    = 0xFFFCE4EC;
            colKbBg       = 0xFF1E0008;
        } else if ("gray".equals(theme)) {
            colKey        = 0xFF424242;
            colKeyPressed = 0xFF616161;
            colSpecial    = 0xFF2C2C2C;
            colKeyText    = 0xFFF5F5F5;
            colKbBg       = 0xFF1E1E1E;
        } else { // black (default)
            colKey        = 0xFF212121;
            colKeyPressed = 0xFF424242;
            colSpecial    = 0xFF171717;
            colKeyText    = 0xFFFFFFFF;
            colKbBg       = 0xFF141414;
        }
    }

    private void buildPaints() {
        pKbBg    = makeFillPaint(colKbBg);
        pKey     = makeFillPaint(colKey);
        pSpecial = makeFillPaint(colSpecial);
        pPressed = makeFillPaint(colKeyPressed);

        pShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        pShadow.setColor(0x44000000);
        pShadow.setStyle(Paint.Style.FILL);

        pKeyText   = makeTextPaint(colKeyText, 0f, true);
        pSmallText = makeTextPaint(colKeyText, 0f, false);
        pPredText  = makeTextPaint(colKeyText, 0f, false);

        pPredChip = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPredChip.setColor(0x55FFFFFF);
        pPredChip.setStyle(Paint.Style.FILL);
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
        setMeasuredDimension(resolveSize(w, widthSpec), totalHeight());
    }

    private int totalHeight() {
        return 4 * keyHeight + 5 * (int)gap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        buildKeys(w);
    }

    // ── Key layout ────────────────────────────────────────────────────────────
    private void buildKeys(int W) {
        java.util.ArrayList<Key> list = new java.util.ArrayList<Key>();
        if (isEmojiMode) {
            buildEmojiLayout(list, W);
        } else if (isSymbolsMode) {
            buildSymbolsLayout(list, W);
        } else {
            buildQwertyLayout(list, W);
        }
        keys = (Key[]) list.toArray(new Key[list.size()]);

        float ts = isEmojiMode ? keyHeight * 0.50f : keyHeight * 0.38f;
        pKeyText.setTextSize(ts);
        pSmallText.setTextSize(ts * 0.75f);
        pPredText.setTextSize(keyHeight * 0.25f);
    }

    private void buildQwertyLayout(java.util.ArrayList<Key> L, int W) {
        // Row 1: QWERTYUIOP
        float y   = gap;
        float kw1 = (W - 11f * gap) / 10f;
        for (int i = 0; i < ROW1.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw1 + gap);
            k.y = y; k.w = kw1; k.h = keyHeight;
            k.label = String.valueOf(ROW1[i]).toUpperCase();
            k.code  = ROW1[i];
            L.add(k);
        }

        // Row 2: ASDFGHJKL + ⌫
        y += keyHeight + gap;
        float kw2 = (W - 11f * gap) / 10.5f;
        float bsW = 1.5f * kw2;
        for (int i = 0; i < ROW2.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw2 + gap);
            k.y = y; k.w = kw2; k.h = keyHeight;
            k.label = String.valueOf(ROW2[i]).toUpperCase();
            k.code  = ROW2[i];
            L.add(k);
        }
        addSpecialKey(L, gap + ROW2.length * (kw2 + gap), y, bsW, keyHeight, "⌫", KEYCODE_DELETE);

        // Row 3: Shift(1.5x) + ZXCVBNM + comma + period
        y += keyHeight + gap;
        float u3     = (W - 11f * gap) / 10.5f;
        float shiftW = 1.5f * u3;
        float lx3    = gap + shiftW + gap;
        addSpecialKey(L, gap, y, shiftW, keyHeight, "⇧", KEYCODE_SHIFT);
        for (int i = 0; i < ROW3.length; i++) {
            Key k = new Key();
            k.x = lx3 + i * (u3 + gap);
            k.y = y; k.w = u3; k.h = keyHeight;
            k.label = String.valueOf(ROW3[i]).toUpperCase();
            k.code  = ROW3[i];
            L.add(k);
        }
        float commaX = lx3 + ROW3.length * (u3 + gap);
        Key commaKey = new Key();
        commaKey.x = commaX; commaKey.y = y; commaKey.w = u3; commaKey.h = keyHeight;
        commaKey.label = ","; commaKey.code = ',';
        L.add(commaKey);
        float dotX = commaX + u3 + gap;
        Key dot = new Key();
        dot.x = dotX; dot.y = y; dot.w = u3; dot.h = keyHeight;
        dot.label = "."; dot.code = '.';
        L.add(dot);

        // Row 4: ?123  😊  SPACE  enter
        y += keyHeight + gap;
        float toggleW = kw1 * 1.8f;
        float emojiW  = kw1 * 1.2f;
        float enterW  = kw1 * 1.8f;
        float spaceW  = W - 5f * gap - toggleW - emojiW - enterW;
        float x4 = gap;
        addSpecialKey(L, x4, y, toggleW, keyHeight, "?123", KEYCODE_SYMBOLS);
        x4 += toggleW + gap;
        addSpecialKey(L, x4, y, emojiW, keyHeight, "😊", KEYCODE_EMOJI);
        x4 += emojiW + gap;
        Key spKey = new Key();
        spKey.x = x4; spKey.y = y; spKey.w = spaceW; spKey.h = keyHeight;
        spKey.label = ""; spKey.code = KEYCODE_SPACE;
        L.add(spKey);
        x4 += spaceW + gap;
        addSpecialKey(L, x4, y, W - x4 - gap, keyHeight, "↵", KEYCODE_DONE);
    }

    private void buildSymbolsLayout(java.util.ArrayList<Key> L, int W) {
        // Row 1: 1234567890
        float y   = gap;
        float kw1 = (W - 11f * gap) / 10f;
        for (int i = 0; i < SYM1_L.length; i++) {
            Key k = new Key();
            k.x = gap + i * (kw1 + gap);
            k.y = y; k.w = kw1; k.h = keyHeight;
            k.label = SYM1_L[i]; k.code = SYM1_C[i];
            L.add(k);
        }

        // Row 2: @#$%^&*() + ⌫
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
        addSpecialKey(L, gap + SYM2_L.length * (kw2s + gap), y, bsWs, keyHeight, "⌫", KEYCODE_DELETE);

        // Row 3: same geometry as QWERTY row 3
        y += keyHeight + gap;
        float u3s     = (W - 10f * gap) / 9.5f;
        float shiftWs = 1.5f * u3s;
        float lx3s    = gap + shiftWs + gap;
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
        float lastX = lx3s + (SYM3_L.length - 2) * (u3s + gap);
        Key last = new Key();
        last.x = lastX; last.y = y; last.w = u3s; last.h = keyHeight;
        last.label = SYM3_L[SYM3_L.length - 1]; last.code = SYM3_C[SYM3_C.length - 1];
        L.add(last);

        // Row 4: ABC  IME  SPACE  enter
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
        Key spKey = new Key();
        spKey.x = x4; spKey.y = y; spKey.w = spaceW; spKey.h = keyHeight;
        spKey.label = ""; spKey.code = KEYCODE_SPACE;
        L.add(spKey);
        x4 += spaceW + gap;
        addSpecialKey(L, x4, y, W - x4 - gap, keyHeight, "↵", KEYCODE_DONE);
    }

    private void buildEmojiLayout(java.util.ArrayList<Key> L, int W) {
        int pageIdx = (emojiPage >= 0 && emojiPage < EMOJI_PAGES.length) ? emojiPage : 0;
        String[] page = EMOJI_PAGES[pageIdx];
        float kw = (W - 11f * gap) / 10f;
        float y  = gap;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 10; col++) {
                int idx = row * 10 + col;
                Key k = new Key();
                k.x = gap + col * (kw + gap);
                k.y = y; k.w = kw; k.h = keyHeight;
                k.label = page[idx]; k.code = 0; k.insert = page[idx];
                L.add(k);
            }
            y += keyHeight + gap;
        }
        // Bottom row: ABC  ◀  [N/M]  ▶  ↵
        float abcW   = kw * 1.5f;
        float navW   = kw * 0.9f;
        float enterW = kw * 1.5f;
        float spaceW = W - 6f * gap - abcW - navW - navW - enterW;
        float x4 = gap;
        addSpecialKey(L, x4, y, abcW, keyHeight, "ABC", KEYCODE_EMOJI);
        x4 += abcW + gap;
        addSpecialKey(L, x4, y, navW, keyHeight, "◀", KEYCODE_EMOJI_PREV);
        x4 += navW + gap;
        Key sp = new Key();
        sp.x = x4; sp.y = y; sp.w = spaceW; sp.h = keyHeight;
        sp.label = (pageIdx + 1) + "/" + EMOJI_PAGES.length;
        sp.code = KEYCODE_SPACE;
        L.add(sp);
        x4 += spaceW + gap;
        addSpecialKey(L, x4, y, navW, keyHeight, "▶", KEYCODE_EMOJI_NEXT);
        x4 += navW + gap;
        addSpecialKey(L, x4, y, W - x4 - gap, keyHeight, "↵", KEYCODE_DONE);
    }

    private void addSpecialKey(java.util.ArrayList<Key> L,
            float x, float y, float w, float h, String label, int code) {
        Key k = new Key();
        k.x = x; k.y = y; k.w = w; k.h = h;
        k.label = label; k.code = code; k.isSpecial = true;
        L.add(k);
    }

    // ── Prediction key mapping ─────────────────────────────────────────────────
    private void computePredKeyIndices() {
        predKeyIndices[0] = predKeyIndices[1] = predKeyIndices[2] = -1;
        if (isSymbolsMode || isEmojiMode || keys == null) return;
        for (int i = 0; i < 3; i++) {
            String pred = (i < predictions.length) ? predictions[i] : "";
            if (pred == null || pred.length() <= currentWordLength) continue;
            char next = Character.toLowerCase(pred.charAt(currentWordLength));
            int keyIdx = findLetterKey(next);
            if (keyIdx < 0) continue;
            boolean taken = false;
            for (int j = 0; j < i; j++) {
                if (predKeyIndices[j] == keyIdx) { taken = true; break; }
            }
            if (!taken) predKeyIndices[i] = keyIdx;
        }
    }

    private int findLetterKey(char c) {
        if (keys == null) return -1;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].code == c) return i;
        }
        return -1;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        if (keys == null) return;

        computePredKeyIndices();

        int W = getWidth();
        canvas.drawRect(0, 0, W, getHeight(), pKbBg);

        RectF r = new RectF();
        for (int i = 0; i < keys.length; i++) {
            Key k = keys[i];
            r.set(k.x, k.y, k.x + k.w, k.y + k.h);

            // Shadow
            canvas.drawRoundRect(
                new RectF(r.left + shadowH, r.top + shadowH,
                          r.right + shadowH, r.bottom + shadowH),
                corner, corner, pShadow);

            // Face
            Paint face;
            if (i == pressedIndex) {
                face = pPressed;
            } else if (k.isSpecial) {
                if (k.code == KEYCODE_SHIFT && isShifted) {
                    Paint glow = new Paint(pPressed);
                    glow.setColor(blendColor(colSpecial, 0xFF6699FF, 0.55f));
                    face = glow;
                } else {
                    face = pSpecial;
                }
            } else {
                face = pKey;
            }
            canvas.drawRoundRect(r, corner, corner, face);

            // Label — shift down when a prediction chip overlays this key
            if (k.label != null && k.label.length() > 0) {
                String lbl = k.label;
                if (!k.isSpecial && isShifted && !isSymbolsMode && !isEmojiMode) {
                    lbl = lbl.toUpperCase();
                }
                Paint tp = k.isSpecial ? pSmallText : pKeyText;
                float tx = k.x + k.w / 2f;
                boolean hasChip = false;
                for (int pi = 0; pi < 3; pi++) {
                    if (predKeyIndices[pi] == i) { hasChip = true; break; }
                }
                float ty = hasChip
                    ? k.y + k.h * 0.72f + tp.getTextSize() * 0.36f
                    : k.y + k.h / 2f   + tp.getTextSize() * 0.36f;
                canvas.drawText(lbl, tx, ty, tp);
            }
        }

        drawPredictions(canvas);
    }

    private void drawPredictions(Canvas canvas) {
        if (keys == null || isSymbolsMode || isEmojiMode) return;

        float m = 2f * density;

        for (int i = 0; i < 3; i++) {
            if (predKeyIndices[i] < 0) continue;
            String pred = (i < predictions.length) ? predictions[i] : "";
            if (pred == null || pred.length() == 0) continue;

            Key k = keys[predKeyIndices[i]];
            RectF chip = new RectF(
                k.x + m,
                k.y + m,
                k.x + k.w - m,
                k.y + k.h * 0.46f - m);

            canvas.drawRoundRect(chip, corner * 0.8f, corner * 0.8f, pPredChip);

            float tx = chip.left + chip.width()  / 2f;
            float ty = chip.top  + chip.height() / 2f + pPredText.getTextSize() * 0.36f;
            String display = clipText(pred, (int)(chip.width() - 4f * density), pPredText);
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
                touchDownY   = y;
                touchDownX   = x;
                touchDownKey = keyAt(x, y);
                pressedIndex = touchDownKey;
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                pressedIndex = touchDownKey;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                computePredKeyIndices();
                pressedIndex = -1;
                invalidate();
                if (listener != null) {
                    float swipeDy = touchDownY - y;
                    float swipeDx = x - touchDownX;
                    // Horizontal swipe in emoji mode navigates pages
                    if (isEmojiMode
                            && Math.abs(swipeDx) > SWIPE_HORIZ_DP * density
                            && Math.abs(swipeDx) > Math.abs(swipeDy)) {
                        if (swipeDx < 0) nextEmojiPage();
                        else             prevEmojiPage();
                    } else if (touchDownKey >= 0) {
                        int predIdx = -1;
                        for (int pi = 0; pi < 3; pi++) {
                            if (predKeyIndices[pi] == touchDownKey) { predIdx = pi; break; }
                        }
                        if (predIdx >= 0 && swipeDy > SWIPE_UP_DP * density) {
                            listener.onPredictionSelected(predictions[predIdx]);
                        } else if (keys[touchDownKey].insert != null) {
                            listener.onTextInsert(keys[touchDownKey].insert);
                        } else {
                            listener.onKeyPress(keys[touchDownKey].code);
                        }
                    }
                }
                touchDownKey = -1;
                break;

            case MotionEvent.ACTION_CANCEL:
                pressedIndex = -1;
                touchDownKey = -1;
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

    // ── Public API ────────────────────────────────────────────────────────────
    public void setPredictions(String[] preds) {
        this.predictions = preds;
        invalidate();
    }

    public void setCurrentWordLength(int len) {
        this.currentWordLength = len;
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
            if (sym) isEmojiMode = false;
            if (getWidth() > 0) buildKeys(getWidth());
            invalidate();
        }
    }

    public boolean isSymbolsMode() {
        return isSymbolsMode;
    }

    public void setEmojiMode(boolean emoji) {
        if (isEmojiMode != emoji) {
            isEmojiMode = emoji;
            if (!emoji) emojiPage = 0;
            if (emoji) isSymbolsMode = false;
            if (getWidth() > 0) buildKeys(getWidth());
            invalidate();
        }
    }

    public boolean isEmojiMode() {
        return isEmojiMode;
    }

    public void prevEmojiPage() {
        emojiPage = (emojiPage + EMOJI_PAGES.length - 1) % EMOJI_PAGES.length;
        if (getWidth() > 0) buildKeys(getWidth());
        invalidate();
    }

    public void nextEmojiPage() {
        emojiPage = (emojiPage + 1) % EMOJI_PAGES.length;
        if (getWidth() > 0) buildKeys(getWidth());
        invalidate();
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
