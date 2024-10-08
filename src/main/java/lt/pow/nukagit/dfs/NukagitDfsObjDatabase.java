package lt.pow.nukagit.dfs;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lt.pow.nukagit.db.dao.NukagitDfsObjDao;
import lt.pow.nukagit.db.dao.NukagitDfsPackConflictException;
import lt.pow.nukagit.db.entities.ImmutablePack;
import lt.pow.nukagit.db.entities.Pack;
import lt.pow.nukagit.minio.NukagitBlockRepository;
import org.eclipse.jgit.internal.storage.dfs.*;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class NukagitDfsObjDatabase extends DfsObjDatabase {
    private final Logger LOGGER = LoggerFactory.getLogger(NukagitDfsObjDatabase.class);

    private final NukagitBlockRepository blockRepository;
    private final NukagitDfsObjDao dfsDao;
    private final int blockSize;

    private final UUID repositoryId;

    public NukagitDfsObjDatabase(
            NukagitDfsRepository nukagitDfsRepository,
            NukagitDfsObjDao dfsDao,
            NukagitBlockRepository blockRepository, DfsReaderOptions readerOptions,
            int blockSize) {
        super(nukagitDfsRepository, readerOptions);
        this.blockRepository = blockRepository;
        LOGGER.debug(
                "NukagitDfsObjDatabase: blockSize={} for repository {}",
                blockSize,
                nukagitDfsRepository.getDescription().getRepositoryName());
        this.dfsDao = dfsDao;
        this.blockSize = blockSize;
        this.repositoryId =
                ((NukagitDfsRepositoryDescription) getRepository().getDescription()).getRepositoryId();
    }

    @Override
    @WithSpan
    protected List<DfsPackDescription> listPacks() {
        var packDescriptionList =
                dfsDao.listPacks(repositoryId).stream()
                        // Group packs by name + source
                        // Each pack will contain all the exts
                        .collect(
                                Collectors.groupingBy(pack -> String.format("%s\0%s", pack.name(), pack.source())))
                        .values()
                        .stream()
                        .map(
                                (List<Pack> packs) ->
                                        mapPacksToPackDescriptions(getRepository().getDescription(), blockSize, packs))
                        .toList();
        LOGGER.debug("listPacks returning {} packs", packDescriptionList.size());
        // This method must return a mutable list.
        return new ArrayList<>(packDescriptionList);
    }

    @NotNull
    public static DfsPackDescription mapPacksToPackDescriptions(
            DfsRepositoryDescription repositoryDescription, int blockSize, List<Pack> packs) {
        var firstPack = packs.get(0);
        var packSource = PackSource.valueOf(firstPack.source());
        // TODO: probably want to pass the repo uuid along
        var desc = new MinioPack(firstPack.name(), repositoryDescription, packSource);
        packs.forEach(
                pack -> {
                    var ext = getExt(pack);
                    desc.addFileExt(ext);
                    desc.setBlockSize(ext, blockSize);
                    desc.setFileSize(ext, pack.fileSize());
                });
        desc.setObjectCount(firstPack.objectCount());
        return desc;
    }

    @NotNull
    private static PackExt getExt(Pack pack) {
        var ext =
                Arrays.stream(PackExt.values())
                        .filter(packExt -> packExt.getExtension().equals(pack.ext()))
                        .findFirst();
        return ext.orElseThrow(
                () -> new IllegalArgumentException(String.format("Invalid pack Extension %s", pack.ext())));
    }

    @Override
    @WithSpan
    protected DfsPackDescription newPack(PackSource source) {
        var packName = "pack-" + UUID.randomUUID() + "-" + source.name();
        var pack = new MinioPack(packName, getRepository().getDescription(), source);
        LOGGER.debug("newPack: pack={} source={} packName={}", pack, source, packName);
        return pack;
    }

    @Override
    @WithSpan
    protected void commitPackImpl(
            Collection<DfsPackDescription> desc, Collection<DfsPackDescription> replace) throws IOException {
        LOGGER.debug("commitPackImpl: desc={}, replace={}", desc, replace);
        ArrayList<Pack> newPacks = mapPackDescriptionsToPacks(desc);
        ArrayList<Pack> removePacks = mapPackDescriptionsToPacks(replace);
        LOGGER.debug("commitPackImpl: newPacks={}, removePacks={}", newPacks, removePacks);
        try {
            dfsDao.commitPack(repositoryId, newPacks, removePacks);
        } catch (NukagitDfsPackConflictException e) {
            // Conflicts in git happen
            LOGGER.info("commitPackImpl: encountered conflict when committing packs", e);
            throw new IOException(e);
        }
        clearCache();
    }

    @NotNull
    @VisibleForTesting
    public static ArrayList<Pack> mapPackDescriptionsToPacks(Collection<DfsPackDescription> desc) {
        var packs = new ArrayList<Pack>();
        if (desc == null) {
            return packs;
        }
        desc.forEach(
                packDesc -> {
                    // This is the only way to get the pack name
                    var name = packDesc.getFileName(PackExt.PACK);
                    int dot = name.lastIndexOf('.');
                    var packName = (dot < 0) ? name : name.substring(0, dot);
                    var packSource = packDesc.getPackSource();
                    Arrays.stream(PackExt.values())
                            .forEach(
                                    ext -> {
                                        if (packDesc.hasFileExt(ext)) {
                                            var extSize = packDesc.getFileSize(ext);
                                            var objectCount = packDesc.getObjectCount();
                                            packs.add(
                                                    ImmutablePack.builder()
                                                            .name(packName)
                                                            .source(packSource.name())
                                                            .ext(ext.getExtension())
                                                            .fileSize(extSize)
                                                            .objectCount(objectCount)
                                                            .minUpdateIndex(packDesc.getMinUpdateIndex())
                                                            .maxUpdateIndex(packDesc.getMaxUpdateIndex())
                                                            .build());
                                        }
                                    });
                });
        return packs;
    }

    @Override
    @WithSpan
    protected void rollbackPack(Collection<DfsPackDescription> desc) {
        LOGGER.debug("rollbackPack: desc={}", desc);
        // Do nothing. Pack is not recorded until commitPack.
    }

    @Override
    protected ReadableChannel openFile(DfsPackDescription desc, PackExt ext) throws IOException {
        LOGGER.debug("openFile: desc={}, ext={}", desc, ext);
        return new MinioBlockReadableChannel(blockRepository, (MinioPack) desc, ext, blockSize);
    }

    @Override
    protected DfsOutputStream writeFile(DfsPackDescription desc, PackExt ext) throws IOException {
        LOGGER.debug("writeFile: desc={}, ext={}", desc, ext);
        return new Out(blockRepository, (MinioPack) desc, ext, blockSize);
    }

    @Override
    @WithSpan
    public long getApproximateObjectCount() {
        LOGGER.debug("getApproximateObjectCount");
        // TODO: reimplement as a single query
        long count = 0;
        for (DfsPackDescription p : listPacks()) {
            count += p.getObjectCount();
        }
        return count;
    }

    private static class MinioPack extends DfsPackDescription {
        MinioPack(String name, DfsRepositoryDescription repoDesc, PackSource source) {
            super(repoDesc, name, source);
        }
    }

    private static class Out extends DfsOutputStream {
        Logger LOGGER = LoggerFactory.getLogger(Out.class);
        private final byte[] buffer;
        private final ByteArrayOutputStream wholePackBuffer;
        private int positionInChunk;
        private int chunkCount;
        private final NukagitBlockRepository blockRepository;
        private final MinioPack desc;
        private final PackExt ext;
        private final int blockSize;

        public Out(NukagitBlockRepository blockRepository, MinioPack desc, PackExt ext, int blockSize) {
            this.blockRepository = blockRepository;
            this.desc = desc;
            this.ext = ext;
            this.blockSize = blockSize;
            this.positionInChunk = 0;
            this.chunkCount = 0;
            this.buffer = new byte[blockSize];
            this.wholePackBuffer = new ByteArrayOutputStream();
        }

        @Override
        public int blockSize() {
            return blockSize;
        }

        @Override
        @WithSpan
        public void write(byte[] buf, int off, @SpanAttribute int len) throws IOException {
            LOGGER.debug("write: buf.length={}, off={}, len={}", buf.length, off, len);
            wholePackBuffer.write(buf, off, len);
            int remaining = len;
            int offset = off;

            while (remaining > 0) {
                int bytesToWrite = Math.min(remaining, blockSize - positionInChunk);
                System.arraycopy(buf, offset, buffer, positionInChunk, bytesToWrite);
                positionInChunk += bytesToWrite;
                remaining -= bytesToWrite;
                offset += bytesToWrite;

                if (positionInChunk == blockSize) {
                    flushChunk();
                }
            }
        }

        @Override
        @WithSpan
        public int read(@SpanAttribute long position, ByteBuffer buf) throws IOException {
            LOGGER.debug("read: position={}, buf={}", position, buf);
            byte[] byteArray = wholePackBuffer.toByteArray();
            int length = byteArray.length;
            int remaining = Math.min(buf.remaining(), length - (int) position);

            if (remaining <= 0) {
                return -1; // End of data
            }

            buf.put(byteArray, (int) position, remaining);
            return remaining;
        }

        @WithSpan
        public void flushChunk() throws IOException {
            if (positionInChunk == 0) {
                return;
            }
            blockRepository.putBlock(((NukagitDfsRepositoryDescription) desc.getRepositoryDescription())
                            .getRepositoryId(),
                    chunkCount,
                    desc.getFileName(ext),
                    buffer,
                    positionInChunk);
            this.chunkCount += 1;
            this.positionInChunk = 0;
        }

        @Override
        @WithSpan
        public void flush() throws IOException {
            LOGGER.debug("flush");
            flushChunk();
        }

        @Override
        @WithSpan
        public void close() throws IOException {
            flushChunk();
            super.close();
        }
    }

    private static final class MinioBlockReadableChannel implements ReadableChannel {
        private final Logger LOGGER = LoggerFactory.getLogger(MinioBlockReadableChannel.class);

        private final NukagitBlockRepository blockRepository;
        private final PackExt ext;
        private final int blockSize;
        private final MinioPack desc;
        private int position;
        private boolean open = true;
        private int readAheadBytes;

        public MinioBlockReadableChannel(
                NukagitBlockRepository blockRepository, MinioPack desc, PackExt ext, int blockSize) {
            this.blockRepository = blockRepository;
            this.ext = ext;
            this.blockSize = blockSize;
            this.position = 0;
            this.readAheadBytes = 0;
            this.desc = desc;
        }

        @Override
        @WithSpan
        public int read(ByteBuffer dst) throws IOException {
            LOGGER.debug("read: dst={}", dst);
            long start = position();
            int positionInBlock = (int) (position() % blockSize);
            int totalBytesRead = 0;

            while (dst.remaining() > 0) {
                long blockNumber = start / blockSize;

                byte[] blockData = blockRepository.getBlock(
                        ((NukagitDfsRepositoryDescription) desc.getRepositoryDescription()).getRepositoryId(),
                        blockNumber,
                        desc.getFileName(ext));
                int bytesToRead = Math.min(Math.min(blockSize - positionInBlock, dst.remaining()), blockData.length);

                dst.put(blockData, positionInBlock, bytesToRead);
                totalBytesRead += bytesToRead;

                positionInBlock = 0; // Reset positionInBlock for subsequent blocks
                start += bytesToRead;
                if (start >= size()) {
                    break; // Reached end of the file
                }
            }
            return totalBytesRead;
        }

        @Override
        public long position() throws IOException {
            return position;
        }

        @Override
        @WithSpan
        public void position(long newPosition) throws IOException {
            LOGGER.debug("position: newPosition={}", newPosition);
            position = (int) newPosition;
        }

        @Override
        public long size() throws IOException {
            var size = desc.getFileSize(ext);
            LOGGER.debug("size={}", size);
            return size;
        }

        @Override
        public int blockSize() {
            return blockSize;
        }

        @Override
        public void setReadAheadBytes(int bufferSize) throws IOException {
            LOGGER.debug("setReadAheadBytes: bufferSize={}", bufferSize);
            this.readAheadBytes = bufferSize;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        @WithSpan
        public void close() {
            LOGGER.debug("close");
            open = false;
        }
    }
}
