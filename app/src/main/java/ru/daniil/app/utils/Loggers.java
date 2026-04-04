package ru.daniil.app.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Утилитарный класс для работы с логгерами
 */
public class Loggers {

    public static final Logger APP = LoggerFactory.getLogger("APP-LOGGER");
    public static final Logger INFO = LoggerFactory.getLogger("INFO-LOGGER");
    public static final Logger AUDIT = LoggerFactory.getLogger("AUDIT-LOGGER");
    public static final Logger DB = LoggerFactory.getLogger("DB-LOGGER");

    private Loggers() {
    }

    /**
     * Инициализирует MDC для трассировки запроса
     */
    public static String initRequestContext(HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        MDC.put("userAgent", request.getHeader("User-Agent"));

        APP.info("Начало обработки запроса {} {}", request.getMethod(), request.getRequestURI());

        return requestId;
    }

    /**
     * Для завершения контекста запроса
     */
    public static void finishRequestContext(int statusCode, long durationMs) {
        MDC.put("status", String.valueOf(statusCode));
        MDC.put("duration", String.valueOf(durationMs));

        String level = statusCode >= 400 ? "ERROR" : "INFO";

        if ("ERROR".equals(level)) {
            APP.error("Завершение запроса с кодом {} за {} мс", statusCode, durationMs);
        } else {
            APP.info("Завершение запроса с кодом {} за {} мс", statusCode, durationMs);
        }

        MDC.clear();
    }

    /**
     * Логирует обычное событие
     */
    public static void logInfo(String method, String message) {
        INFO.info("Выполнение метода: {} | Сообщение: {}", method, message);
    }

    /**
     * Логирует аудит действие
     */
    public static void logAudit(String action, String entityType, Long entityId, String details) {
        String userId = getCurrentUserId();
        AUDIT.info("Аудит | Действие: {} | Объект: {} | ID: {} | Пользователь: {} | Детали: {}",
                action, entityType, entityId, userId, details);
    }

    /**
     * Логирует SQL запрос
     */
    public static void logSql(String sql, Object[] params, long durationMs) {
        if (durationMs > 1000) {
            DB.warn("Медленный SQL ({} мс): {} | Параметры: {}", durationMs, sql, params);
        } else {
            DB.debug("SQL ({} мс): {} | Параметры: {}", durationMs, sql, params);
        }
    }

    /**
     * Логирует ошибку с контекстом
     */
    public static void logError(String message, Throwable throwable, Object... context) {
        APP.error("Ошибка: {} | Контекст: {} | Исключение: {}",
                message, context, throwable != null ? throwable.getMessage() : "null", throwable);
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private static String getCurrentUserId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getRemoteUser() != null ? request.getRemoteUser() : "anonymous";
            }
        } catch (Exception e) {
            // Ошибки игнорируются
        }
        return "system";
    }
}
