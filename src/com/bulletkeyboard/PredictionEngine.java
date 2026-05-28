package com.bulletkeyboard;

import java.util.ArrayList;

public class PredictionEngine {

    private static final String[] DICTIONARY = {
        "a","able","about","above","across","act","action","actually","add","after",
        "again","age","ago","agree","air","all","allow","also","always","am",
        "among","an","and","another","answer","any","anyone","anything","appear","are",
        "area","around","as","ask","at","away","back","bad","based","be",
        "because","been","before","begin","being","believe","best","better","between","big",
        "both","bring","business","but","by","call","can","cannot","care","case",
        "certain","change","check","child","clear","close","come","coming","common","community",
        "complete","consider","continue","could","create","current","day","days","did","different",
        "do","does","doing","done","down","during","each","early","either","else",
        "end","enough","even","ever","every","everything","example","experience","face","fact",
        "family","feel","few","finally","find","first","follow","following","for","form",
        "forward","found","from","full","general","get","give","go","going","good",
        "got","great","group","had","hand","happen","has","have","he","help",
        "her","here","high","him","his","home","hope","how","however","i",
        "idea","if","important","in","include","including","increase","interest","international","into",
        "is","it","its","just","keep","kind","know","knowledge","language","large",
        "last","later","learning","left","let","life","like","likely","little","long",
        "look","looking","love","made","make","making","man","many","matter","may",
        "maybe","me","mean","message","might","moment","more","most","much","my",
        "name","national","need","never","new","next","no","not","nothing","now",
        "number","of","off","often","on","once","one","only","open","or",
        "order","other","others","our","out","outside","over","own","part","people",
        "personal","place","please","point","political","possible","problem","process","program","provide",
        "public","put","question","quite","rather","really","reason","remember","result","return",
        "right","said","same","say","second","see","seem","service","several","she",
        "should","show","since","small","so","social","some","something","sometimes","speak",
        "started","still","student","such","support","sure","system","take","taking","talking",
        "tell","than","thank","that","the","their","them","themselves","then","there",
        "these","they","thing","think","this","those","though","through","time","to",
        "today","together","too","true","try","turn","two","under","understand","until",
        "up","use","used","usually","various","very","version","want","was","way",
        "we","well","were","what","when","where","whether","which","while","who",
        "why","will","with","without","work","working","world","would","year","yes",
        "yet","you","your","yourself","according","already","although","available","become",
        "came","decided","entire","especially","given","government","including","instead","less",
        "state","took","upon","using","within","years",
        "zero","one","two","three","four","five","six","seven","eight","nine",
        "ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen",
        "twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety","hundred","thousand",
        "monday","tuesday","wednesday","thursday","friday","saturday","sunday",
        "january","february","march","april","may","june","july","august",
        "september","october","november","december",
        "hello","hey","hi","bye","goodbye","please","thank","thanks","sorry","okay","ok",
        "yes","no","maybe","sure","right","wrong","good","bad","great","awesome",
        "phone","email","message","text","call","app","android","keyboard","type","send",
        "receive","open","close","save","delete","edit","view","search","find","help"
    };

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
        for (int i = 0; i < DICTIONARY.length; i++) {
            String word = DICTIONARY[i];
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
