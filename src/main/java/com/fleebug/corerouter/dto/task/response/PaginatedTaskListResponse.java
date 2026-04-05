package com.fleebug.corerouter.dto.task.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Paginated Task List Response",
    description = "Paginated list of tasks with optional status filtering"
)
public class PaginatedTaskListResponse {

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "5")
    private int size;

    @Schema(description = "Total number of tasks matching the filter", example = "120")
    private long totalElements;

    @Schema(description = "Total pages", example = "24")
    private int totalPages;

    @Schema(description = "Tasks for this page")
    private List<TaskListItemResponse> tasks;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean lastPage;

    public static PaginatedTaskListResponse fromPage(Page<TaskListItemResponse> page) {
        return PaginatedTaskListResponse.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .tasks(page.getContent())
                .lastPage(page.isLast())
                .build();
    }
}