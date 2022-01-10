package main.services;

import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import java.io.IOException;
import java.util.HashMap;



public class Lemmatizer {

    private static HashMap<String, Integer> fillHashMap(TokenStream stream) throws IOException {
        HashMap<String, Integer> lemmas = new HashMap<>();
        stream.reset();
        while (stream.incrementToken()) {
            String lemma = stream.getAttribute(CharTermAttribute.class).toString();
            if (lemma.length() <= 2)
                continue;
            if (lemmas.containsKey(lemma)) {
                int countLemms = lemmas.get(lemma);
                lemmas.replace(lemma, countLemms, ++countLemms);
            } else {
                lemmas.put(lemma, 1);
            }
        }
        stream.end();
        stream.close();
        return lemmas;
    }

    @SneakyThrows
    public static HashMap<String, Integer> getMapWithLemmasAndCountLemmasInText(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        String russianText = text.replaceAll("[^А-яЁё ]", " ").toLowerCase();
        Analyzer russianAnalyzer = new RussianAnalyzer(RussianAnalyzer.getDefaultStopSet());
        TokenStream russianStream = russianAnalyzer.tokenStream("field", russianText);
        lemmas.putAll(fillHashMap(russianStream));
        String englishText = text.replaceAll("[^a-zA-Z ]", " ");
        if (!(englishText.isEmpty())) {
            Analyzer englishAnalyzer = new EnglishAnalyzer(EnglishAnalyzer.getDefaultStopSet());
            TokenStream englishStream = englishAnalyzer.tokenStream("field", englishText);
            lemmas.putAll(fillHashMap(englishStream));
        }
        return lemmas;
    }
}

