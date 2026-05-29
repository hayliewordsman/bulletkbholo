package com.bulletkeyboard;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

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
            keyboardView.setEmojiMode(false);
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
                postPredictions();
                break;

            case BulletKeyboardView.KEYCODE_DONE:
                sendEnterOrAction(ic);
                predEngine.reset();
                postPredictions();
                break;

            case BulletKeyboardView.KEYCODE_SHIFT:
                if (keyboardView != null) keyboardView.setShifted(!keyboardView.isShifted());
                break;

            case BulletKeyboardView.KEYCODE_SYMBOLS:
                if (keyboardView != null) {
                    keyboardView.setSymbolsMode(!keyboardView.isSymbolsMode());
                    predEngine.reset();
                    postPredictions();
                }
                break;

            case BulletKeyboardView.KEYCODE_SWITCH:
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showInputMethodPicker();
                break;

            case BulletKeyboardView.KEYCODE_EMOJI:
                if (keyboardView != null) {
                    keyboardView.setEmojiMode(!keyboardView.isEmojiMode());
                    predEngine.reset();
                    postPredictions();
                }
                break;

            case BulletKeyboardView.KEYCODE_EMOJI_PREV:
                if (keyboardView != null) keyboardView.prevEmojiPage();
                break;

            case BulletKeyboardView.KEYCODE_EMOJI_NEXT:
                if (keyboardView != null) keyboardView.nextEmojiPage();
                break;

            case BulletKeyboardView.KEYCODE_SPACE:
                ic.commitText(" ", 1);
                predEngine.reset();
                postPredictions();
                dropShiftOnce();
                break;

            default:
                char c = (char) code;
                boolean shifted = keyboardView != null && keyboardView.isShifted();
                if (shifted) {
                    c = Character.toUpperCase(c);
                    dropShiftOnce();
                }
                ic.commitText(String.valueOf(c), 1);
                if (Character.isLetter(c) && (keyboardView == null || !keyboardView.isSymbolsMode()))
                    predEngine.addChar(c);
                else
                    predEngine.reset();
                postPredictions();
                break;
        }
    }

    @Override
    public void onTextInsert(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || text == null) return;
        ic.commitText(text, 1);
    }

    @Override
    public void onPredictionSelected(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || word == null || word.length() == 0) return;
        replaceCurrentWord(ic, word);
        ic.commitText(" ", 1);
        dropShiftOnce();
    }

    private void replaceCurrentWord(InputConnection ic, String replacement) {
        String current = predEngine.getCurrentWord();
        if (current.length() > 0) ic.deleteSurroundingText(current.length(), 0);
        ic.commitText(replacement, 1);
        predEngine.reset();
        postPredictions();
    }

    private void postPredictions() {
        if (keyboardView == null) return;
        keyboardView.setPredictions(predEngine.getSuggestions());
        keyboardView.setCurrentWordLength(predEngine.getCurrentWord().length());
    }

    private void dropShiftOnce() {
        if (keyboardView != null && keyboardView.isShifted()) keyboardView.setShifted(false);
    }

    private void sendEnterOrAction(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        boolean multiLine = ei != null &&
                (ei.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        int action = (ei != null)
                ? (ei.imeOptions & EditorInfo.IME_MASK_ACTION)
                : EditorInfo.IME_ACTION_NONE;
        if (!multiLine && action != EditorInfo.IME_ACTION_NONE
                && action != EditorInfo.IME_ACTION_UNSPECIFIED)
            ic.performEditorAction(action);
        else
            ic.commitText("\n", 1);
    }
}
