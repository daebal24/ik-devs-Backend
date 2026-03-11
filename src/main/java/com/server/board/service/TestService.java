package com.server.board.service;

import com.server.board.domain.dto.*;
import com.server.board.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TestService {

    private final TestRepository repo;

    public TestService(TestRepository repo) {
        this.repo = repo;
        // 애플리케이션 시작 후 필요 시 테이블 보장
        this.repo.initTableIfNeeded();
    }

    public List<NameRow> getAll() {
        return repo.findAll();
    }

    public int add(String name) {
        return repo.insert(name);
    }

    public int rename(String oldName, String newName) {
        return repo.updateName(oldName, newName);
    }

    public int insertPageData(String pagename, String content, String memo)
    {
        return repo.insertPageData(pagename, content, memo);
    }

    public List<ViewPageData> viewPageData(String pageName) {
        return repo.viewPageData(pageName);
    }
    public List<ViewPageData> viewPageDataAll() {
        return repo.viewPageDataAll();
    }

    public int updatePageData(String pagename, String content, String memo) {
        return repo.updatePageData(pagename, content, memo);
    }
    public List<Login> login(String id, String pw){
        return repo.login(id, pw);
    }

    // 로그인 실패 카운트 조회 (5회 이상이면 "locked" 반환)
    public String viewLoginFailcount(String id, int maxloginfailcount) {
        int count = repo.getLoginFailcount(id);
        return count >= maxloginfailcount ? "locked" : "ok";
    }

    public int getLoginFailcount(String id)
    {
        return repo.getLoginFailcount(id);
    }

    // mode 0: 초기화, mode 1: +1
    public int updateLoginFailcount(String id, int mode) {
        if (mode == 0) return repo.resetLoginFailcount(id);
        else           return repo.incrementLoginFailcount(id);
    }

    public int addMultimedia(String name, String description, String filename, String filetype){
        return repo.insertMultimedia(name, description, filename, filetype);
    }
    public List<ViewMediaData> viewMediaData(){
        return repo.viewMediaData();
    }

    public List<ViewPageContenttasksummary> ViewPageContenttasksummary() {
        return repo.ViewPageContenttasksummary();
    }

    public String UpdatePageContenttasksummary(List<UpdatePageContentTaskSummaryRequest.YearGroupDto> yearGroups, List<UpdatePageContentTaskSummaryRequest.ProjectDto> projects)
    {
        return repo.UpdatePageContenttasksummary(yearGroups, projects);
    }

    public int setOtpSecret(String id, String secret) {
        return repo.setOtpSecret(id, secret);
    }

    public List<ViewOTPStatus> getGoogleOTPStatus(String id) {
        return repo.getGoogleOTPStatus(id);
    }


//    public List<ViewPageData> viewPageData() {
//        return repo.viewPageData();
//    }
}
