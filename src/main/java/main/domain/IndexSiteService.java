package main.domain;

import main.data.*;
import main.services.EditorTables;
import main.services.PageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IndexSiteService {
    private final SiteRepository SITE_REPOSITORY;
    private final Set<SiteEntity> SITE_ENTITY_SET;
    private final HashMap<String, Object> NEGATIVE_RESULT_REQUEST = new HashMap<>();
    public static boolean sitesIndexing = false;
    private final HashMap<String, Object> POSITIVE_RESULT_REQUEST = new HashMap<>();

    public IndexSiteService(SiteRepository siteRepository, @Value("${sites.url}") String[] ARRAY_URL, @Value("${sites.name}") String[] ARRAY_NAME) {
        SITE_REPOSITORY = siteRepository;
        EditorTables.fillTableSites(ARRAY_URL, ARRAY_NAME);
        SITE_ENTITY_SET = EditorTables.getSitesForIndexing();
        NEGATIVE_RESULT_REQUEST.put("result", false);
        NEGATIVE_RESULT_REQUEST.put("error", "");
        POSITIVE_RESULT_REQUEST.put("result", true);
    }

    public ResponseEntity startIndexing() {
        if (SiteParser.isIndexingStarted()) {
            NEGATIVE_RESULT_REQUEST.replace("error", "Индексация уже запущена");
            return new ResponseEntity(NEGATIVE_RESULT_REQUEST, HttpStatus.OK);
        }
        sitesIndexing = false;
        prepareSitesForIndexing();
        new Thread(() -> {
            SITE_ENTITY_SET.forEach(this::indexingSite);
            EditorTables.fillTableSitesLemmas();
            SiteParser.setIndexingStarted(false);
            sitesIndexing = true;
        }).start();
        return new ResponseEntity(POSITIVE_RESULT_REQUEST, HttpStatus.OK);
    }

    private void indexingSite(SiteEntity site) {
        site.setStatus(SiteIndexingStatus.INDEXING);
        SITE_REPOSITORY.save(site);
        SiteParser siteParser = new SiteParser(site, site.getUrl() + "/");
        int statusCodeSite = siteParser.getPAGE_INFO().getStatusCode();
        if (statusCodeSite > 400 || statusCodeSite == 0) {
            site.setStatus(SiteIndexingStatus.FAILED);
            site.setLast_error("Ошибка индексации, главная страница сайта не доступна");
            SITE_REPOSITORY.save(site);
        } else {
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPool.invoke(siteParser);
            site.setStatus(siteParser.getRawResult());
            SITE_REPOSITORY.save(site);
            forkJoinPool.shutdown();
        }
    }

    private void prepareSitesForIndexing() {
        EditorTables.clearTables("lemmas", "pages", "search_indexes", "sites_lemmas");
        SiteParser.setIndexingStarted(true);
        SITE_ENTITY_SET.forEach(site -> {
            site.setStatus_time(new Date());
            site.setLast_error("");
            site.setStatus(SiteIndexingStatus.NOT_INDEXED);
            SITE_REPOSITORY.save(site);
        });
    }

    public ResponseEntity stopIndexing() {
        if (!SiteParser.isIndexingStarted()) {
            NEGATIVE_RESULT_REQUEST.replace("error", "Индексация не запущена");
            return new ResponseEntity(NEGATIVE_RESULT_REQUEST, HttpStatus.OK);
        }
        SiteParser.setIndexingStarted(false);
        return new ResponseEntity(POSITIVE_RESULT_REQUEST, HttpStatus.OK);
    }

    public ResponseEntity indexPage(String url) {
        SiteEntity site = getSiteFromUrl(url);
        if (site.getUrl() == null) {
            NEGATIVE_RESULT_REQUEST.replace("error", "Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
            return new ResponseEntity(NEGATIVE_RESULT_REQUEST, HttpStatus.OK);
        } else {
            if (fillTablesForPage(site, url)) {
                EditorTables.fillTableSitesLemmas();
                return new ResponseEntity(POSITIVE_RESULT_REQUEST, HttpStatus.OK);
            } else {
                NEGATIVE_RESULT_REQUEST.replace("error", "Данная страница уже была проиндексированна");
                return new ResponseEntity(NEGATIVE_RESULT_REQUEST, HttpStatus.OK);
            }
        }
    }

    private boolean fillTablesForPage(SiteEntity site, String url) {
        PageInfo pageInfo = new PageInfo(url);
        EditorTables editorTables = new EditorTables(site, pageInfo);
        editorTables.fillTablePages();
        if (editorTables.isPageExist())
            return false;
        if (pageInfo.getLinesWithPageHtmlCode().length() > 0) {
            editorTables.fillTableLemmas();
            editorTables.fillTableSearchIndex();
        }
        return true;
    }

    private SiteEntity getSiteFromUrl(String url) {
        SiteEntity siteFromUrl = new SiteEntity();
        String urlDomainName = getDomainNameFromURL(url);
        for (SiteEntity site : SITE_ENTITY_SET) {
            if (site.getUrl().equals(urlDomainName)) {
                siteFromUrl = site;
                break;
            }
        }
        return siteFromUrl;
    }

    private static String getDomainNameFromURL(String url) {
        String regex = "http[s]?:\\/\\/[^\\/]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        int start = 0;
        int end = 0;
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }
        return url.substring(start, end);
    }

    public static boolean isSitesIndexing() {
        return sitesIndexing;
    }
}
