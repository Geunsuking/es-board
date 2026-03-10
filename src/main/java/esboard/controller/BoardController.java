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
@RequestMapping("/api/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @PostMapping("/save")
    public String savePost(@ModelAttribute BoardItem item) {
        try {
            item.setCreatedAt(LocalDateTime.now());

            boardService.save(item);

            return "redirect:/api/board/list";
        } catch (Exception e) {
            e.printStackTrace();
            return "error"; // 에러 시 에러 페이지로 (나중에 만듦)
        }
    }

    @GetMapping("/all")
    @ResponseBody
    public Map<String, Object> getAllPosts() { // 👈 반환 타입을 Map으로 변경
        return boardService.search(null, null, 0);
    }
    // BoardController.java

    @GetMapping({"/", "/list"})
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
        // 1. 기존 데이터를 먼저 불러옵니다 (기존 조회수를 지키기 위해)
        BoardItem originItem = boardService.findById(id);

        if (originItem != null) {
            // 2. 바꿀 내용만 교체합니다.
            originItem.setTitle(updateData.getTitle());
            originItem.setContent(updateData.getContent());
            // 작성자는 보통 안 바꾸지만 원하시면 추가: originItem.setAuthor(updateData.getAuthor());

            // 3. originItem에는 이미 기존 views가 들어있으므로 그대로 저장!
            boardService.save(originItem);
        }
        return "redirect:/api/board/list/";
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
    public String addComment(@ModelAttribute CommentItem comment,
                             @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {

        // 1. 파일이 넘어왔는지 확인하고 서버에 저장합니다.
        if (file != null && !file.isEmpty()) {
            String uploadDir = "C:/uploads/"; // 실제 파일이 저장될 물리적 경로

            // 폴더가 없으면 생성
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            // 파일명 중복을 피하기 위해 UUID를 붙입니다.
            String fileName = java.util.UUID.randomUUID() + "_" + file.getOriginalFilename();
            java.io.File saveFile = new java.io.File(uploadDir + fileName);

            // 파일을 지정된 경로에 실제로 저장
            file.transferTo(saveFile);

            // 2. DB(Elasticsearch)에 저장할 수 있게 CommentItem에 파일 경로를 넣어줍니다.
            // (미리 CommentItem 클래스에 imageUrl 필드를 만들어둬야 합니다!)
            comment.setImageUrl("/uploads/" + fileName);
        }

        // 3. 서비스에게 댓글(이미지 경로 포함) 저장을 시킵니다.
        boardService.saveComment(comment);

        // 4. 원래 보던 상세 페이지로 이동
        return "redirect:/api/board/view/" + comment.getBoardId();
    }
}