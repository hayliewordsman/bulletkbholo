package com.bulletkeyboard;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PredictionEngine {

    private String[]      words;
    private StringBuilder currentWord = new StringBuilder();

    public PredictionEngine(Context context) {
        words = loadWords(context);
    }

    private String[] loadWords(Context context) {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.words);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            ArrayList<String> list = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) list.add(line);
            }
            br.close();
            return list.toArray(new String[list.size()]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    public void addChar(char c) {
        currentWord.append(Character.toLowerCase(c));
    }

    public void deleteChar() {
        if (currentWord.length() > 0)
            currentWord.deleteCharAt(currentWord.length() - 1);
    }

    public void reset() {
        currentWord = new StringBuilder();
    }

    public String[] getSuggestions() {
        String prefix = currentWord.toString().toLowerCase();
        if (prefix.length() == 0) return new String[]{"", "", ""};
        ArrayList<String> matches = new ArrayList<String>();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.startsWith(prefix) && !w.equals(prefix)) {
                matches.add(w);
                if (matches.size() >= 3) break;
            }
        }
        String[] result = new String[]{"", "", ""};
        for (int i = 0; i < matches.size() && i < 3; i++)
            result[i] = matches.get(i);
        return result;
    }

    public String getCurrentWord() { return currentWord.toString(); }
    public boolean hasInput()      { return currentWord.length() > 0; }
}
