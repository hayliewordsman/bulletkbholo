package com.bulletkeyboard;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class BulletKeyboardService extends InputMethodService
        implements BulletKeyboardView.KeyboardActionListener {

    private BulletKeyboardView keyboardView;
    private PredictionEngine   predEngine;

    @Override
    public void onCreate() {
        super.onCreate();
        predEngine = new PredictionEngine();
    }

    @Override
    public View onCreateInputView() {
        keyboardView = new BulletKeyboardView(this);
        keyboardView.setKeyboardActionListener(this);
        return keyboardView;
    }

    @Override
    public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        predEngine.reset();
        if (keyboardView != null) {
            keyboardView.setShifted(false);
            keyboardView.setSymbolsMode(false);
            keyboardView.setPredictions(new String[]{"","",""});
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (keyboardView != null) keyboardView.requestLayout();
    }

    @Override
    public void onKeyPress(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (code) {

            case BulletKeyboardView.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                predEngine.deleteChar();
                postPreds();
                break;

            case BulletKeyboardView.KEYCODE_DONE:
                sendEnter(ic);
                predEngine.reset();
                postPreds();
                break;

            case BulletKeyboardView.KEYCODE_SHIFT:
                if (keyboardView != null) keyboardView.setShifted(!keyboardView.isShifted());
                break;

            case BulletKeyboardView.KEYCODE_SYMBOLS:
                if (keyboardView != null) {
                    keyboardView.setSymbolsMode(!keyboardView.isSymbolsMode());
                    predEngine.reset();
                    postPreds();
                }
                break;

            case BulletKeyboardView.KEYCODE_SPACE:
                String[] cur = predEngine.getSuggestions();
                if (cur[0] != null && cur[0].length() > 0) {
                    replaceWord(ic, cur[0]);
                    ic.commitText(" ", 1);
                } else {
                    ic.commitText(" ", 1);
                    predEngine.reset();
                    postPreds();
                }
                dropShift();
                break;

            default:
                char c = (char) code;
                if (keyboardView != null && keyboardView.isShifted()) {
                    c = Character.toUpperCase(c);
                    dropShift();
                }
                ic.commitText(String.valueOf(c), 1);
                if (Character.isLetter(c) && (keyboardView == null || !keyboardView.isSymbolsMode()))
                    predEngine.addChar(c);
                else
                    predEngine.reset();
                postPreds();
                break;
        }
    }

    @Override
    public void onPredictionSelected(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || word == null || word.length() == 0) return;
        replaceWord(ic, word);
        ic.commitText(" ", 1);
        dropShift();
    }

    private void replaceWord(InputConnection ic, String w) {
        String cur = predEngine.getCurrentWord();
        if (cur.length() > 0) ic.deleteSurroundingText(cur.length(), 0);
        ic.commitText(w, 1);
        predEngine.reset();
        postPreds();
    }

    private void postPreds() {
        if (keyboardView != null) keyboardView.setPredictions(predEngine.getSuggestions());
    }

    private void dropShift() {
        if (keyboardView != null && keyboardView.isShifted()) keyboardView.setShifted(false);
    }

    private void sendEnter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        int action = (ei != null)
                ? (ei.imeOptions & EditorInfo.IME_MASK_ACTION)
                : EditorInfo.IME_ACTION_NONE;
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED)
            ic.performEditorAction(action);
        else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_ENTER));
        }
    }
}
