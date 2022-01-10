package main.controller;

import main.domain.SearchService;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public JSONObject searchPage(@RequestParam String query, @RequestParam int offset, @RequestParam byte limit, @RequestParam(required = false) String site){
        return searchService.searchPages(query,offset,limit,site);
    }
}
