package main.domain;

import main.data.*;
import main.services.Lemmatizer;
import org.hibernate.Session;
import java.util.*;
import java.util.stream.Collectors;

public class SearchResult {
    private List<LemmaEntity> lemmasFromDB;
    private final Set<String> LEMMAS_FROM_REQUEST;
    private SiteEntity siteForSearch;

    public SearchResult(String searchRequest) {
        LEMMAS_FROM_REQUEST = Lemmatizer.getMapWithLemmasAndCountLemmasInText(searchRequest).keySet();
        lemmasFromDB = getLemmasFromDB();
    }

    private List<PageEntity> sortedPageEntityByRanks(Set<PageEntity> pages) {
        List<PageEntity> sortedPagesList;
        String hql = "from PageEntity pe where pe.id in " +
                "(select p.id from PageEntity p join SearchIndexEntity s on p.id = s.keySearchIndexEntity.page " +
                "where s.keySearchIndexEntity.page in (" + createMultiValuesForSelectPages(pages) + ") and s.keySearchIndexEntity.lemma in (select l.id from LemmaEntity l where l.lemma in (" + createMultiValuesForSelectLemmas(LEMMAS_FROM_REQUEST) + ")) group by s.keySearchIndexEntity.page order by sum(s.ranks) desc)";
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            sortedPagesList = session.createQuery(hql).list();
        }
        return sortedPagesList;
    }

    public Double getAbsoluteRelevance(PageEntity page){
        Optional<Double> result;
        String hql = "select sum(s.ranks) from PageEntity p join SearchIndexEntity s on p.id = s.keySearchIndexEntity.page " +
                "where s.keySearchIndexEntity.page = '" + page.getId() + "' and s.keySearchIndexEntity.lemma in (select l.id from LemmaEntity l where l.lemma in (" + createMultiValuesForSelectLemmas(LEMMAS_FROM_REQUEST) + ")) group by s.keySearchIndexEntity.page";
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            result = session.createQuery(hql).stream().findAny();
        }

        return result.isPresent() ? result.get() : 0;
    }

    public List<PageEntity> getAllPagesWhereMeetAllLemmasFromRequest() {
        if (lemmasFromDB.size() != LEMMAS_FROM_REQUEST.size() || lemmasFromDB.isEmpty()) {
            return new LinkedList<>();
        }
        lemmasFromDB = lemmasFromDB.stream()
                .filter(lemma -> lemma.getFrequency() < 300) // Исключение лемм из запроса, которые встречаются более чем на 300 страницах
                .sorted(Comparator.comparing(LemmaEntity::getFrequency))
                .collect(Collectors.toList());
        Set<PageEntity> pagesWhereMeetAllLemmasFromRequest;
        pagesWhereMeetAllLemmasFromRequest = getPagesWhereMeetLessPopularLemmaFromRequest();
        for (int i = 1; i < lemmasFromDB.size(); i++) {
            pagesWhereMeetAllLemmasFromRequest = getPagesWhereMeetLemmaFromOtherPages(pagesWhereMeetAllLemmasFromRequest, lemmasFromDB.get(i));
        }
        if (pagesWhereMeetAllLemmasFromRequest.isEmpty())
            return new LinkedList<>();
        return sortedPageEntityByRanks(pagesWhereMeetAllLemmasFromRequest);
    }

    private Set<PageEntity> getPagesWhereMeetLessPopularLemmaFromRequest() {
        Set<PageEntity> pages = new HashSet<>();
        String hql ="from SearchIndexEntity s where s.keySearchIndexEntity.lemma = " + lemmasFromDB.get(0).getId();
        if (siteForSearch != null){
            hql += " and s.keySearchIndexEntity.page in (select p.id from PageEntity p where p.site = " + siteForSearch.getId() + ")";
        }
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            List<SearchIndexEntity> list = session.createQuery(hql).list();
            list.forEach(index -> pages.add(index.getKeySearchIndexEntity().getPage()));
        }
        return pages;
    }

    private Set<PageEntity> getPagesWhereMeetLemmaFromOtherPages(Set<PageEntity> pages, LemmaEntity lemma) {
        Set<PageEntity> subPages = new HashSet<>();
        String pageIdValues = createMultiValuesForSelectPages(pages);
        if (pageIdValues.isEmpty())
            return subPages;
        String hql = "from SearchIndexEntity where page_id in (" + pageIdValues + ") and lemma_id = (select id from LemmaEntity where lemma = '" + lemma.getLemma() + "')";
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            List<SearchIndexEntity> list = session.createQuery(hql).list();
            list.forEach(index -> subPages.add(index.getKeySearchIndexEntity().getPage()));
        }
        return subPages;
    }

    private String createMultiValuesForSelectPages(Set<PageEntity> pageEntities) {
        StringBuilder valuesPages = new StringBuilder();
        for (PageEntity page : pageEntities) {
            valuesPages.append(valuesPages.length() == 0 ? "" : ",").append(page.getId());
        }
        return valuesPages.toString();
    }

    private List<LemmaEntity> getLemmasFromDB() {
        String multiValues = createMultiValuesForSelectLemmas(LEMMAS_FROM_REQUEST);
        if (multiValues.isEmpty()){
            return new LinkedList<>();
        }
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            String hqlSearchLemmas = "from LemmaEntity where lemma in (" + multiValues + ")";
            lemmasFromDB = session.createQuery(hqlSearchLemmas).list();
        }
        return lemmasFromDB;
    }

    private String createMultiValuesForSelectLemmas(Set<String> lemmasFromRequest) {
        StringBuilder valuesLemmas = new StringBuilder();
        for (String lemma : lemmasFromRequest) {
            valuesLemmas.append(valuesLemmas.length() == 0 ? "" : ",").append("'").append(lemma).append("'");
        }
        return valuesLemmas.toString();
    }

    public Set<String> getLEMMAS_FROM_REQUEST() {
        return LEMMAS_FROM_REQUEST;
    }

    public void setSiteForSearch(SiteEntity siteForSearch) {
        this.siteForSearch = siteForSearch;
    }
}
