package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.ArticleDAO;
import oh.hakju.scrapper.dao.ParagraphDAO;
import oh.hakju.scrapper.entity.Article;
import oh.hakju.scrapper.entity.Paragraph;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleToParagraph implements Runnable, Closeable {

    private ArticleDAO articleDAO = new ArticleDAO();

    private ParagraphDAO paragraphDAO = new ParagraphDAO();

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    @Override
    public void run() {
        Long startArticleId = paragraphDAO.findMaxArticleId();
        Long endArticleId = articleDAO.findMaxArticleId();

        if (startArticleId == endArticleId) {
            return;
        }

        for (Long i = startArticleId; i < endArticleId; i++) {
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

                Paragraph entity = new Paragraph();
                entity.setArticleId(articleId);
                entity.setParagraphSeq(paragraphSeq++);
                entity.setText(paragraph);
                paragraphDAO.insert(entity);

                System.out.println("Inserted the paragraph: " + paragraph);
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
    }

    public static void main(String[] args) {
        try (ArticleToParagraph articleToParagraph = new ArticleToParagraph();) {
            articleToParagraph.run();
        } catch (IOException e) {
        }
    }
}
