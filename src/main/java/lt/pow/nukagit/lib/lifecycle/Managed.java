package lt.pow.nukagit.lib.lifecycle;

import java.io.Closeable;

public interface Managed extends AutoCloseable, Closeable {
  void start();
}
