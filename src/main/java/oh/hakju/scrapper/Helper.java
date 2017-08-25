package oh.hakju.scrapper;

import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class Helper {

    private static final Map<String, String> cookies = new HashMap();
    static {
        cookies.put("nytimes.com", "vi_www_hp=z09; NYT_W2=New%20YorkNYUS|ChicagoILUS|London--UK|Los%20AngelesCAUS|San%20FranciscoCAUS|Tokyo--JP; nyt-d=101.56D0o1A00CAI0H050s9Iny1/7GKH074oKi0a6miq0cAted0rCJSY1ySMCa0f5Gn/0zB0SA1w70SI0M6tmN0vCp0g0aVm0U0K2Wnv0eCoir0kVm0U032W9v0UUXm70I4HeU0D77nr1fTd9h1pTszw0S1m8908V7TX1s3s1w@5ecf28b9/9c77a970; NYT-S=38dxzheUfULGuEdP16THbD0Jx0gxD7Ohg..NyO4zEeqpVs4btuPYvDvq3cTtfmyFu71rgTV.ppm/LCE4Nc3kQgOOVz3YNRgqj9rKVq8Q/dNnWyKW321p0tlc94OessMuKPcCkD3Hi2l2gtMAkSx1J2jLLdEXMyQl13zDE8rN0jmrcGarfwvL2Sx7dCAQ2Hyk76heIbpKocPfYCkw4nnkkINSbIpxzNAkFfDYIoGbfZTAsNpWntn7UQWehFD3vC/lNrl/B6iWk8lHU0; NYT-BCET=1503605455%7CK1H5igSfP8EXvGxmvjnrVfYLRcM%3D%7CY%3BX%7C66H4adfSy7YYlokx%2BBQnk%2Bxxhe5UwHQK%2FZBrVXIcLfM%3D; nyt-a=c8ff523221c495645c5c475a77495dffedfd524acee60bf50b9e45d2d2329af0");
    }

    public static String getContentFrom(URL url) throws IOException {
        List<String> lines;
        HttpURLConnection connection = openConnection(url);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 301 || responseCode == 302) {
                String redirectLocation = connection.getHeaderField("Location");
                return getContentFrom(new URL(redirectLocation));
            } else {
                String contentEncoding = connection.getHeaderField("Content-Encoding");
                InputStream inputStream = connection.getInputStream();
                if ("gzip".equalsIgnoreCase(contentEncoding)) {
                    inputStream = new GZIPInputStream(inputStream);
                } else if ("br".equalsIgnoreCase(contentEncoding)) {
                    inputStream = new BrotliCompressorInputStream(inputStream);
                }
                lines = IOUtils.readLines(inputStream, "UTF-8");
                return toString(lines);
            }
        } finally {
            connection.disconnect();
        }
    }

    public static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36");
        String host = url.getHost();
        if (cookies.containsKey(host)) {
            conn.setRequestProperty("Cookie", cookies.get(host));
        }
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8,ko;q=0.6,fr;q=0.4");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");

        return conn;
    }

    private static String toString(Collection<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }
        return sb.toString();
    }
}
