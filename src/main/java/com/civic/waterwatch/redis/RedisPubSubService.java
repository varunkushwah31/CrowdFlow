package com.civic.waterwatch.redis;

import com.civic.waterwatch.config.RedisConfig;
import com.civic.waterwatch.incident.service.LiveTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Cross-Instance Incident Event Publisher & Subscriber using Redis Pub/Sub.
 * Enables real-time synchronization of SSE streams across multiple application containers.
 */
@Service
@Slf4j
public class RedisPubSubService implements MessageListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic topic;
    private final RedisConnectionFactory connectionFactory;
    private final LiveTrackingService liveTrackingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisPubSubService(
            StringRedisTemplate stringRedisTemplate,
            ChannelTopic topic,
            RedisConnectionFactory connectionFactory,
            @Lazy LiveTrackingService liveTrackingService
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.topic = topic;
        this.connectionFactory = connectionFactory;
        this.liveTrackingService = liveTrackingService;
    }

    @PostConstruct
    public void registerSubscriber() {
        try {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.addMessageListener(this, topic);
            container.afterPropertiesSet();
            container.start();
            log.info("Redis Pub/Sub message listener subscribed to channel '{}'", topic.getTopic());
        } catch (Exception e) {
            log.warn("Could not register Redis Pub/Sub listener: {}", e.getMessage());
        }
    }

    /**
     * Publishes an incident state change event to Redis topic.
     */
    public void publishIncidentUpdate(String reportCode, Long clusterId, String eventType) {
        try {
            IncidentEventMessage event = new IncidentEventMessage(
                    reportCode, clusterId, eventType, LocalDateTime.now().toString()
            );
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(topic.getTopic(), json);
            log.debug("Published incident update to Redis Pub/Sub: {}", json);
        } catch (Exception e) {
            log.error("Failed to publish incident update to Redis Pub/Sub: {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            IncidentEventMessage event = objectMapper.readValue(payload, IncidentEventMessage.class);
            log.debug("Received Redis Pub/Sub event for report: {}", event.getReportCode());

            if (event.getReportCode() != null && liveTrackingService != null) {
                liveTrackingService.broadcastLocalSse(event.getReportCode());
            }
        } catch (Exception e) {
            log.error("Error processing Redis Pub/Sub message: {}", e.getMessage());
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentEventMessage implements Serializable {
        private String reportCode;
        private Long clusterId;
        private String eventType;
        private String timestamp;
    }
}
