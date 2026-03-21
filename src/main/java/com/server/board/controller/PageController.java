package com.server.board.controller;

import com.server.board.domain.dto.UpdatePageContentTaskSummaryRequest;
import com.server.board.domain.dto.ViewPageContenttasksummary;
import com.server.board.domain.dto.ViewPageData;
import com.server.board.service.MainService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PageController {
    private final MainService service;

    public PageController(MainService service) {
        this.service = service;
    }

    @PostMapping("/viewPageData")
    public List<ViewPageData> viewPageData(@RequestBody Map<String, String> req) {
        List<ViewPageData> result = service.viewPageData(req.get("pagename"));

        //존재하지 않는 데이터이면 빈값형식을 만들어서 리턴
        if(result.isEmpty())
        {
            List<ViewPageData> empty = List.of(
                    new ViewPageData(0L, "", "", "")
            );
            return empty;
        }
        else
            return result;
    }
    @PostMapping("/viewPageDataAll")
    public List<ViewPageData> viewPageDataAll() {
        return service.viewPageDataAll();
    }
    @PostMapping("/insertPageData")
    public String insertPageData(@RequestBody Map<String, String> req, HttpSession session) {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            throw new IllegalArgumentException("not allowed user");
        }
        List<ViewPageData> result = service.viewPageData(req.get("pagename"));

        //존재하지 않는 데이터이면 빈값형식을 만들어서 리턴
        if(result.isEmpty())
        {
            int n = service.insertPageData(req.get("pagename"), req.get("content"), req.get("memo"));
            return "Success";
        }
        else
        {
            return "AlreadyExist";
        }


    }

    @PostMapping("/updatePageData")
    public String updatePageData(@RequestBody Map<String, String> req, HttpSession session) {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            throw new IllegalArgumentException("not allowed user");
        }
        int n = service.updatePageData(req.get("pagename"), req.get("content"), req.get("memo"));
        return n + "개의 행이 업데이트되었습니다.";
    }
    @PostMapping("/ViewPageContenttasksummary")
    public List<ViewPageContenttasksummary> ViewPageContenttasksummary() {
        return service.ViewPageContenttasksummary();
    }

    @PostMapping("/UpdatePageContenttasksummary")
    public String UpdatePageContenttasksummary(
            @RequestBody UpdatePageContentTaskSummaryRequest req,
            HttpSession session
    ) {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            throw new IllegalArgumentException("not allowed user");
        }

        if (req == null) {
            throw new IllegalArgumentException("request body is null");
        }

        var yearGroups = req.getYearGroups();
        var projects = req.getProjects();

        if (yearGroups == null) yearGroups = List.of();
        if (projects == null) projects = List.of();

        return service.UpdatePageContenttasksummary(yearGroups, projects);
    }
}
