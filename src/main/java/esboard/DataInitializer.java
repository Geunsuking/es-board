package esboard;

import esboard.model.BoardItem;
import esboard.model.CommentItem;
import esboard.repository.BoardItemRepository;
import esboard.repository.CommentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BoardItemRepository boardItemRepository;
    private final CommentRepository commentRepository;

    public DataInitializer(BoardItemRepository boardItemRepository, CommentRepository commentRepository) {
        this.boardItemRepository = boardItemRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public void run(String... args) {
        // [안전 장치] 데이터가 아예 없을 때만 실행됩니다.
        if (boardItemRepository.count() == 0) {
            System.out.println("ℹ️ [System] 게시글이 없어 샘플 데이터를 생성합니다...");

            List<String> authorPool = new ArrayList<>(Arrays.asList(
                    "호랑이", "돼지", "엘라스틱마스터", "데이터분석가", "토끼", "코딩전문가", "보안담당자", "시스템운영자"
            ));
            Collections.shuffle(authorPool);

            int totalCommentCount = 1;

            for (int i = 1; i <= 7; i++) {
                String postId = "post_" + i;

                // [게시글 생성]
                BoardItem item = new BoardItem();
                item.setId(postId);
                item.setTitle("게시글 " + i + "번");

                // 🚀 내용(Content) 추가: 상세 페이지에서 보일 텍스트입니다.
                item.setContent(i + "번 게시글의 본문 내용입니다. 테스트를 위해 생성된 데이터입니다.");

                item.setAuthor(authorPool.get(i - 1));
                item.setViews(i * 15);
                item.setCreatedAt(LocalDateTime.now().minusHours(7 - i));
                boardItemRepository.save(item);

                // [댓글 생성]
                for (int j = 1; j <= 2; j++) {
                    CommentItem comment = new CommentItem();
                    comment.setId("comment_" + totalCommentCount);
                    comment.setBoardId(postId);
                    comment.setContent(totalCommentCount + "번째 테스트 댓글");
                    comment.setAuthor("댓글" + totalCommentCount);
                    comment.setCreatedAt(LocalDateTime.now());

                    commentRepository.save(comment);
                    totalCommentCount++;
                }
            }
        }
    }
}