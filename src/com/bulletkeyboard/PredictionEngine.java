package com.bulletkeyboard;

import java.util.ArrayList;

public class PredictionEngine {


    private StringBuilder currentWord = new StringBuilder();

    public void addChar(char c) {
        currentWord.append(Character.toLowerCase(c));
    }

    public void deleteChar() {
        if (currentWord.length() > 0) {
            currentWord.deleteCharAt(currentWord.length() - 1);
        }
    }

    public void reset() {
        currentWord = new StringBuilder();
    }

    public String[] getSuggestions() {
        String prefix = currentWord.toString().toLowerCase();
        if (prefix.length() == 0) {
            return new String[]{"", "", ""};
        }
        ArrayList<String> matches = new ArrayList<String>();
        for (int i = 0; i < WordList.WORDS.length; i++) {
            String word = WordList.WORDS[i];
            if (word.startsWith(prefix) && !word.equals(prefix)) {
                matches.add(word);
                if (matches.size() >= 3) break;
            }
        }
        String[] result = new String[]{"", "", ""};
        for (int i = 0; i < matches.size() && i < 3; i++) {
            result[i] = (String) matches.get(i);
        }
        return result;
    }

    public String getCurrentWord() {
        return currentWord.toString();
    }

    public boolean hasInput() {
        return currentWord.length() > 0;
    }
}
