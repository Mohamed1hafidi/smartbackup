package com.smartbackup.dto.response;

import java.util.List;

public record ScheduleListResponse(
        List<ScheduleResponse> data,
        long totalElements
) {
}
