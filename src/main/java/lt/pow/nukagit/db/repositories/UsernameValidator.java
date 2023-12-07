package lt.pow.nukagit.db.repositories;

import javax.inject.Inject;

public class UsernameValidator {

    @Inject
    public UsernameValidator() {
    }
    public boolean isValid(String username) {
        String validUserRegex = "^[a-z_][a-z0-9_-]*[$]?$";
        return username.matches(validUserRegex);
    }
}
