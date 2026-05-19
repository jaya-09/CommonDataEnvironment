package com.cde.analytics.dto;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrendPoint {
    private LocalDate date;
    private int passCount;
    private int blockCount;
    private int ncrsOpened;
    private int ncrsClosed;
    private int certsIssued;
}
