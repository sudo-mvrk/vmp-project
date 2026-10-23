package com.volunnear.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/**
 * Q23: a ban deletes every session of the user, so an open session gets 401 on its next request.
 * Needs the indexed session repository (Redis {@code repository-type: indexed}), which finds sessions by username.
 * Runs after the commit: deleting earlier would let a login that starts before the commit keep a fresh session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessions {
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBanned(UserBanned event) {
        Set<String> ids = sessionRepository.findByPrincipalName(event.username()).keySet();
        ids.forEach(sessionRepository::deleteById);
        log.info("Banned user {}: {} session(s) deleted", event.username(), ids.size());
    }
}
