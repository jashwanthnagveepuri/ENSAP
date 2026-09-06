package com.ensap.siteprofile.dto;

import java.util.List;

/**
 * Standard list envelope (docs/10-api-design.md "Pagination & filtering").
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {
}
