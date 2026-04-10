package com.taskforge.project.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProjectInsightsResponse {
    private int highPriorityCount;
    private int overdueCount;
    private List<TimeInStatusMetric> timeInStatus;

    @Data
    public static class TimeInStatusMetric {
        private String status;
        private double avgDays;
    }
}
