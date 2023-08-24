package lt.pow.nukagit.dfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lt.pow.nukagit.db.dao.NukagitDfsDao;
import lt.pow.nukagit.db.entities.ImmutablePack;
import lt.pow.nukagit.db.entities.Pack;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsOutputStream;
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription;
import org.eclipse.jgit.internal.storage.dfs.DfsReaderOptions;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.ReadableChannel;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NukagitDfsObjDatabase extends DfsObjDatabase {
  Logger LOGGER = LoggerFactory.getLogger(NukagitDfsObjDatabase.class);
  private final NukagitDfsDao dfsDao;
  private final int blockSize;

  private final UUID repositoryId;

  public NukagitDfsObjDatabase(
      NukagitDfsRepository nukagitDfsRepository,
      NukagitDfsDao dfsDao,
      DfsReaderOptions readerOptions,
      int blockSize) {
    super(nukagitDfsRepository, readerOptions);
    LOGGER.debug(
        "NukagitDfsObjDatabase: blockSize={} for repository {}",
        blockSize,
        nukagitDfsRepository.getDescription().getRepositoryName());
    // Should I add one more abstraction layer like a repository class on top of all the dao classes
    // and minio?
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
          desc.setFileSize(ext, pack.file_size());
        });
    desc.setObjectCount(firstPack.object_count());
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
      Collection<DfsPackDescription> desc, Collection<DfsPackDescription> replace) {
    LOGGER.debug("commitPackImpl: desc={}, replace={}", desc, replace);
    ArrayList<Pack> newPacks = mapPackDescriptionsToPacks(desc);
    ArrayList<Pack> removePacks = mapPackDescriptionsToPacks(replace);
    LOGGER.debug("commitPackImpl: newPacks={}, removePacks={}", newPacks, removePacks);
    dfsDao.commitPack(repositoryId, newPacks, removePacks);
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
                              .file_size(extSize)
                              .object_count(objectCount)
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
    // Pass along minio client and database client
    return new MinioBlockReadableChannel((MinioPack) desc, blockSize);
  }

  @Override
  protected DfsOutputStream writeFile(DfsPackDescription desc, PackExt ext) throws IOException {
    LOGGER.debug("writeFile: desc={}, ext={}", desc, ext);
    return new Out((MinioPack) desc, ext, blockSize);
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
    private final int blockSize;

    public Out(MinioPack desc, PackExt ext, int blockSize) {
      this.blockSize = blockSize;
      // Pass in minio client
    }

    @Override
    public int blockSize() {
      return blockSize;
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this output stream.
     * The general contract for write(b, off, len) is that some of the bytes in the array b are
     * written to the output stream in order; element b[off] is the first byte written and
     * b[off+len-1] is the last byte written by this operation. The write method of OutputStream
     * calls the write method of one argument on each of the bytes to be written out. Subclasses are
     * encouraged to override this method and provide a more efficient implementation. If b is null,
     * a NullPointerException is thrown. If off is negative, or len is negative, or off+len is
     * greater than the length of the array b, then an IndexOutOfBoundsException is thrown.
     */
    @Override
    @WithSpan
    public void write(byte[] buf, int off, int len) throws IOException {
      LOGGER.debug("write: buf.length={}, off={}, len={}", buf.length, off, len);
    }

    /**
     * Read back a portion of already written data. The writing position of the output stream is not
     * affected by a read. Params: position – offset to read from. buf – buffer to populate. Up to
     * buf.remaining() bytes will be read from position. Returns: number of bytes actually read.
     * Throws: IOException – reading is not supported, or the read cannot be performed due to DFS
     * errors.
     *
     * <p>NOTE: this has to support reading from packs not yet flushed.
     */
    @Override
    @WithSpan
    public int read(long position, ByteBuffer buf) throws IOException {
      LOGGER.debug("read: position={}, buf={}", position, buf);
      return 0;
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be written out. The
     * general contract of {@code flush} is that calling it is an indication that, if any bytes
     * previously written have been buffered by the implementation of the output stream, such bytes
     * should immediately be written to their intended destination.
     *
     * <p>If the intended destination of this stream is an abstraction provided by the underlying
     * operating system, for example a file, then flushing the stream guarantees only that bytes
     * previously written to the stream are passed to the operating system for writing; it does not
     * guarantee that they are actually written to a physical device such as a disk drive.
     *
     * <p>The {@code flush} method of {@code OutputStream} does nothing.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    @WithSpan
    public void flush() throws IOException {
      LOGGER.debug("flush");
    }
  }

  private static class MinioBlockReadableChannel implements ReadableChannel {
    Logger LOGGER = LoggerFactory.getLogger(MinioBlockReadableChannel.class);
    private final int blockSize;
    private int position;
    private boolean open = true;
    private int readAheadBytes;

    public MinioBlockReadableChannel(MinioPack desc, int blockSize) {
      this.blockSize = blockSize;
      this.position = 0;
      this.readAheadBytes = 0;
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p>An attempt is made to read up to <i>r</i> bytes from the channel, where <i>r</i> is the
     * number of bytes remaining in the buffer, that is, {@code dst.remaining()}, at the moment this
     * method is invoked.
     *
     * <p>Suppose that a byte sequence of length <i>n</i> is read, where {@code 0}&nbsp;{@code
     * <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>. This byte sequence will be transferred into
     * the buffer so that the first byte in the sequence is at index <i>p</i> and the last byte is
     * at index <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>&nbsp;{@code -}&nbsp;{@code 1}, where <i>p</i>
     * is the buffer's position at the moment this method is invoked. Upon return the buffer's
     * position will be equal to <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>; its limit will not have
     * changed.
     *
     * <p>A read operation might not fill the buffer, and in fact it might not read any bytes at
     * all. Whether or not it does so depends upon the nature and state of the channel. A socket
     * channel in non-blocking mode, for example, cannot read any more bytes than are immediately
     * available from the socket's input buffer; similarly, a file channel cannot read any more
     * bytes than remain in the file. It is guaranteed, however, that if a channel is in blocking
     * mode and there is at least one byte remaining in the buffer then this method will block until
     * at least one byte is read.
     *
     * <p>This method may be invoked at any time. If another thread has already initiated a read
     * operation upon this channel, however, then an invocation of this method will block until the
     * first operation is complete.
     *
     * @param dst The buffer into which bytes are to be transferred
     * @return The number of bytes read, possibly zero, or {@code -1} if the channel has reached
     *     end-of-stream
     * @throws IllegalArgumentException If the buffer is read-only
     * @throws NonReadableChannelException If this channel was not opened for reading
     * @throws ClosedChannelException If this channel is closed
     * @throws AsynchronousCloseException If another thread closes this channel while the read
     *     operation is in progress
     * @throws ClosedByInterruptException If another thread interrupts the current thread while the
     *     read operation is in progress, thereby closing the channel and setting the current
     *     thread's interrupt status
     * @throws IOException If some other I/O error occurs
     */
    @Override
    @WithSpan
    public int read(ByteBuffer dst) throws IOException {
      LOGGER.debug("read: dst={}", dst);
      return 0;
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
      LOGGER.debug("size");
      // Capture as part of the pack desc?
      return 0;
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
      // Might want to signal this to the cache
      open = false;
    }
  }
}
