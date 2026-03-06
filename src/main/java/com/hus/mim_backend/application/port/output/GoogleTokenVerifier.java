package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.auth.model.GoogleUserInfo;

/**
 * Output port for Google ID token verification.
 */
public interface GoogleTokenVerifier {
    GoogleUserInfo verifyIdToken(String idToken);
}
