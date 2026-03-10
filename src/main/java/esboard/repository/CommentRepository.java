package esboard.repository;

import esboard.model.CommentItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

// ElasticsearchRepository를 상속받으면 기본적인 저장/조회 기능은 자동으로 생깁니다.
public interface CommentRepository extends ElasticsearchRepository<CommentItem, String> {

    // 특정 게시글(boardId)의 댓글만 쏙쏙 뽑아오는 기능을 추가합니다.
    List<CommentItem> findByBoardId(String boardId);
}