package esboard.service;

import com.google.gson.*;
import esboard.model.BoardItem;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

    // 날짜를 예쁘게 처리할 수 있는 똑똑한 Gson 만들기
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString());
                }
            })
            .create();

    public void save(BoardItem item) throws Exception {
        Map<String, Object> JsonMap = new HashMap<>();
        JsonMap.put("title", item.getTitle());
        JsonMap.put("content", item.getContent());
        JsonMap.put("author", item.getAuthor());
        JsonMap.put("createdAt", item.getCreatedAt().toString());

        IndexRequest request = new IndexRequest("board")
                .id(item.getId())
                .source(JsonMap);

        client.index(request, RequestOptions.DEFAULT);
    }
    // BoardService.java 파일 안에 추가

    public List<BoardItem> findAll() {
        try {
            // 1. "board"라는 이름의 창고(인덱스)에 요청을 보냅니다.
            SearchRequest searchRequest = new SearchRequest("board");

            // 2. "아무 조건 없이 전부 다(matchAll) 찾아줘"라고 설정합니다.
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.matchAllQuery());
            searchRequest.source(sourceBuilder);

            // 3. 엘라스틱서치 서버에 명령을 쏘고 응답을 받습니다.
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            // 4. 받은 데이터(JSON)를 우리가 만든 그릇(BoardItem)에 담아서 리스트로 만듭니다.
            return Arrays.stream(response.getHits().getHits())
                    .map(hit -> {
                        // JSON -> BoardItem 객체로 변환
                        BoardItem item = gson.fromJson(hit.getSourceAsString(), BoardItem.class);
                        item.setId(hit.getId()); // 엘라스틱서치가 부여한 ID도 담아줍니다.
                        return item;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 에러가 나면 빈 목록을 돌려줍니다.
            return new ArrayList<>();
        }
    }
}
