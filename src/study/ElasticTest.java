package study;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class ElasticTest {
    public static void main(String[] args) throws Exception {
        // 1. 연결
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        // 2. 검색 요청서 만들기
        SearchRequest searchRequest = new SearchRequest("my_index");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // [핵심] "title" 필드에서 "사과"라는 단어가 포함된 것만 찾아라!
        searchSourceBuilder.query(QueryBuilders.matchQuery("title", "사과"));
        searchRequest.source(searchSourceBuilder);

        // 3. 검색 실행 및 결과 받기
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // 4. 결과 출력
        System.out.println("🍎 '사과' 검색 결과 🍎");
        for (SearchHit hit : response.getHits().getHits()) {
            System.out.println("검색된 내용: " + hit.getSourceAsString());
        }

        client.close();
    }
}