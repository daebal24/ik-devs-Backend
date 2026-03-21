package com.server.board.repository;

import com.server.board.domain.dto.*;

import java.util.List;

public interface MainRepository {
    void initTableIfNeeded();
    List<NameRow> findAll();
    int insert(String name);
    int updateName(String oldName, String newName);
    int insertPageData(String pagename, String content, String memo);
    List<ViewPageData> viewPageDataAll();
    List<ViewPageData> viewPageData(String pageName);
    int updatePageData(String pageName, String content, String memo);
    //List<ViewPageData> viewPageData();
    List<Login> login(String id, String pw);
    int getLoginFailcount(String id);
    int resetLoginFailcount(String id);
    int incrementLoginFailcount(String id);
    int insertMultimedia(String name, String description, String filename, String filetype);
    List<ViewMediaData> viewMediaData();

    List<ViewPageContenttasksummary> ViewPageContenttasksummary();

    String UpdatePageContenttasksummary (List<UpdatePageContentTaskSummaryRequest.YearGroupDto> yearGroups, List<UpdatePageContentTaskSummaryRequest.ProjectDto> projects);

    int setOtpSecret(String id, String secret);
    List<ViewOTPStatus> getGoogleOTPStatus(String id);

    //구글 OTP 임시시연용 함수. 특정 테스트아이디의 구글 OTP기능 초기화
    int test_gooleotpreset(String id);

    String isidexist(String id);
}
