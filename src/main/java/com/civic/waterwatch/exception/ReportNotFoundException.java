package com.civic.waterwatch.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an Indian water report or tracking code (e.g. IND-H2O-1686) is not found.
 */
public class ReportNotFoundException extends WaterWatchException {

    public ReportNotFoundException(String reportCode) {
        super(
                "Water incident grievance not found for code: " + reportCode,
                HttpStatus.NOT_FOUND,
                "REPORT_NOT_FOUND",
                Map.of("reportCode", reportCode)
        );
    }

    public ReportNotFoundException(Long reportId) {
        super(
                "Water incident grievance not found for ID: " + reportId,
                HttpStatus.NOT_FOUND,
                "REPORT_NOT_FOUND",
                Map.of("reportId", reportId)
        );
    }
}
