package esboard.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@Document(indexName = "board_comments")
public class CommentItem {
    @Id
    private String id;          // 댓글 고유 ID

    @Field
    private String boardId;     // ⭐ 중요: 연결된 게시글 ID
    private String author;      // 댓글 작성자
    private String content;     // 댓글 내용

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
}
