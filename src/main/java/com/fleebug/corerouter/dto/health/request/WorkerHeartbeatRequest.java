package com.fleebug.corerouter.dto.health.request;

import lombok.Data;

@Data
public class WorkerHeartbeatRequest {
    private String instanceId;
    private String serviceName;
    private String status;
}
