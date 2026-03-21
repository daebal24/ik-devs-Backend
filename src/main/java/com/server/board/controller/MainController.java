package com.server.board.controller;


import com.server.board.domain.dto.*;
import com.server.board.service.MainService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper; // json

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api") // 프리픽스: /api/...
public class MainController {

    private final MainService service;
    private final LoginController loginController;
    private final PageController pageController;
    private final ChatbotController chatbotController;

    //Controller 통합주입
    public MainController(MainService service, LoginController loginController, PageController pageController, ChatbotController chatbotController) {
        this.service = service;
        this.loginController = loginController;
        this.pageController = pageController;
        this.chatbotController = chatbotController;
    }



    //이미지 업로드
    @PostMapping(
                value = "/uploadMultimedia",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadMultimedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "meta", required = false) String metaJson,
            HttpSession session
    )
    {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            System.out.println("usertype = " + usertype);
            throw new IllegalArgumentException("not allowed user");
        }
        Map<String, Object> response = new HashMap<>();

        try {
            // 1) 현재 작업 디렉토리 (프로젝트 루트일 가능성이 큼)
            Path rootPath = Paths.get("").toAbsolutePath();
            System.out.println("rootPath = " + rootPath);

            // 2) ./multimedia 절대 경로 만들기
            Path multimediaDir = rootPath.resolve("multimedia");
            Files.createDirectories(multimediaDir); // 없으면 생성

            System.out.println("multimediaDir = " + multimediaDir);

            // 3) 파일명/확장자
            String originalName = file.getOriginalFilename();
            String ext = Objects.requireNonNull(originalName)
                    .substring(originalName.lastIndexOf("."))
                    .toLowerCase();

            if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".pdf")) {
                response.put("ok", false);
                response.put("message", "jpg/png/pdf만 업로드 가능합니다.");
                return response;
            }

            String saveName = UUID.randomUUID() + ext;
            Path savePath = multimediaDir.resolve(saveName);
            System.out.println("savePath = " + savePath);

            // 4) 임시파일 → 우리가 지정한 절대 경로로 이동
            file.transferTo(savePath.toFile());

            // 5) DB처리
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> meta = mapper.readValue(metaJson, Map.class);
            String username = meta.get("username");
            String description = meta.get("description");
            System.out.println("metaJson username = " + username);
            System.out.println("metaJson description = " + description);

            int n = service.addMultimedia(username,description,saveName,ext);

            // 6) response 처리
            response.put("ok", true);
            response.put("filename", saveName);
            response.put("path", savePath.toString());
            response.put("meta", metaJson);
            return response;

        }
        catch (Exception e) {
            e.printStackTrace();
            response.put("ok", false);
            response.put("message", e.toString());
            return response;
        }
    }

    @PostMapping("/viewMediaData")
    public List<ViewMediaData> viewMediaData() {
        return service.viewMediaData();
    }

    //테스트용 api
//    @GetMapping("/testDB")
//    public List<NameRow> list() {
//        return service.getAll();
//    }
//
//    @PostMapping("/insertName")
//    public String insert(@RequestBody CreateNameRequest req) {
//        int n = service.add(req.name());
//        return n + "행 삽입 완료: " + req.name();
//    }
//
//    @PatchMapping("/updateName")
//    public String update(@RequestBody UpdateNameRequest req) {
//        int n = service.rename(req.oldName(), req.newName());
//        return n + "개의 행이 " + req.oldName() + " → " + req.newName() + " 로 업데이트되었습니다.";
//    }
}


