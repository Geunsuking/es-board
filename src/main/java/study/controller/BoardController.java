package study;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller // RestController에서 Controller로 변경!
public class BoardController {

    private final RestHighLevelClient client;

    public BoardController(RestHighLevelClient client) {
        this.client = client;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "query", required = false) String query, Model model) throws IOException {
        if (query == null || query.isEmpty()) {
            return "search";
        }

        SearchRequest searchRequest = new SearchRequest("fruit");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.wildcardQuery("name", "*" + query + "*"));
        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // [수정 포인트] 복잡한 JSON 대신 실제 데이터 리스트만 추출
        java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        response.getHits().forEach(hit -> results.add(hit.getSourceAsMap()));

        model.addAttribute("results", results); // 리스트 형태로 전달
        model.addAttribute("query", query);

        return "search";
    }
    // 데이터를 저장하는 기능 (Create)
    @PostMapping("/save")
    public String save(@RequestParam(name = "name") String name) throws java.io.IOException {
        // 1. 엘라스틱서치에 넣을 요청서를 만듭니다 (인덱스 이름: fruit)
        org.elasticsearch.action.index.IndexRequest request = new org.elasticsearch.action.index.IndexRequest("fruit");
        request.id(name);
        // 2. 입력받은 이름을 데이터로 담습니다
        request.source("name", name);

        // 3. 엘라스틱서치에 전송하여 저장합니다
        client.index(request, org.elasticsearch.client.RequestOptions.DEFAULT);

        // 4. 저장이 완료되면 다시 메인 화면(/search)으로 돌아갑니다
        return "redirect:/search";
    }
}