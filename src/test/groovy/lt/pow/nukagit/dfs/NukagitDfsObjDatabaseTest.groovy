package lt.pow.nukagit.dfs

import io.minio.MinioClient
import lt.pow.nukagit.db.dao.NukagitDfsDao
import lt.pow.nukagit.db.entities.ImmutablePack
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import org.eclipse.jgit.internal.storage.dfs.DfsReaderOptions
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.pack.PackExt
import org.jeasy.random.EasyRandom
import spock.lang.Specification

class NukagitDfsObjDatabaseTest extends Specification {
    EasyRandom random = new EasyRandom()
    NukagitDfsObjDatabase nukagitDfsObjDatabase
    NukagitDfsRepository nukagitDfsRepository
    NukagitDfsDao nukagitDfsDao
    MinioClient minioClient
    UUID repositoryId = UUID.randomUUID()

    def setup() {
        nukagitDfsDao = Mock(NukagitDfsDao.class)
        nukagitDfsRepository = Mock(NukagitDfsRepository.class)
        nukagitDfsRepository.getDescription() >> new NukagitDfsRepositoryDescription(repositoryId, random.nextObject(String.class))
        minioClient = Mock(MinioClient.class)
        nukagitDfsObjDatabase = new NukagitDfsObjDatabase(
                nukagitDfsRepository,
                nukagitDfsDao,
                minioClient,
                new DfsReaderOptions(),
                1024)
    }

    def "mapPacksToPackDescriptions can map a single pack into a description"() {
        given:
        def blockSize = Math.abs(random.nextInt())
        def packExt = random.nextObject(PackExt.class)
        def packSource = random.nextObject(DfsObjDatabase.PackSource.class)
        def pack = random.nextObject(ImmutablePack.class)
                .withExt(packExt.extension)
                .withSource(packSource.name())
                .withFile_size(Math.abs(random.nextInt()))
        def dfsRepositoryDescription = new DfsRepositoryDescription("repo")
        when:
        def packDescription = nukagitDfsObjDatabase.mapPacksToPackDescriptions(dfsRepositoryDescription, blockSize, [pack])
        then:
        packDescription.getFileName(PackExt.PACK) == "${pack.name()}.pack"
        packDescription.repositoryDescription == dfsRepositoryDescription
        packDescription.packSource == packSource
        packDescription.hasFileExt(packExt)
        packDescription.getBlockSize(packExt) == blockSize
        packDescription.getFileSize(packExt) == pack.file_size()
    }

    def "mapPacksToPackDescriptions can map multiple packs into a description with multiple extensions"() {
        given:
        def packName = random.nextObject(String.class)
        def packSource = random.nextObject(DfsObjDatabase.PackSource.class)
        def objectCount = Math.abs(random.nextInt())
        def packExtensions = [PackExt.PACK, PackExt.INDEX, PackExt.BITMAP_INDEX]
        def fileSizes = random.ints(3).map { i -> Math.abs(i) }.toArray()

        def packs = new ArrayList<ImmutablePack>()
        for (int i = 0; i < 3; i++) {
            packs.add(ImmutablePack.builder()
                    .name(packName)
                    .source(packSource.name())
                    .ext(packExtensions.get(i).extension)
                    .file_size(fileSizes[i])
                    .object_count(objectCount)
                    .min_update_index(1)
                    .max_update_index(1)
                    .build())
        }

        def dfsRepositoryDescription = new DfsRepositoryDescription("repo")
        def blockSize = Math.abs(random.nextInt())
        when:
        def packDescription = nukagitDfsObjDatabase.mapPacksToPackDescriptions(dfsRepositoryDescription, blockSize, packs)
        then:
        packDescription.getFileName(PackExt.PACK) == "${packs[0].name()}.pack"
        packDescription.repositoryDescription == dfsRepositoryDescription
        packDescription.packSource == packSource
        packDescription.objectCount == objectCount
        for (int i = 0; i < 3; i++) {
            def packExt = packExtensions.get(i)
            assert packDescription.hasFileExt(packExt)
            assert packDescription.getBlockSize(packExt) == blockSize
            assert packDescription.getFileSize(packExt) == fileSizes[i]
        }
    }

    def "mapPackDescriptionsToPacks creates a Pack object for every extension"() {
        given:
        def packName = random.nextObject(String.class)
        def packSource = random.nextObject(DfsObjDatabase.PackSource.class)
        def objectCount = Math.abs(random.nextInt())
        // These should be sorted
        def packExtensions = [PackExt.PACK, PackExt.INDEX, PackExt.BITMAP_INDEX]
        def fileSizes = random.ints(3).map { i -> Math.abs(i) }.toArray()

        def dfsRepositoryDescription = new DfsRepositoryDescription("repo")
        def blockSize = Math.abs(random.nextInt())
        def minUpdateIndex = Math.abs(random.nextInt())
        def maxUpdateIndex = Math.abs(random.nextInt())

        // Construct pack description with all the extensions
        def packDescription = new DfsPackDescription(dfsRepositoryDescription, packName, packSource)
        packDescription.setObjectCount(objectCount)
        for (int i = 0; i < 3; i++) {
            def packExt = packExtensions.get(i)
            packDescription.addFileExt(packExt)
            packDescription.setBlockSize(packExt, blockSize)
            packDescription.setFileSize(packExt, fileSizes[i])
            packDescription.setMinUpdateIndex(minUpdateIndex)
            packDescription.setMaxUpdateIndex(maxUpdateIndex)
        }

        // Make the expected packs
        def expectedPacks = new ArrayList<ImmutablePack>()
        for (int i = 0; i < 3; i++) {
            expectedPacks.add(ImmutablePack.builder()
                    .name(packName)
                    .source(packSource.name())
                    .ext(packExtensions.get(i).extension)
                    .file_size(fileSizes[i])
                    .object_count(objectCount)
                    .min_update_index(minUpdateIndex)
                    .max_update_index(maxUpdateIndex)
                    .build())
        }
        when:
        def resultPacks = nukagitDfsObjDatabase.mapPackDescriptionsToPacks([packDescription])
        then:
        resultPacks == expectedPacks
    }

    def "test listPacks"() {
        given:
        nukagitDfsDao.listPacks(repositoryId) >> List.of()
        expect:
        nukagitDfsObjDatabase.listPacks() == List.of()
    }

    def "test commitPackImpl"() {
        when:
        nukagitDfsObjDatabase.commitPackImpl(List.of(), List.of())
        then:
        1 * nukagitDfsDao.commitPack(repositoryId, _, _)
    }
}
