package com.server.board.repository;

import com.server.board.domain.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class TestRepositoryJdbc implements TestRepository {

    private final JdbcTemplate jdbc;

    public TestRepositoryJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void initTableIfNeeded() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS test1(
              id   INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT
            );
        """);
    }

    @Override
    public List<NameRow> findAll() {
        return jdbc.query(
                "SELECT id, name FROM test2 ORDER BY id",
                (rs, i) -> new NameRow(rs.getLong("id"), rs.getString("name"))
        );
    }

    @Override
    public int insert(String name) {
        return jdbc.update("INSERT INTO PageContent_base(name) VALUES(?)", name);
    }

    @Override
    public int updateName(String oldName, String newName) {
        return jdbc.update("UPDATE PageContent_base SET name=? WHERE name=?", newName, oldName);
    }

    @Override
    public int insertPageData(String pagename, String content, String memo) {
        return jdbc.update("INSERT INTO PageContent_base(pagename, content, memo) VALUES(?,?,?)", pagename, content, memo);
    }

    @Override
    public List<ViewPageData> viewPageDataAll() {
        return jdbc.query(
                "SELECT id, pagename, content, memo FROM PageContent_base",
                (rs, i) -> new ViewPageData(
                        rs.getLong("id"),
                        rs.getString("pagename"),
                        rs.getString("content"),
                        rs.getString("memo")
                )
        );
    }

    @Override
    public List<ViewPageData> viewPageData(String pagename_input) {
        return jdbc.query(
                "SELECT id, pagename, content, memo FROM PageContent_base WHERE pagename = ?",
                (rs, i) -> new ViewPageData(
                        rs.getLong("id"),
                        rs.getString("pagename"),
                        rs.getString("content"),
                        rs.getString("memo")
                ),
                pagename_input
        );
    }

    @Override
    public int updatePageData(String pagename, String content, String memo)
    {
        return jdbc.update("UPDATE PageContent_base SET content=?, memo=? WHERE pagename=?", content, memo, pagename);
    }

    @Override
    public List<Login> login(String id, String pw)
    {
        String query = "SELECT * FROM users where id=? and pw=?";
        System.out.println("id = [" + id + "]");
        System.out.println("pw = [" + pw + "]");

        return jdbc.query(
                query,
                (rs, i) -> new Login(
                        rs.getString("id"),
                        rs.getString("usertype")
                ),
                id, pw
        );
    }
    @Override
    public int getLoginFailcount(String id) {
        List<Integer> result = jdbc.query(
                "SELECT loginfailcount FROM users WHERE id = ?",
                (rs, i) -> rs.getInt("loginfailcount"),
                id
        );
        return result.isEmpty() ? 0 : result.getFirst();
    }

    @Override
    public int resetLoginFailcount(String id) {
        return jdbc.update("UPDATE users SET loginfailcount = 0 WHERE id = ?", id);
    }

    @Override
    public int incrementLoginFailcount(String id) {
        return jdbc.update("UPDATE users SET loginfailcount = loginfailcount + 1 WHERE id = ?", id);
    }

    @Override
    public int insertMultimedia(String name, String description, String filename, String filetype)
    {
        return jdbc.update("INSERT INTO media_metadata (name, memo, filename, filetype) VALUES(?,?,?,?)", name, description, filename, filetype);
    }

    @Override
    public List<ViewMediaData> viewMediaData() {
        return jdbc.query(
                "SELECT name, memo, filename, filetype FROM media_metadata WHERE is_deleted = 0",
                (rs, i) -> new ViewMediaData(
                        rs.getString("name"),
                        rs.getString("memo"),
                        rs.getString("filename"),
                        rs.getString("filetype")
                )
        );
    }

    public List<ViewPageContenttasksummary> ViewPageContenttasksummary()
    {
        return jdbc.query(
                "SELECT pk as id, contenttype, title, content FROM PageContent_tasksummary",
                (rs, i) -> new ViewPageContenttasksummary(
                        rs.getLong("id"),
                        rs.getString("contenttype"),
                        rs.getString("title"),
                        rs.getString("content")
                )
        );
    }

//    public String UpdatePageContenttasksummary (
//            List<UpdatePageContentTaskSummaryRequest.YearGroupDto> yearGroups,
//            List<UpdatePageContentTaskSummaryRequest.ProjectDto> projects)
//    {
//    //
//    //        type YearGroup = {
//    //        id: string;
//    //        title: string; // 사용자가 직접 입력하는 제목(예: "2022", "2023")
//    //        content: string;   // 항목은 무조건 1개(단일 텍스트)
//    //        open?: boolean;
//    //        status: string; // "insert" | "update" | "delete" | ""
//    //        isEditing?: boolean;};
//    //
//    //        type Project = {
//    //        id: string;
//    //        title: string;
//    //        content: string;
//    //        status: string; // "insert" | "update" | "delete" | ""
//    //        isEditing?: boolean;};
//    //
//    //
//    //        const initialYearGroups: YearGroup[] = [];
//    //        const initialProjects: Project[] = [];
//
//        return "";
//    }

    public String UpdatePageContenttasksummary(
            List<UpdatePageContentTaskSummaryRequest.YearGroupDto> yearGroups, List<UpdatePageContentTaskSummaryRequest.ProjectDto> projects
    ) {

        // YearGroup 처리
        for (UpdatePageContentTaskSummaryRequest.YearGroupDto yg : yearGroups) {
            String status = yg.getStatus();

            if (status == null) continue;

            switch (status) {
                case "insert" -> {
                    jdbc.update("""
                    INSERT INTO PageContent_tasksummary
                    (contenttype, title, content, createdat)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """,
                            "year",
                            yg.getTitle(),
                            yg.getContent()
                    );
                }

                case "update" -> {
                    jdbc.update("""
                    UPDATE PageContent_tasksummary
                    SET title = ?, content = ?, updatedat = CURRENT_TIMESTAMP
                    WHERE pk = ?
                """,
                            yg.getTitle(),
                            yg.getContent(),
                            Integer.parseInt(yg.getId())
                    );
                }

                case "delete" -> {
                    jdbc.update("""
                    DELETE FROM PageContent_tasksummary
                    WHERE pk = ?
                """,
                            Integer.parseInt(yg.getId())
                    );
                }
            }
        }

        // Project 처리
        for (UpdatePageContentTaskSummaryRequest.ProjectDto pr : projects) {
            String status = pr.getStatus();
            if (status == null) continue;

            switch (status) {
                case "insert" -> {
                    jdbc.update("""
                    INSERT INTO PageContent_tasksummary
                    (contenttype, title, content, createdat)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """,
                            "project",
                            pr.getTitle(),
                            pr.getContent()
                    );
                }

                case "update" -> {
                    jdbc.update("""
                    UPDATE PageContent_tasksummary
                    SET title = ?, content = ?, updatedat = CURRENT_TIMESTAMP
                    WHERE pk = ?
                """,
                            pr.getTitle(),
                            pr.getContent(),
                            Integer.parseInt(pr.getId())
                    );
                }

                case "delete" -> {
                    jdbc.update("""
                    DELETE FROM PageContent_tasksummary
                    WHERE pk = ?
                """,
                            Integer.parseInt(pr.getId())
                    );
                }
            }
        }

        return "ok";
    }

    @Override
    public int setOtpSecret(String id, String secret) {
        return jdbc.update("UPDATE users SET otp_secret = ? WHERE id = ?", secret, id);
    }

    @Override
    public List<ViewOTPStatus> getGoogleOTPStatus(String id)
    {
        String query = "SELECT otp_enabled, otp_secret FROM users where id=?";
        System.out.println("getGoogleOTPStatus processing. id : "+id);

        return jdbc.query(
                query,
                (rs, i) -> new ViewOTPStatus(
                        rs.getString("otp_enabled"),
                        rs.getString("otp_secret") != null ? rs.getString("otp_secret") : ""
                ),
                id
        );
    }

    @Override
    public int test_gooleotpreset(String id)
    {
        System.out.println("Test : OTP Reset query activated");
        return jdbc.update("UPDATE users SET otp_secret = null WHERE id = ?", id);
    }



    //    @Override
//    public List<ViewPageData> viewPageData() {
//        return jdbc.query(
//                "SELECT id, pagename, content FROM test1",
//                (rs, i) -> new ViewPageData(
//                        rs.getLong("id"),
//                        rs.getString("pagename"),
//                        rs.getString("content")
//                )
//        );
//    }
}
