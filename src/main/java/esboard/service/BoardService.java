package esboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import esboard.model.BoardItem;
import esboard.model.CommentItem;
import esboard.repository.CommentRepository;
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

    @Autowired
    private CommentRepository commentRepository;

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
        jsonMap.put("views", item.getViews());

        IndexRequest request = new IndexRequest("board")
                .id(item.getId())
                .source(jsonMap);

        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        client.index(request, RequestOptions.DEFAULT);
    }

    // 2. 검색 및 전체 목록 조회 (정렬 로직 포함!)
    // 파라미터를 다시 3개(keyword, searchType, page)로 맞춥니다.
    public Map<String, Object> search(String keyword, String searchType, int page) {
        int size = 10;
        int from = page * size;
        List<BoardItem> list = new ArrayList<>();
        long totalCount = 0;

        try {
            SearchRequest searchRequest = new SearchRequest("board");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            // 🚀 검색 조건 설정
            if (keyword != null && !keyword.isEmpty()) {
                // 1. 앞뒤에 *를 붙여서 부분 검색 가능하게 만듭니다.
                var queryBuilder = QueryBuilders.queryStringQuery("*" + keyword + "*");

                // 2. 검색 필드 설정
                if ("title".equals(searchType)) {
                    queryBuilder.field("title");
                } else if ("content".equals(searchType)) {
                    queryBuilder.field("content");
                } else if ("title_content".equals(searchType)) {
                    queryBuilder.field("title").field("content");
                } else {
                    queryBuilder.field("title").field("content").field("author");
                }

                // [핵심] analyzeWildcard(true)는 유지하되,
                // 하이라이트가 문장 전체를 잡지 않도록 쿼리를 전달합니다.
                sourceBuilder.query(queryBuilder.analyzeWildcard(true));
            } else {
                sourceBuilder.query(QueryBuilders.matchAllQuery());
            }

            // 하이라이트 및 정렬 설정 (이전과 동일)
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<em>");
            highlightBuilder.postTags("</em>");

            highlightBuilder.field(new HighlightBuilder.Field("title")
                    .highlighterType("unified")
                    .numOfFragments(0));
            highlightBuilder.field(new HighlightBuilder.Field("content")
                    .highlighterType("unified")
                    .numOfFragments(0));
            highlightBuilder.field(new HighlightBuilder.Field("author")
                    .highlighterType("unified")
                    .numOfFragments(0));

            sourceBuilder.highlighter(highlightBuilder);
            sourceBuilder.sort("createdAt", SortOrder.DESC);
            sourceBuilder.from(from);
            sourceBuilder.size(size);

            searchRequest.source(sourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            totalCount = response.getHits().getTotalHits().value;

            for (SearchHit hit : response.getHits().getHits()) {
                BoardItem item = gson.fromJson(hit.getSourceAsString(), BoardItem.class);
                item.setId(hit.getId());
                Map<String, org.elasticsearch.search.fetch.subphase.highlight.HighlightField> highlightFields = hit.getHighlightFields();
                if (highlightFields.containsKey("title")) item.setTitle(highlightFields.get("title").fragments()[0].string());
                if (highlightFields.containsKey("content")) item.setContent(highlightFields.get("content").fragments()[0].string());
                if (highlightFields.containsKey("author")) item.setAuthor(highlightFields.get("author").fragments()[0].string());
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
            // [1] 게시글 삭제 (Elasticsearch RestHighLevelClient 사용)
            DeleteRequest request = new DeleteRequest("board", id);
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            client.delete(request, RequestOptions.DEFAULT);

            // [2] 해당 게시글의 댓글들 삭제 (Spring Data Repository 사용)
            // 게시글 ID(boardId)가 삭제하려는 게시글의 id와 일치하는 댓글들을 모두 가져옵니다.
            List<CommentItem> comments = commentRepository.findByBoardId(id);

            if (comments != null && !comments.isEmpty()) {
                commentRepository.deleteAll(comments); // 찾은 댓글 뭉치를 한꺼번에 삭제!
                System.out.println("🗑️ 게시글 [" + id + "]의 댓글 " + comments.size() + "건이 함께 삭제되었습니다.");
            }

        } catch (Exception e) {
            System.err.println("삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void incrementViews(String id) {
        try {
            BoardItem item = findById(id);
            if (item != null) {
                item.setViews(item.getViews() + 1);
                save(item); // 여기서 발생하는 Exception을 try-catch로 감싸줍니다.
            }
        } catch (Exception e) {
            // 로그를 남겨서 왜 업데이트가 안 됐는지 확인할 수 있게 합니다.
            System.err.println("조회수 업데이트 중 오류 발생: " + e.getMessage());
        }
    }
    // 1. 댓글 저장하기
    public void saveComment(CommentItem comment) {
        // 댓글 쓴 시간을 지금으로 설정
        comment.setCreatedAt(LocalDateTime.now());
        // 댓글 고유 ID가 없다면 생성 (랜덤 문자열 등)
        if (comment.getId() == null) {
            comment.setId(java.util.UUID.randomUUID().toString());
        }
        commentRepository.save(comment);
    }

    // 2. 특정 게시글에 달린 댓글들만 싹 가져오는 기능
    public List<CommentItem> getCommentsByBoardId(String boardId) {
        // 1. 'board_comments' 인덱스에서 boardId가 일치하는 데이터를 검색합니다.
        // 2. 검색 결과를 List<CommentItem>으로 변환해서 리턴합니다.


        return commentRepository.findByBoardId(boardId); // 실제 Repository가 있다면 이렇게!
    }
    public void deleteComment(String commentId) {
        try {
            // Spring Data Elasticsearch Repository를 사용하여 ID로 바로 삭제합니다.
            commentRepository.deleteById(commentId);
            System.out.println("🗑️ 댓글 삭제 완료: " + commentId);
        } catch (Exception e) {
            System.err.println("댓글 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 4. 댓글 수정 기능
    public void updateComment(String commentId, String newContent) {
        try {
            // 1. 먼저 해당 ID의 댓글이 있는지 찾아봅니다.
            commentRepository.findById(commentId).ifPresent(comment -> {
                // 2. 내용만 새 내용으로 바꿉니다.
                comment.setContent(newContent);
                // 3. 수정 시간을 현재로 업데이트합니다. (선택 사항)
                comment.setCreatedAt(LocalDateTime.now());

                // 4. 다시 저장(save)하면 엘라스틱서치가 알아서 업데이트(Upsert) 해줍니다.
                commentRepository.save(comment);
                System.out.println("✏️ 댓글 수정 완료: " + commentId);
            });
        } catch (Exception e) {
            System.err.println("댓글 수정 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}