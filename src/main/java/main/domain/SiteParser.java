package main.domain;

import lombok.SneakyThrows;
import main.data.*;
import main.services.EditorTables;
import main.services.PageInfo;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveTask<SiteIndexingStatus> {
    private final PageInfo PAGE_INFO;
    private final SiteEntity SITE;
    private static boolean indexingStarted;
    private final EditorTables EDITOR_TABLE;

    public SiteParser(SiteEntity site, String url) {
        SITE = site;
        PAGE_INFO = new PageInfo(url);
        EDITOR_TABLE = new EditorTables(SITE, PAGE_INFO);
    }

    @Override
    protected SiteIndexingStatus compute() {
        List<SiteParser> tasks = new LinkedList<>();
        if (!indexingStarted) {
            tasks.forEach(task -> task.cancel(true));
            return SiteIndexingStatus.NOT_INDEXED;
        }
        EDITOR_TABLE.fillTablePages();
        if (!(EDITOR_TABLE.isPageExist())   ) {
            fillTablesForIndexing();
            for (String link : PAGE_INFO.getAllLinksOnPage()) {
                SiteParser siteParser = new SiteParser(SITE, link);
                siteParser.fork();
                tasks.add(siteParser);
            }
            tasks.forEach(ForkJoinTask::join);
        }
        return indexingStarted ? SiteIndexingStatus.INDEXED : SiteIndexingStatus.NOT_INDEXED;
    }

    @SneakyThrows
    private void fillTablesForIndexing() {
        Thread.sleep(1500);
        if (PAGE_INFO.getLinesWithPageHtmlCode().length() > 0) {
            EDITOR_TABLE.fillTableLemmas();
            EDITOR_TABLE.fillTableSearchIndex();
        }

    }

    public static boolean isIndexingStarted() {
        return indexingStarted;
    }

    public static void setIndexingStarted(boolean indexingStarted) {
        SiteParser.indexingStarted = indexingStarted;
    }

    public PageInfo getPAGE_INFO() {
        return PAGE_INFO;
    }
}
