package esboard.controller;

import esboard.model.BoardItem;
import esboard.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @PostMapping("/save")
    public String savePost(@RequestBody BoardItem item){
        try {
            item.setCreatedAt(LocalDateTime.now());

            boardService.save(item);

            return "저장 성공! ID: " + item.getId();
        } catch (Exception e) {
            return "저장 실패: " + e.getMessage();
        }
    }
    @GetMapping("/all") // 1. 주소창에 /all을 붙여서 GET 요청을 보내면 실행됩니다.
    public List<BoardItem> getAllPosts() {
        // 2. 아까 공들여 만든 요리사(Service)의 findAll 기능을 호출합니다.
        return boardService.findAll();
    }
}
