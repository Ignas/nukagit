package lt.pow.nukagit.db.repositories;

public class InvalidUsernameException extends Exception {
    public InvalidUsernameException(String msg) {
        super(msg);
    }
}
