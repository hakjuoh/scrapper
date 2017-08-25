package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.ArticleDAO;
import oh.hakju.scrapper.dao.ContentCategoryDAO;
import oh.hakju.scrapper.dao.ContentDAO;
import oh.hakju.scrapper.dao.ContentRelationDAO;
import oh.hakju.scrapper.entity.Article;
import oh.hakju.scrapper.entity.Content;
import oh.hakju.scrapper.entity.ContentCategory;
import oh.hakju.scrapper.entity.ContentRelation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import static oh.hakju.scrapper.Helper.getContentFrom;

public class Bootstrapper {

    private ContentDAO contentDao = new ContentDAO();
    private ContentRelationDAO contentRelationDAO = new ContentRelationDAO();
    private ContentCategoryDAO contentCategoryDAO = new ContentCategoryDAO();
    private ArticleDAO articleDAO = new ArticleDAO();

    private BlockingQueue<Worker> queue = new LinkedBlockingQueue();
    private ExecutorService queueTaker = Executors.newSingleThreadExecutor();
    private ExecutorService scrapperPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

    public Bootstrapper() {
        queueTaker.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Worker worker = null;
                try {
                    worker = queue.take();
                } catch (InterruptedException e) {
                    break;
                }

                if (worker != null) {
                    scrapperPool.execute(worker);
                }
            }
        });
    }

    private boolean isAvailable(URL url, String href) {
        String host = url.getHost();
        if (!href.startsWith("http://" + host) && !href.startsWith("https://" + host)) {
            return false;
        }

        URL contentURL;
        try {
            contentURL = new URL(href);
        } catch (MalformedURLException e) {
            return false;
        }

        String path = contentURL.getPath();
        if (!path.endsWith("html")) {
            return false;
        }

        return true;
    }

    public Content scrap(URL url) throws IOException {
        String contentString = getContentFrom(url);
        if (contentString == null) {
            return null;
        }

        Document document = Jsoup.parse(contentString);
        Element body = document.body();
        if (body == null) {
            return null;
        }

        Article article = null;
        Element headlineElement = body.getElementById("headline");
        if (headlineElement != null) {
            article = new Article();
            article.setHeadline(headlineElement.text());
        }

        List<Element> storyBodies = body.getElementsByClass("story-body");
        if (article != null) {
            StringBuilder sb = new StringBuilder();
            for (Element storyBody : storyBodies) {
                for (Element storyBodyText : storyBody.getElementsByClass("story-body-text")) {
                    sb.append(storyBodyText.text()).append("\n");
                }
            }
            article.setText(sb.toString());
        }

        Content content = null;
        if (article != null) {
            if (contentDao.exists(url)) {
                return null;
            } else {
                Content.ContentType contentType = Content.ContentType.Article;
                content = contentDao.insert(contentType, url, contentString);
                System.out.println("Inserted content of " + url);

                ContentCategory category = toContentCategory(url);
                category.setContentId(content.getContentId());
                contentCategoryDAO.insert(category);
            }

            article.setContentId(content.getContentId());
            try {
                articleDAO.insert(article);
            } catch (DAOException e) {
            }
        }

        Map<String, List<String>> urlStringMap = new LinkedHashMap();
        body.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (!(node instanceof Element)) {
                    return;
                }
                Element element = (Element) node;
                if (!"a".equals(element.tagName())) {
                    return;
                }

                String href = element.attr("href");
                if (!isAvailable(url, href)) {
                    return;
                }

                String text = element.text();

                List<String> texts;
                if (urlStringMap.containsKey(href)) {
                    texts = urlStringMap.get(href);
                } else {
                    texts = new ArrayList();
                    urlStringMap.put(href, texts);
                }

                texts.add(text);
            }

            @Override
            public void tail(Node node, int depth) {
                if (!(node instanceof Element)) {
                    return;
                }
                Element element = (Element) node;
                if (!"a".equals(element.tagName())) {
                    return;
                }
            }
        });

        List<String> hrefs = new ArrayList(urlStringMap.keySet());
        Collections.sort(hrefs);

        for (String href : hrefs) {
            URL contentUrl = null;
            try {
                contentUrl = new URL(href);
            } catch (MalformedURLException e) {
            }

            Worker worker = new Worker(content, contentUrl, urlStringMap.get(href));
            queue.offer(worker);
        }

        return content;
    }

    private ContentCategory toContentCategory(URL url) {
        String urlPath = url.getPath();

        StringTokenizer tokenizer = new StringTokenizer(urlPath, "/");
        Pattern digitPattern = Pattern.compile("[\\d]+");

        String topCategory = null;
        String subCategory = null;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token == null || token.length() == 0) {
                continue;
            }

            if (digitPattern.matcher(token).matches()) {
                continue;
            }

            if (token.endsWith(".html")) {
                break;
            }

            if (topCategory == null) {
                topCategory = token;
            } else {
                subCategory = token;
            }
        }

        ContentCategory category = new ContentCategory();
        category.setTopCategory(topCategory);
        category.setSubCategory(subCategory);

        return category;
    }

    public class Worker implements Runnable {

        private Content content;
        private URL href;
        private List<String> linkTexts;

        public Worker(Content content, URL href, List<String> linkTexts) {
            this.content = content;
            this.href = href;
            this.linkTexts = linkTexts;
        }

        @Override
        public void run() {
            Content target = null;
            try {
                target = scrap(href);
            } catch (IOException | DAOException e) {
            }

            if (content != null && target != null) {
                if (content.getContentType() == Content.ContentType.Article &&
                    target.getContentType() == Content.ContentType.Article) {

                    ContentRelation contentRelation = new ContentRelation();
                    contentRelation.setSourceContentId(content.getContentId());
                    contentRelation.setTargetContentId(target.getContentId());

                    for (String text : linkTexts) {
                        contentRelation.setLinkText(text);
                        try {
                            contentRelationDAO.insert(contentRelation);
                        } catch (DAOException e) {
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = "https://www.nytimes.com/";
        new Bootstrapper().scrap(new URL(baseUrl));
    }
}
