package com.tinyhis.aspect;

import com.tinyhis.annotation.CheckUserAccess;
import com.tinyhis.config.UserPrincipal;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

@Aspect
@Component
public class UserAccessAspect {

    @Before("@annotation(com.tinyhis.annotation.CheckUserAccess)")
    public void checkAccess(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Not authenticated");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        // If user is not a patient (e.g. doctor, admin), allow access
        if (!"PATIENT".equals(principal.getRole()) && !"ROLE_PATIENT".equals(principal.getRole())) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CheckUserAccess annotation = method.getAnnotation(CheckUserAccess.class);
        String paramName = annotation.paramName();

        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();

        Long requestedId = null;
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                if (args[i] instanceof Long) {
                    requestedId = (Long) args[i];
                } else if (args[i] instanceof String) {
                    try {
                        requestedId = Long.parseLong((String) args[i]);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                break;
            }
        }

        if (requestedId == null) {
            // If we can't find the ID, maybe we should block or allow? 
            // Safest is to block if we expected to check it.
            throw new AccessDeniedException("Cannot verify user access: ID parameter not found");
        }

        if (!Objects.equals(principal.getUserId(), requestedId)) {
            throw new AccessDeniedException("You do not have permission to access this data");
        }
    }
}
