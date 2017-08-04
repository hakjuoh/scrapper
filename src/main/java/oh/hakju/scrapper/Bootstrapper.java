package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.ContentDAO;
import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Bootstrapper {

    private ContentDAO contentDao = new ContentDAO();

    public String getContentFrom(URL url) throws IOException {
        List<String> lines;
        HttpURLConnection connection = openConnection(url);
        String redirectLocation = null;
        try {
            lines = IOUtils.readLines(connection.getInputStream(), "UTF-8");

            if (connection.getResponseCode() == 301) {
                redirectLocation = connection.getHeaderField("Location");
            }
        } finally {
            connection.disconnect();
        }

        if (redirectLocation != null) {
            return getContentFrom(new URL(redirectLocation));
        } else {
            return toString(lines);
        }
    }

    public HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private String toString(Collection<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }
        return sb.toString();
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

    public void scrap(URL url) throws IOException {
        if (contentDao.exists(url)) {
            return;
        }

        String content = getContentFrom(url);
        if (content == null) {
            return;
        }

        if (contentDao.exists(content)) {
            return;
        }

        contentDao.insert(url, content);
        System.out.println("Inserted content of " + url);

        Document document = Jsoup.parse(content);
        Map<String, List<String>> urlStringMap = new LinkedHashMap();

        Element body = document.body();
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

        hrefs.parallelStream().forEach(href -> {
            URL contentUrl = null;
            try {
                contentUrl = new URL(href);
            } catch (MalformedURLException e) {
            }
            try {
                scrap(contentUrl);
            } catch (IOException | DAOException e) {
            }
        });
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = "https://www.nytimes.com/";
        new Bootstrapper().scrap(new URL(baseUrl));
    }
}
