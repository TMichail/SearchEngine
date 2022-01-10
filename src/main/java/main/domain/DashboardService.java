package main.domain;

import main.data.ConnectionToSessionFactory;
import main.data.SiteEntity;
import main.services.EditorTables;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
import java.util.Set;


@Service
public class DashboardService {


    public JSONObject getStatistics() {
        JSONObject result = new JSONObject();
        result.put("result",true);
        JSONObject statistics = new JSONObject();
        statistics.put("total",getTotalStatistics());
        statistics.put("detailed",getDetailedStatistics());
        result.put("statistics",statistics);
        return result;
    }

    private JSONObject getTotalStatistics(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sites",getCountSites());
        jsonObject.put("pages", getCountPagesForTotalStatistic());
        jsonObject.put("lemmas", getCountLemmasForTotalStatistics());
        jsonObject.put("isIndexing",IndexSiteService.isSitesIndexing());
        return jsonObject;
    }

    private JSONArray getDetailedStatistics(){
        Set<SiteEntity> sitesForDetailedInformation = EditorTables.getSitesForIndexing();
        JSONArray jsonArray = new JSONArray();
        sitesForDetailedInformation.forEach(site -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("url",site.getUrl());
            jsonObject.put("name",site.getName());
            jsonObject.put("status",site.getStatus());
            jsonObject.put("statusTime",site.getStatus_time());
            jsonObject.put("error",site.getLast_error());
            jsonObject.put("pages",getCountPagesForSite(site));
            jsonObject.put("lemmas",getCountLemmasForSite(site));
            jsonArray.add(jsonObject);
        });
        return jsonArray;
    }

    private long getCountPagesForSite(SiteEntity site){
        long count;
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
             count = session.createNativeQuery("select * from pages where site_id = " + site.getId()).stream().count();
        }
        return count;
    }

    private long getCountLemmasForSite(SiteEntity site){
        long count;
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            count = session.createNativeQuery("select * from sites_lemmas where sites_id = " + site.getId()).stream().count();
        }
        return count;
    }
    private long getCountSites(){
        long countSites;
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            countSites = session.createQuery("from SiteEntity").stream().count();
        }
        return countSites;
    }

    private long getCountPagesForTotalStatistic(){
        long countPages;
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            countPages = session.createQuery("from PageEntity").stream().count();
        }
        return countPages;
    }

    private long getCountLemmasForTotalStatistics(){
        long countLemmas;
        try (Session session = ConnectionToSessionFactory.getSessionFactory().openSession()) {
            countLemmas = session.createQuery("from LemmaEntity").stream().count();
        }
        return countLemmas;
    }

}
