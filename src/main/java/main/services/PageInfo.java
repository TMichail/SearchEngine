package main.services;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PageInfo {
    private final String URL;
    private int statusCode;
    private HashMap<String, Integer> lemmasAndTheirCountInTitle;
    private HashMap<String, Integer> lemmasAndTheirCountInBody;
    private final Set<String> allLemmasInPage = new HashSet<>();
    private String title;
    private String body;
    private Connection.Response response;
    private Document pageHtmlCode;

    public PageInfo(String URL) {
        this.URL = URL;
        Connection connection = Jsoup.connect(URL)
                .userAgent("HeliontSearchBot")
                .referrer("http://www.google.com");
        try {
            response = connection.execute();
            pageHtmlCode = response.parse();
            statusCode = response.statusCode();
        } catch (HttpStatusException httpStatusException) {
            statusCode = httpStatusException.getStatusCode();
        } catch (IOException ioException) {
            statusCode = 0;
        }
    }

    public String getTitle() {
        if (title == null) {
            title = Jsoup.parse(getLinesWithPageHtmlCode()).title();
        }
        return title;
    }

    public String getBody() {
        if (body == null) {
            body = Jsoup.parse(getLinesWithPageHtmlCode()).body().text();
        }
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getLinesWithPageHtmlCode() {
        if (pageHtmlCode == null) {
            return "";
        }
        return pageHtmlCode.toString();
    }

    public HashMap<String, Integer> getLemmasAndCountLemmasInTitle() {
        if (lemmasAndTheirCountInTitle == null) {
            lemmasAndTheirCountInTitle = Lemmatizer.getMapWithLemmasAndCountLemmasInText(getTitle());
        }
        return lemmasAndTheirCountInTitle;
    }

    public HashMap<String, Integer> getLemmasAndCountLemmasInBody() {
        if (lemmasAndTheirCountInBody == null) {
            lemmasAndTheirCountInBody = Lemmatizer.getMapWithLemmasAndCountLemmasInText(getBody());
        }
        return lemmasAndTheirCountInBody;
    }

    public Set<String> getAllLemmasInPage() {
        if (allLemmasInPage.isEmpty()) {
            allLemmasInPage.addAll(getLemmasAndCountLemmasInBody().keySet());
            allLemmasInPage.addAll(getLemmasAndCountLemmasInTitle().keySet());
        }
        return allLemmasInPage;
    }

    @lombok.SneakyThrows
    public Set<String> getAllLinksOnPage() {
        Set<String> allLinksOnPage = new HashSet<>();
        if (pageHtmlCode == null)
            return allLinksOnPage;
        pageHtmlCode.select("a[href]").forEach(element -> {
            String subString = element.absUrl("abs:href");
            allLinksOnPage.add(subString);
        });
        return deleteIncorrectLinks(allLinksOnPage);
    }

    private Set<String> deleteIncorrectLinks(Set<String> allLinks) {
        Set<String> correctLink = new HashSet<>();
        Pattern pattern = createPatternForCheckLinksForCorrectness(URL);
        allLinks.forEach(link -> {
            if (pattern.matcher(link).matches()) {
                correctLink.add(link);
            }
        });
        return correctLink;
    }

    private static Pattern createPatternForCheckLinksForCorrectness(String url) {
        String REGEX = url + "[^#]+";
        return Pattern.compile(REGEX);
    }

    public String getURL() {
        return URL;
    }

}
