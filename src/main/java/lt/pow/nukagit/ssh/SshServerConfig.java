package lt.pow.nukagit.ssh;

public interface SshServerConfig {
  Integer port();

  String hostname();

  String hostKey();

  String hostKeyAlgorithm();
}
