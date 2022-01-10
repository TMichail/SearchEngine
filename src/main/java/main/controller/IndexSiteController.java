package main.controller;

import main.domain.IndexSiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


@RestController
public class IndexSiteController {

    private final IndexSiteService indexSiteService;

    public IndexSiteController(IndexSiteService indexSiteService) {
        this.indexSiteService = indexSiteService;
    }

    @GetMapping("/startIndexing")
    public ResponseEntity fullIndexingSites() {
        return indexSiteService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexingSites(){
        return indexSiteService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestBody String url) throws UnsupportedEncodingException {
        return indexSiteService.indexPage(URLDecoder.decode(url, StandardCharsets.UTF_8.toString()).replace("url=",""));
    }
}
