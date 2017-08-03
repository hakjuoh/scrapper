package oh.hakju.scrapper;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Bootstrapper {

    public String getContentFrom(URL url) throws IOException {
        List<String> lines;
        HttpURLConnection connection = openConnection(url);
        try {
            lines = IOUtils.readLines(connection.getInputStream(), "UTF-8");
        } finally {
            connection.disconnect();
        }

        return toString(lines);
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

    public static void main(String[] args) throws Exception {

        String baseUrl = "https://www.nytimes.com";
        String content = new Bootstrapper().getContentFrom(new URL(baseUrl));
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
                if (!href.startsWith(baseUrl)) {
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
            System.out.println(href + " --> " + urlStringMap.get(href));
        }
    }
}
