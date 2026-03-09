package esboard.controller;

import esboard.model.BoardItem;
import esboard.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        return boardService.search(null, 0);
    }
    // BoardController.java

    @GetMapping("/list")
    public String listPage(Model model, @RequestParam(value = "page", defaultValue = "0") int page) {
        // 1. 서비스가 주는 '상자(Map)'를 통째로 받습니다.
        Map<String, Object> result = boardService.search(null, page);

        long totalCount = (long) result.get("total");
        int totalPages = (int) Math.ceil((double) totalCount / 10); // 전체 페이지 수 계산 (예: 13/10 = 1.3 -> 올림해서 2)

        model.addAttribute("totalPages", totalPages);
        // 2. 상자 안에서 "list"라는 이름의 알맹이(게시글들)를 꺼내서 담습니다.
        model.addAttribute("boards", result.get("list"));

        // 3. 상자 안에서 "total"이라는 이름의 알맹이(전체 개수)를 꺼내서 담습니다.
        model.addAttribute("totalCount", result.get("total"));

        // 4. 현재 페이지 번호를 담습니다.
        model.addAttribute("currentPage", page);

        return "boardList";
    }

    @GetMapping("/write")
    public String writePage() {
        return "boardWrite"; // templates/boardWrite.html 파일을 찾으라는 뜻입니다.
    }

    @GetMapping("/delete/{id}")
    public String deletePost(@PathVariable String id) {
        boardService.delete(id); // 서비스에서 삭제 기능을 호출합니다.
        return "redirect:/api/board/list"; // 삭제 후 다시 목록으로!
    }

    @GetMapping("/search")
    public String search(@RequestParam("q") String query,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         Model model) {
        // 1. 서비스에 검색을 시킵니다.
        Map<String, Object> result = boardService.search(query, page);

        // 2. 검색 결과에서 전체 개수(long)를 가져옵니다.
        long totalCount = (long) result.get("total");

        // 🚀 [중요] 여기서 totalPages 변수를 직접 만들어줘야 합니다!
        // 전체 글 개수를 10으로 나누고 올림 처리합니다.
        int totalPages = (int) Math.ceil((double) totalCount / 10);

        // 3. 이제 모델에 담습니다. (이제 빨간 줄이 사라질 거예요!)
        model.addAttribute("boards", result.get("list"));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages); // 👈 여기서 사용 가능!
        model.addAttribute("q", query);

        return "boardList";
    }

    @GetMapping("/view/{id}")
    public String viewPost(@PathVariable("id") String id, Model model) {
        // 1. 서비스에게 "이 ID를 가진 글 하나만 가져와줘"라고 시킵니다.
        BoardItem post = boardService.findById(id);

        // 2. 가져온 글을 모델에 담습니다.
        model.addAttribute("post", post);

        // 3. 상세보기 화면(view.html)으로 보냅니다.
        return "view";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable String id, Model model) {
        BoardItem post = boardService.findById(id); // 기존 글을 가져와서
        model.addAttribute("post", post);           // 화면에 전달
        return "boardWrite"; // 기존 글쓰기 화면을 재활용하거나 새로 만듭니다.
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
}