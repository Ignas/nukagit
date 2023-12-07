package lt.pow.nukagit.db.repositories;

public class InvalidKeyStringException extends Exception {
    public InvalidKeyStringException(String msg, Exception cause) {
        super(msg, cause);
    }

    public InvalidKeyStringException(String msg) {
        super(msg);
    }
}
