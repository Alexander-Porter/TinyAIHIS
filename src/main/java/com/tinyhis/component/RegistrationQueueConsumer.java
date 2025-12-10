package com.tinyhis.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyhis.dto.RegistrationRequest;
import com.tinyhis.entity.Registration;
import com.tinyhis.entity.Schedule;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import org.springframework.context.annotation.Profile;

/**
 * Async Consumer for Registration Queue
 * Reads from Redis List and persists to MySQL
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RegistrationQueueConsumer {

    private final StringRedisTemplate redisTemplate;
    private final RegistrationMapper registrationMapper;
    private final ScheduleMapper scheduleMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.registration-queue:registration:queue}")
    private String QUEUE_KEY;
    @Value("${app.registration.fee:50.00}")
    private BigDecimal registrationFee;

    @Value("${app.queue.max-batch-size:50}")
    private int maxBatchSize;

    // 每 10ms 轮询一次队列，也可使用阻塞式 pop 循环以减少 CPU 空转
    // 出于实现简单性的考虑，当前使用 @Scheduled 调度；高吞吐场景建议在 @PostConstruct 启动一个循环线程或使用阻塞 pop
    // 当前实现使用简单的定时任务并按批次处理消息

    @Scheduled(fixedDelayString = "${app.queue.poll-interval-ms:10}")
    public void processQueue() {
        try {
            // 每次批量处理最多 maxBatchSize 条消息
            for (int i = 0; i < maxBatchSize; i++) {
                String message = redisTemplate.opsForList().rightPop(QUEUE_KEY);
                if (message == null) {
                    break;
                }
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("Error processing registration queue", e);
        }
    }

    @Transactional
    public void processMessage(String message) {
        try {
            RegistrationRequest request = objectMapper.readValue(message, RegistrationRequest.class);

            // 1. Get Schedule info (for doctorId)
            Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
            if (schedule == null) {
                log.error("Schedule {} not found for async registration", request.getScheduleId());
                return; // 本处应在真实系统中对 Redis quota 回滚；但现在较复杂，暂不处理
            }

            // 2. Create Registration
            Registration registration = new Registration();
            registration.setPatientId(request.getPatientId());
            registration.setDoctorId(schedule.getDoctorId());
            registration.setScheduleId(request.getScheduleId());
            registration.setStatus(0); // Pending payment
            // 队列号 = 当前已挂号人数 + 1
            // 由于消费者是单线程串行处理消息的，此处直接使用 currentCount + 1 是安全的
            int queueNumber = schedule.getCurrentCount() + 1;
            registration.setQueueNumber(queueNumber);
            registration.setFee(registrationFee);

            registrationMapper.insert(registration);

            // 3. Update Schedule Count (Direct SQL update, no optimistic lock needed as we
            // are the authority now)
            // We just increment it.
            // 注意：这里不需要再次检查配额（Redis 在前面已做原子性配额减扣），因此不会重复校验
            schedule.setCurrentCount(schedule.getCurrentCount() + 1);
            scheduleMapper.updateById(schedule);

            log.info("Async registration success: Patient {} for Schedule {}", request.getPatientId(),
                    request.getScheduleId());

        } catch (Exception e) {
            log.error("Failed to process registration message: {}", message, e);
            // In a real system, push to "Dead Letter Queue"
        }
    }
}
