package com.tinyhis.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.entity.Registration;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
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
        wrapper.eq(Registration::getStatus, 0) // Pending payment
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
}
