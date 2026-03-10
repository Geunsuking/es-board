package esboard.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoardItem {
    private String id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private int views;
    private String imageUrl;
}
