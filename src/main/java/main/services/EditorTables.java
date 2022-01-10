package main.services;

import lombok.SneakyThrows;
import main.data.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EditorTables {
    private static float weightTitleLemmas = 0;
    private static float weightBodyLemmas = 0;
    private final SiteEntity SITE;
    private final PageInfo PAGE_INFO;
    private boolean pageExist;
    private final String URL_WITHOUT_DOMAIN;
    private final PageEntity PAGE;
    private final static Set<SiteEntity> SITES_FOR_INDEXING = new HashSet<>();

    public EditorTables(SiteEntity site, PageInfo pageInfo) {
        SITE = site;
        PAGE_INFO = pageInfo;
        URL_WITHOUT_DOMAIN = PAGE_INFO.getURL().replace(SITE.getUrl(), "");
        PAGE = new PageEntity();
    }

    public synchronized void fillTablePages() {
        pageExist = isTablePagesContainsPage();
        if (!isPageExist()) {
            try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
                Transaction transaction = session.beginTransaction();
                int statusCode = PAGE_INFO.getStatusCode();
                PAGE.setPath(URL_WITHOUT_DOMAIN);
                PAGE.setCode(statusCode);
                PAGE.setContent(PAGE_INFO.getLinesWithPageHtmlCode());
                PAGE.setSite(SITE);
                session.save(PAGE);
                transaction.commit();
            }
        }
    }

    public void fillTableLemmas() {
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            String sql = "INSERT INTO lemmas(lemma, frequency)" +
                    "VALUES" + createMultiValuesForInsertLemmas() +
                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1";
            session.createNativeQuery(sql).executeUpdate();
            transaction.commit();
        }
    }

    public static void fillTableSitesLemmas() {
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            String sql = "insert into sites_lemmas(sites_id,lemmas_id) " +
                    "select pages.site_id, search_indexes.lemma_id from search_indexes " +
                    "join pages on pages.id = search_indexes.page_id " +
                    "ON DUPLICATE KEY UPDATE sites_id = sites_id, lemmas_id = lemmas_id";
            session.createNativeQuery(sql).executeUpdate();
            transaction.commit();
        }
    }

    public void fillTableSearchIndex() {
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            String sql = "Insert into search_indexes(lemma_id, page_id,ranks)" +
                    "values" + createMultiValuesForInsertSearchIndex() +
                    "ON DUPLICATE KEY UPDATE lemma_id = lemma_id,page_id = page_id,ranks = ranks";
            session.createNativeQuery(sql).executeUpdate();
            transaction.commit();
        }
    }

    @SneakyThrows
    public static void fillTableSites(String[] arrayUrl, String[] arrayName) {
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            for (int i = 0; i < arrayUrl.length; i++) {
                Optional siteEntity = session.createQuery("from SiteEntity where url = '" + arrayUrl[i] + "'").stream().findAny();
                if (!(siteEntity.isPresent())) {
                    SiteEntity site = new SiteEntity();
                    site.setUrl(arrayUrl[i]);
                    site.setName(arrayName[i]);
                    site.setStatus_time(new Date());
                    site.setStatus(SiteIndexingStatus.NOT_INDEXED);
                    SITES_FOR_INDEXING.add(site);

                    session.save(site);
                } else {
                    SITES_FOR_INDEXING.add((SiteEntity) siteEntity.get());
                }
            }
            transaction.commit();
        }
    }

    private String createMultiValuesForInsertSearchIndex() {
        StringBuilder valuesSearchIndex = new StringBuilder();
        int countRepeatInTitle;
        int countRepeatInBody;
        for (String oneLemma : PAGE_INFO.getAllLemmasInPage()) {
            countRepeatInTitle = PAGE_INFO.getLemmasAndCountLemmasInTitle().getOrDefault(oneLemma, 0);
            countRepeatInBody = PAGE_INFO.getLemmasAndCountLemmasInBody().getOrDefault(oneLemma, 0);
            valuesSearchIndex.append((valuesSearchIndex.length() == 0 ? "" : ",") +
                    "((select id from lemmas where lemma = '" + oneLemma + "')," + PAGE.getId() +"," +
                    calculateRankLemmas(countRepeatInTitle, countRepeatInBody) + ")");
        }
        return valuesSearchIndex.toString();
    }

    private String createMultiValuesForInsertLemmas() {
        StringBuilder valuesLemmas = new StringBuilder();
        for (String oneLemma : PAGE_INFO.getAllLemmasInPage()) {
            valuesLemmas.append((valuesLemmas.length() == 0 ? "" : ",") + "('" + oneLemma + "',1)");
        }
        return valuesLemmas.toString();
    }

    private float calculateRankLemmas(int countRepeatInTitle, int countRepeatInBody) {
        return (getWeightTitleLemmas() * countRepeatInTitle) + (getWeightBodyLemmas() * countRepeatInBody);
    }

    private boolean isTablePagesContainsPage() {
        boolean resultQuery;
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            String sql = "select id from pages where path = '" + URL_WITHOUT_DOMAIN + "' " +
                    "and site_id = '" + SITE.getId() + "'";
            resultQuery = session.createNativeQuery(sql).stream().findFirst().isPresent();
            transaction.commit();
        }
        return resultQuery;
    }

    private Float getWeightTitleLemmas() {
        if (weightTitleLemmas == 0) {
            try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
                weightTitleLemmas = session.get(FieldEntity.class, "title").getWeight();
            }
        }
        return weightTitleLemmas;
    }

    private Float getWeightBodyLemmas() {
        if (weightBodyLemmas == 0) {
            try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
                weightBodyLemmas = session.get(FieldEntity.class, "body").getWeight();
            }
        }
        return weightBodyLemmas;
    }

    public boolean isPageExist() {
        return pageExist;
    }

    public static void clearTables(String... nameTables) {
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            for (String nameTable : nameTables) {
                String truncateLemmaEntity = "TRUNCATE " + nameTable;
                session.createNativeQuery(truncateLemmaEntity).executeUpdate();
            }
            transaction.commit();
        }
    }

    public static Set<SiteEntity> getSitesForIndexing() {
        return SITES_FOR_INDEXING;
    }
}
