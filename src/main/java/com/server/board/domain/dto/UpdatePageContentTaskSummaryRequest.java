package com.server.board.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePageContentTaskSummaryRequest {

    private List<YearGroupDto> yearGroups;
    private List<ProjectDto> projects;

    public List<YearGroupDto> getYearGroups() { return yearGroups; }
    public void setYearGroups(List<YearGroupDto> yearGroups) { this.yearGroups = yearGroups; }

    public List<ProjectDto> getProjects() { return projects; }
    public void setProjects(List<ProjectDto> projects) { this.projects = projects; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YearGroupDto {
        private String id;
        private String title;
        private String content;
        private Boolean open;
        private String status;
        private Boolean isEditing;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Boolean getOpen() { return open; }
        public void setOpen(Boolean open) { this.open = open; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Boolean getIsEditing() { return isEditing; }
        public void setIsEditing(Boolean isEditing) { this.isEditing = isEditing; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectDto {
        private String id;
        private String title;
        private String content;
        private String status;
        private Boolean isEditing;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Boolean getIsEditing() { return isEditing; }
        public void setIsEditing(Boolean isEditing) { this.isEditing = isEditing; }
    }
}