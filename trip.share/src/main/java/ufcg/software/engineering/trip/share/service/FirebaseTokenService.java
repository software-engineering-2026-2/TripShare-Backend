package ufcg.software.engineering.trip.share.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

/**
 * Valida a identidade Firebase somente durante o login, incluindo revogação e conta desativada.
 */
@Service
public class FirebaseTokenService {

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebaseToken validate(String idToken) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(idToken, true);
    }
}
