package lt.pow.nukagit.db.repositories;

public class InvalidPublicKeyTypeException extends InvalidKeyStringException {
    public InvalidPublicKeyTypeException(String msg) {
        super(msg);
    }
}
