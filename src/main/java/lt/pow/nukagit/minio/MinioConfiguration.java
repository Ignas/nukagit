package lt.pow.nukagit.minio;

public interface MinioConfiguration {
    String getAccessKey();

    String getSecretKey();

    String getEndpoint();

    String getBucket();

    Integer getBlockSize();
}
