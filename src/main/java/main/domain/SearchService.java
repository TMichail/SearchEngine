package main.domain;

import main.data.*;
import main.services.EditorTables;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SearchService {
    private final JSONObject NEGATIVE_RESULT_REQUEST;

    public SearchService() {
        NEGATIVE_RESULT_REQUEST = new JSONObject();
        NEGATIVE_RESULT_REQUEST.put("result", false);
        NEGATIVE_RESULT_REQUEST.put("error", "");
    }

    public JSONObject searchPages(String query, int offset, byte limit, String site) {
        SearchResult searchResult = new SearchResult(query);
        if (isRequestCorrect(query, site, searchResult)) {
            List<PageEntity> pages = searchResult.getAllPagesWhereMeetAllLemmasFromRequest();
            if (pages.isEmpty()) {
                NEGATIVE_RESULT_REQUEST.replace("error", "По данному запросу, ничего не найдено");
                return NEGATIVE_RESULT_REQUEST;
            }
            JSONObject correctResultRequest = new JSONObject();
            correctResultRequest.put("result", true);
            correctResultRequest.put("count", pages.size());
            correctResultRequest.put("data", getArrayPages(searchResult, pages, offset, limit));
            return correctResultRequest;
        } else return NEGATIVE_RESULT_REQUEST;
    }

    private JSONArray getArrayPages(SearchResult searchResult, List<PageEntity> pages, int offset, int limit) {
        JSONArray jsonArray = new JSONArray();
        double absoluteRelevanceFirstPage = searchResult.getAbsoluteRelevance(pages.get(0));
        for (int i = offset; i < offset + limit; i++) {
            if (i >= pages.size())
                break;
            PageEntity page = pages.get(i);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("site", page.getSite().getUrl());
            jsonObject.put("siteName", page.getSite().getName());
            jsonObject.put("uri", page.getPath());
            jsonObject.put("title", Jsoup.parse(page.getContent()).title());
            jsonObject.put("snippet", getSnippet(searchResult, page));
            jsonObject.put("relevance", i == 0 ? 1.0 : searchResult.getAbsoluteRelevance(page) / absoluteRelevanceFirstPage);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private String getSnippet(SearchResult searchResult, PageEntity page) {
        StringBuilder snippet = new StringBuilder();
        StringBuilder content = new StringBuilder(Jsoup.clean(Jsoup.parseBodyFragment(page.getContent()).body().toString(), Whitelist.none()));
        int lengthContent = content.length();
        for (String lemma : searchResult.getLEMMAS_FROM_REQUEST()) {
            if (snippet.toString().contains(lemma)) {
                continue;
            }
            int startWord = content.indexOf(lemma);
            if (startWord == -1)
                continue;
            int endWord = content.indexOf(" ", startWord);
            snippet.append("<b>").append(content.substring(startWord, endWord)).append("</b>").append(endWord + 20 > lengthContent ? content.substring(endWord) : content.substring(endWord, content.indexOf(" ", endWord + 20)));
        }
        return snippet.toString();
    }

    private boolean isRequestCorrect(String query, String siteURL, SearchResult searchResult) {
        Set<SiteEntity> sitesSet = EditorTables.getSitesForIndexing();
        SiteEntity siteForSearch = null;
        if (query.isEmpty()) {
            NEGATIVE_RESULT_REQUEST.replace("error", "Задан пустой поисковый запрос");
            return false;
        }
        if (!(checkHasSiteIndexed(sitesSet))){
            NEGATIVE_RESULT_REQUEST.replace("error", "Отсутствуют проиндексированные сайты");
            return false;
        }
        if (siteURL != null) {
            for (SiteEntity site : sitesSet)
                if (site.getUrl().equals(siteURL)) {
                    siteForSearch = site;
                    searchResult.setSiteForSearch(siteForSearch);
                }
        }
        if (siteForSearch != null && !(siteForSearch.getStatus().equals(SiteIndexingStatus.INDEXED))){
            NEGATIVE_RESULT_REQUEST.replace("error", "Сайт, по которому осуществляется поиск, не проиндексирован");
            return false;
        }
        return true;
    }

    private boolean checkHasSiteIndexed(Set<SiteEntity> sitesSet){
        for (SiteEntity site : sitesSet){
            if (site.getStatus().equals(SiteIndexingStatus.INDEXED)){
                return true;
            }
        }
        return false;
    }

}
