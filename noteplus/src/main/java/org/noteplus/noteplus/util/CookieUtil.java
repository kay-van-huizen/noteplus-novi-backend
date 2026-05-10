package org.noteplus.noteplus.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public void addJwtCookie(HttpServletResponse response, String token, long expirySeconds) {
        // Cookie API does not support SameSite — set all attributes via raw header
        response.addHeader("Set-Cookie",
                "jwt=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + expirySeconds);
    }

    public void clearJwtCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", "jwt=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
    }
}
