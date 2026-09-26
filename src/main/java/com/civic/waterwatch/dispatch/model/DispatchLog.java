package com.civic.waterwatch.dispatch.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DispatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "cluster_code", length = 32)
    private String clusterCode;

    @Column(name = "ward_number")
    private Integer wardNumber;

    @Column(name = "recipient", length = 128)
    private String recipient;

    @Column(name = "dispatch_channel", length = 32)
    private String dispatchChannel; // EMAIL, WEBHOOK, CITIZEN_NOTICE

    @Column(name = "status", length = 32)
    private String status; // DELIVERED, SIMULATED, FAILED

    @Column(name = "payload_summary", length = 1000)
    private String payloadSummary;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "dispatched_at", nullable = false)
    private LocalDateTime dispatchedAt;

    public DispatchLog(Long clusterId, String clusterCode, Integer wardNumber, String recipient,
                       String dispatchChannel, String status, String payloadSummary,
                       Integer responseCode) {
        this.clusterId = clusterId;
        this.clusterCode = clusterCode;
        this.wardNumber = wardNumber;
        this.recipient = recipient;
        this.dispatchChannel = dispatchChannel;
        this.status = status;
        this.payloadSummary = payloadSummary;
        this.responseCode = responseCode;
        this.dispatchedAt = LocalDateTime.now();
    }
}
