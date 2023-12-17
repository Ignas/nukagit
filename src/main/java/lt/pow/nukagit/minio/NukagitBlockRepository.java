package lt.pow.nukagit.minio;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NukagitBlockRepository {

    private final MinioClient minio;
    private final LoadingCache<String, byte[]> blockCache;

    @Inject
    public NukagitBlockRepository(MinioClient minio) {
        this.minio = minio;
        this.blockCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(CacheLoader.from(this::getBlock));
    }

    private byte[] getBlock(String key) throws UncheckedIOException {
        try {
            try {
                return minio
                        .getObject(GetObjectArgs.builder().bucket("nukagit").object(key).build())
                        .readAllBytes();
            } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(
                    String.format("Failed to load a block %s from cache", key), ex);
        }
    }

    public byte[] getBlock(UUID repositoryId, long blockNumber, String fileName) throws IOException {
        try {
            return blockCache.getUnchecked(getBlockKey(repositoryId, blockNumber, fileName));
        } catch (UncheckedIOException e) {
            throw new IOException(e);
        }
    }

    public void putBlock(
            UUID repositoryId,
            long blockNumber,
            String fileName,
            byte[] buffer,
            int length) throws IOException {
        try {
            minio.putObject(
                    PutObjectArgs.builder()
                            .bucket("nukagit")
                            .object(getBlockKey(repositoryId, blockNumber, fileName))
                            .stream(new ByteArrayInputStream(buffer, 0, length), length, -1)
                            .contentType("application/octet-stream")
                            .build());
            byte[] truncatedBuffer = new byte[length];
            System.arraycopy(buffer, 0, truncatedBuffer, 0, length);
            blockCache.put(getBlockKey(repositoryId, blockNumber, fileName), truncatedBuffer);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String getBlockKey(UUID repositoryId, long blockNumber, String fileName) {
        return String.format(
                "%s/%05d-%s",
                repositoryId,
                blockNumber,
                fileName);
    }

    public int getBlockSize() {
        return 1024 * 1024;
    }
}
