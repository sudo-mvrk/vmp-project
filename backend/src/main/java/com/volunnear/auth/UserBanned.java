package com.volunnear.auth;

/** Published inside the ban transaction; {@link UserSessions} ends the user's sessions after the commit (Q23). */
public record UserBanned(String username) {
}
