package esboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import esboard.model.BoardItem;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;



@Service
public class BoardService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ObjectMapper objectMapper;

    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString()))
            .create();

    // 1. 저장 (날짜 저장 방식 최적화)
    public void save(BoardItem item) throws Exception {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("title", item.getTitle());
        jsonMap.put("content", item.getContent());
        jsonMap.put("author", item.getAuthor());
        // ISO_DATE_TIME 형식으로 저장해야 엘라스틱서치가 날짜로 잘 인식합니다.
        jsonMap.put("createdAt", item.getCreatedAt().toString());

        IndexRequest request = new IndexRequest("board")
                .id(item.getId())
                .source(jsonMap);

        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.index(request, RequestOptions.DEFAULT);
    }

    // 2. 검색 및 전체 목록 조회 (정렬 로직 포함!)
    public Map<String, Object> search(String keyword, int page) {
        System.out.println("🚀 현재 요청된 페이지: " + page);
        int size = 10;
        int from = page * size;

        List<BoardItem> list = new ArrayList<>();
        long totalCount = 0; // 🚀 전체 개수를 담을 변수 추가

        try {
            SearchRequest searchRequest = new SearchRequest("board");
            // 1. 여기서부터 (SearchSourceBuilder 생성 아래)
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

// 🚀 [A] 검색 쿼리 설정
            if (keyword != null && !keyword.isEmpty()) {
                sourceBuilder.query(QueryBuilders.queryStringQuery("*" + keyword + "*")
                        .field("title")
                        .field("content")
                        .field("author")
                        .analyzeWildcard(true));
            } else {
                sourceBuilder.query(QueryBuilders.matchAllQuery());
            }

// 🚀 [B] 하이라이트 설정 (이 부분만 남기고 기존의 다른 하이라이트 코드는 다 지우세요!)
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<em style='background-color: yellow; font-style: normal;'>");
            highlightBuilder.postTags("</em>");

// 제목 정밀 설정
            highlightBuilder.field(new HighlightBuilder.Field("title")
                    .highlighterType("plain")
                    .fragmentSize(0)
                    .boundaryScannerType(HighlightBuilder.BoundaryScannerType.CHARS));

// 내용 정밀 설정
            highlightBuilder.field(new HighlightBuilder.Field("content")
                    .highlighterType("plain")
                    .fragmentSize(0)
                    .boundaryScannerType(HighlightBuilder.BoundaryScannerType.CHARS));

// 작성자 정밀 설정
            highlightBuilder.field(new HighlightBuilder.Field("author")
                    .highlighterType("plain")
                    .fragmentSize(0)
                    .boundaryScannerType(HighlightBuilder.BoundaryScannerType.CHARS));

// --- 여기까지가 하이라이트 끝입니다! ---

            sourceBuilder.sort("createdAt", SortOrder.DESC);
            sourceBuilder.from(from);
            sourceBuilder.size(size);

            searchRequest.source(sourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            // 🚀 추가: 엘라스틱서치에서 진짜 전체 개수(예: 13개)를 가져옵니다.
            totalCount = response.getHits().getTotalHits().value;

            for (SearchHit hit : response.getHits().getHits()) {
                BoardItem item = gson.fromJson(hit.getSourceAsString(), BoardItem.class);
                item.setId(hit.getId());

                Map<String, org.elasticsearch.search.fetch.subphase.highlight.HighlightField> highlightFields = hit.getHighlightFields();

                // 🚀 제목 하이라이트
                if (highlightFields.containsKey("title")) {
                    item.setTitle(highlightFields.get("title").fragments()[0].string());
                }
                // 🚀 내용 하이라이트 (BoardItem에 content 필드가 있다면)
                if (highlightFields.containsKey("content")) {
                    item.setContent(highlightFields.get("content").fragments()[0].string());
                }
                // 🚀 작성자 하이라이트
                if (highlightFields.containsKey("author")) {
                    item.setAuthor(highlightFields.get("author").fragments()[0].string());
                }

                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🚀 마지막: 리스트와 전체개수를 Map에 담아서 보냅니다.
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", totalCount);
        return result;
    }

    // 3. 상세 보기
    public BoardItem findById(String id) {
        try {
            GetRequest getRequest = new GetRequest("board", id);
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            if (response.isExists()) {
                BoardItem item = gson.fromJson(response.getSourceAsString(), BoardItem.class);
                item.setId(response.getId());
                return item;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. 삭제
    public void delete(String id) {
        try {
            DeleteRequest request = new DeleteRequest("board", id);
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client.delete(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}