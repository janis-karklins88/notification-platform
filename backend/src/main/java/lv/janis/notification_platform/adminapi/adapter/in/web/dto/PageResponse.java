package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
}
