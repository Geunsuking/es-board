package esboard.repository;

import esboard.model.BoardItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardItemRepository extends ElasticsearchRepository<BoardItem, String> {
    // 게시글 저장을 위한 전용 리포지토리입니다.
}