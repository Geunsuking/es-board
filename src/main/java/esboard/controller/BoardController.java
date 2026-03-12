package esboard.controller;

import esboard.model.BoardItem;
import esboard.model.CommentItem;
import esboard.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping({"/", "/api/board"})
public class BoardController {

    @Autowired
    private BoardService boardService;

    @GetMapping("/")
    public String home() {
        // 사용자가 / 로 들어오면 /api/board 로 강제 이동시켜!
        return "redirect:/api/board/list";
    }
    
    @PostMapping("/save")
    public String savePost(@ModelAttribute BoardItem item) {
        try {
            // [중요] 나노초를 지우는 대신, 밀리초 1을 강제로 넣어 형식을 맞춥니다.
            // .withNano(1000000) 은 딱 1밀리초를 의미합니다.
            // 이렇게 하면 2026-03-11T15:08:33.001 처럼 생성되어 형식이 완벽히 맞습니다.
            item.setCreatedAt(LocalDateTime.now().withNano(1000000));

            boardService.save(item);

            return "redirect:/api/board/list";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @GetMapping("/all")
    @ResponseBody
    public Map<String, Object> getAllPosts() { // 👈 반환 타입을 Map으로 변경
        return boardService.search(null, null, 0);
    }
    // BoardController.java

    @GetMapping("/list")
    public String listPage(
            Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            // [체크] 화면에서 넘어오는 searchType과 keyword를 받습니다.
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        // 🚀 [중요] 서비스 호출 시 인자를 '3개' 순서대로 정확히 넣습니다.
        // (keyword, searchType, page) -> 이 순서가 서비스와 똑같아야 합니다!
        Map<String, Object> result = boardService.search(keyword, searchType, page);

        // 결과 데이터 꺼내기
        long totalCount = (long) result.get("total");
        int totalPages = (int) Math.ceil((double) totalCount / 10);

        // 뷰(HTML)로 전달할 데이터 담기
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("boards", result.get("list"));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPage", page);

        // 🚀 [중요] 검색 후에도 검색창에 값이 남아있도록 다시 보냅니다.
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);

        return "boardList";
    }
    @GetMapping("/view/{id}")
    public String getBoardView(@PathVariable String id, Model model) {

        // 1. [핵심] 들어오자마자 무조건 조회수 1 증가!
        boardService.incrementViews(id);
        // 2. 글 데이터를 가져와서 화면으로 전달
        BoardItem boardItem = boardService.findById(id);
        model.addAttribute("board", boardItem);

        List<CommentItem> commentList = boardService.getCommentsByBoardId(id);
        model.addAttribute("comments", commentList);

        return "view";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable String id, Model model) {
        // 1. 수정할 글 데이터를 DB에서 가져옵니다.
        BoardItem boardItem = boardService.findById(id);

        // 2. 화면(boardWrite)에 기존 글 내용을 채워주기 위해 데이터를 보냅니다.
        model.addAttribute("post", boardItem);

        // 3. 글쓰기 페이지(boardWrite.html)를 열어줍니다.
        return "boardWrite";
    }
    @PostMapping("/edit/{id}")
    public String updateBoard(@PathVariable String id, @ModelAttribute BoardItem updateData) throws Exception {
        // 1. 기존 데이터를 먼저 불러옵니다.
        BoardItem originItem = boardService.findById(id);

        if (originItem != null) {
            // 2. 바꿀 내용들을 교체합니다.
            originItem.setTitle(updateData.getTitle());
            originItem.setContent(updateData.getContent());

            // [여기가 핵심!] 작성자 필드도 업데이트되도록 이 줄을 추가하세요.
            originItem.setAuthor(updateData.getAuthor());

            // 3. 기존 조회수(views)와 시간은 그대로 유지된 채 저장됩니다.
            boardService.save(originItem);
        }
        return "redirect:/api/board/list";
    }

    // 2. 수정 처리
    @PostMapping("/update")
    public String updatePost(@ModelAttribute BoardItem item) {
        try {
            // 1. 기존에 저장되어 있던 원본 글을 다시 불러옵니다 (원래 시간을 알아내기 위해)
            BoardItem originalPost = boardService.findById(item.getId());

            if (originalPost != null) {
                // 2. 원래 글이 가지고 있던 '처음 작성 시간'을 그대로 다시 넣어줍니다.
                item.setCreatedAt(originalPost.getCreatedAt());
            }

            // 3. 시간은 그대로인 상태로 내용만 덮어씁니다.
            boardService.save(item);

            return "redirect:/api/board/list";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

    }
    @GetMapping("/write")
    public String writePage() {
        // templates 폴더 안에 있는 boardWrite.html 파일을 열어줍니다.
        return "boardWrite";
    }
    @DeleteMapping("/delete/{id}")
    @ResponseBody // HTML 페이지가 아닌 성공/실패 신호만 보내기 위해 필요!
    public ResponseEntity<String> deletePost(@PathVariable String id) {
        try {
            boardService.delete(id);
            return ResponseEntity.ok("삭제 성공");
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 500 에러와 메시지 전송
            return ResponseEntity.status(500).body("삭제 실패: " + e.getMessage());
        }
    }
    @PostMapping("/comment/add")
    public String addComment(@ModelAttribute CommentItem comment) {
        // 파일 관련 파라미터(MultipartFile)와 저장 로직을 모두 지웠습니다.

        // 1. 서비스에게 댓글 저장만 시킵니다.
        boardService.saveComment(comment);

        // 2. 저장이 끝나면 다시 보던 상세 페이지로 돌아갑니다.
        return "redirect:/api/board/view/" + comment.getBoardId();
    }
    // 1. 댓글 삭제 요청 처리 (주소 앞에 /api/board 추가 완료)
    @PostMapping("/comment/delete/{commentId}")
    @ResponseBody
    public String deleteComment(@PathVariable String commentId) {
        boardService.deleteComment(commentId);
        return "success";
    }

    // 2. 댓글 수정 요청 처리 (주소 앞에 /api/board 추가!)
    @PostMapping("/comment/update/{commentId}")
    @ResponseBody
    public String updateComment(@PathVariable String commentId, @RequestParam String content) {
        boardService.updateComment(commentId, content);
        return "success";
    }
}