package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.ParagraphDAO;
import oh.hakju.scrapper.dao.VocabularyDAO;
import oh.hakju.scrapper.dao.VocabularyRelationDAO;
import oh.hakju.scrapper.entity.Paragraph;
import oh.hakju.scrapper.entity.Vocabulary;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MakeVocabularyThroughPronouns implements Runnable, Closeable {

    private static List<String> pronouns_for_possession = Arrays.asList(" my ", " his ", " her ", " their ", " our ");

    private ParagraphDAO paragraphDAO = new ParagraphDAO();

    private VocabularyDAO vocabularyDAO = new VocabularyDAO();
    private VocabularyRelationDAO vocabularyRelationDAO = new VocabularyRelationDAO();

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    @Override
    public void run() {
        for (Paragraph paragraph : paragraphDAO.findParagraph()) {
            Worker worker = new Worker(paragraph);
            executor.execute(worker);
        }
    }

    private class Worker implements Runnable {

        private Paragraph paragraph;

        public Worker(Paragraph paragraph) {
            this.paragraph = paragraph;
        }

        @Override
        public void run() {
            String text = paragraph.getText();

            for (String pronoun_for_possession : pronouns_for_possession) {
                int idx = text.indexOf(pronoun_for_possession);
                if (idx != -1) {
                    String word = findWordAfterIndex(text, idx + pronoun_for_possession.length());
                    if (word.length() > 2) {
                        Vocabulary vocabulary = new Vocabulary();
                        vocabulary.setWord(word);
                        vocabulary.setPos("Noun");
                        try {
                            vocabulary = vocabularyDAO.insert(vocabulary);
                        } catch (DAOException ignore) {
                            vocabulary = vocabularyDAO.findByWordAndPos(vocabulary.getWord(), vocabulary.getPos());
                        }

                        if (vocabulary != null) {
                            try {
                                vocabularyRelationDAO.insertVocabularyRelation(paragraph, vocabulary);
                            } catch (DAOException ignore) {}
                        }
                    }
                }
            }

        }
    }

    private String findWordAfterIndex(String text, int idx) {
        int endIdx = text.indexOf(' ', idx);
        String word = text.substring(idx, (endIdx != -1) ? endIdx : text.length());
        StringBuilder sb = new StringBuilder();
        for (char ch : word.toCharArray()) {
            if (Character.isLetter(ch)) {
                sb.append(ch);
            } else {
                return "";
            }
        }
        return word;
    }

    public static void main(String[] args) throws IOException {

        try (MakeVocabularyThroughPronouns task = new MakeVocabularyThroughPronouns();) {
            task.run();
        }
    }
}
