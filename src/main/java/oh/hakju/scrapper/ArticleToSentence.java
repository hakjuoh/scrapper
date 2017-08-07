package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.ArticleDAO;
import oh.hakju.scrapper.dao.SentenceDAO;
import oh.hakju.scrapper.entity.Article;
import oh.hakju.scrapper.entity.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleToSentence implements Runnable, Closeable {

    private ArticleDAO articleDAO = new ArticleDAO();

    private SentenceDAO sentenceDAO = new SentenceDAO();

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    @Override
    public void run() {
        Long minArticleId = sentenceDAO.findMinArticleId();
        Long maxArticleId = articleDAO.findMaxArticleId();

        if (minArticleId == maxArticleId) {
            return;
        }

        for (Long i = minArticleId; i < maxArticleId; i++) {
            execute(i);
        }
    }

    public void execute(Long articleId) {
        Worker worker = new Worker(articleId);
        executor.execute(worker);
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    private class Worker implements Runnable {
        private Long articleId;

        public Worker(Long articleId) {
            this.articleId = articleId;
        }

        @Override
        public void run() {
            Article article = articleDAO.findById(articleId);
            if (article == null) {
                return;
            }

            String text = article.getText();
            int paragraphSeq = 1;
            for (String paragraph : getParagraphs(text)) {
                int sentenceSeq = 1;
                for (String sentence : getSentences(paragraph)) {

                    Sentence entity = new Sentence();
                    entity.setArticleId(articleId);
                    entity.setParagraphSeq(paragraphSeq++);
                    entity.setSentenceSeq(sentenceSeq++);
                    entity.setText(sentence);

                    sentenceDAO.insert(entity);
                    System.out.println("Inserted the following sentence: " + sentence);
                }
            }
        }

        private List<String> getParagraphs(String text) {
            List<String> paragraphs = new ArrayList();
            StringTokenizer paragraphTokenizer = new StringTokenizer(text, "\n");
            while (paragraphTokenizer.hasMoreTokens()) {
                String paragraph = paragraphTokenizer.nextToken();
                paragraphs.add(paragraph);
            }
            return paragraphs;
        }

        private List<Character> END_CHAR = Arrays.asList('.', '!', '?');
        private List<String> PREFIXS_OF_PERSON_NAME = Arrays.asList("Mr", "Ms", "Mrs");

        private List<String> getSentences(String paragraph) {
            List<String> sentences = new ArrayList();

            paragraph = paragraph.replaceAll("[(]", "").replaceAll("[)]", "");

            StringTokenizer sentenceTokenizer = new StringTokenizer(paragraph, " ");
            StringBuilder sb = new StringBuilder();

            boolean isStartedWithDoubleQuote = false;
            while (sentenceTokenizer.hasMoreTokens()) {
                String token = sentenceTokenizer.nextToken();
                sb.append(token).append(" ");

                char firstChar = token.charAt(0);
                if (firstChar == '“') {
                    isStartedWithDoubleQuote = true;
                }

                char lastChar = token.charAt(token.length() - 1);
                if (lastChar == '”') {
                    if (!isStartedWithDoubleQuote) {
                        throw new IllegalStateException();
                    }

                    isStartedWithDoubleQuote = false;
                    lastChar = token.charAt(token.length() - 2);
                }

                if (END_CHAR.contains(lastChar)) {
                    if (isStartedWithDoubleQuote) {
                        continue;
                    }
                    if (PREFIXS_OF_PERSON_NAME.contains(token.substring(0, token.length() - 1))) {
                        continue;
                    }


                    String sentence = sb.toString();
                    sentence = sentence.trim();
                    sentences.add(sentence);

                    sb = new StringBuilder();
                }
            }

            return sentences;
        }

        private List<Character> STOP_CHAR = Arrays.asList('\"', '(', ')');

        private String normalize(String token) {
            StringBuilder sb = new StringBuilder();
            for (char ch : token.toCharArray()) {
                if (STOP_CHAR.contains(ch)) {
                    continue;
                }

                sb.append(ch);
            }

            return sb.toString();
        }
    }

    public static void main(String[] args) {
        try (ArticleToSentence articleToSentence = new ArticleToSentence();) {
            //articleToSentence.execute(2L);
            articleToSentence.run();
        } catch (IOException e) {
        }
    }
}
