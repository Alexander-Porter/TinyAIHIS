package com.tinyhis.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.entity.Registration;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("!test") // Exclude from test profile
@RequiredArgsConstructor
public class RegistrationTask {

    private final RegistrationMapper registrationMapper;
    private final RegistrationService registrationService;

    /**
     * Check for unpaid registrations every minute
     * Cancel if created more than 15 minutes ago
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelUnpaidRegistrations() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(15);
        
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getStatus, 0) // 待支付
               .lt(Registration::getCreateTime, expireTime);
               
        List<Registration> expiredList = registrationMapper.selectList(wrapper);
        
        if (!expiredList.isEmpty()) {
            log.info("Found {} expired registrations", expiredList.size());
            for (Registration reg : expiredList) {
                try {
                    registrationService.cancelRegistration(reg.getRegId());
                    log.info("Auto-cancelled expired registration: {}", reg.getRegId());
                } catch (Exception e) {
                    log.error("Failed to auto-cancel registration {}", reg.getRegId(), e);
                }
            }
        }
    }

    /**
     * Check for expired registrations (past date) daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkExpiredRegistrations() {
        log.info("Checking for expired registrations...");
        int count = registrationMapper.expirePastRegistrations();
        log.info("Expired {} past registrations.", count);
    }

    /**
     * Check on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        checkExpiredRegistrations();
    }
}
