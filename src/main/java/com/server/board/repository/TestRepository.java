package com.server.board.repository;

import com.server.board.domain.dto.*;

import java.util.List;

public interface TestRepository {
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
}
