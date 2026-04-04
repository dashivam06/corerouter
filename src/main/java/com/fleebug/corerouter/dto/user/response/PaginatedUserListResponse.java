package com.fleebug.corerouter.dto.user.response;

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
    title = "Paginated User List Response",
    description = "Paginated list of users with filtering options"
)
public class PaginatedUserListResponse {

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total number of users matching the filter", example = "150")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;

    @Schema(description = "List of users on this page")
    private List<UserProfileResponse> users;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean isLastPage;

    public static PaginatedUserListResponse fromPage(Page<UserProfileResponse> page) {
        return PaginatedUserListResponse.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .users(page.getContent())
                .isLastPage(page.isLast())
                .build();
    }
}
