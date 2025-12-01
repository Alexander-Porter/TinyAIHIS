package com.tinyhis.dto;

import lombok.Data;
import java.util.List;

/**
 * Queue Info DTO for WebSocket
 */
@Data
public class QueueInfo {
    private Long deptId;
    private String deptName;
    private CurrentPatient current;
    private List<WaitingPatient> waiting;
    
    @Data
    public static class CurrentPatient {
        private Long regId;
        private Integer queueNumber;
        private String patientName;
        private String roomNumber;
    }
    
    @Data
    public static class WaitingPatient {
        private Long regId;
        private Integer queueNumber;
        private String patientName;
    }
}
