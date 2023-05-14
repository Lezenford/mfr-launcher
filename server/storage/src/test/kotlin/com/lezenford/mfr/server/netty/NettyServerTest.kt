package com.lezenford.mfr.server.netty

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.netty.*
import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.entity.*
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.model.FileService
import com.lezenford.mfr.server.service.model.LauncherService
import com.lezenford.mfr.server.service.model.OverviewService
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import kotlin.random.Random

@TestPropertySource(
    properties = ["setting.netty.port=\${NettyServerTest.port}",
        "setting.build.local=\${NettyServerTest.serverDirectory}",
        "setting.launcherFolder=\${NettyServerTest.serverDirectory}",
        "setting.netty.timeout=5"
    ]
)
internal class NettyServerTest : BaseTest() {

    @Value("\${setting.netty.port}")
    private var nettyPort: Int = 0

    @MockBean
    private lateinit var fileService: FileService

    @MockBean
    private lateinit var launcherService: LauncherService

    @MockBean
    private lateinit var overviewService: OverviewService

    @Autowired
    private lateinit var factory: HandlerFactory

    @Autowired
    private lateinit var maintenanceService: MaintenanceService

    @BeforeEach
    fun setUp() {
        MaintenanceService.Type.values().forEach { maintenanceService.setDown(it) }
    }

    @Test
    fun `download game file successfully`() {
        val testBranchDirectory = createTempDirectory(TEMP_DIRECTORY)
        val file1 = createTempFile(testBranchDirectory).also {
            it.writeBytes(Random.nextBytes(1024 * 1024))
        }
        val file2 = createTempFile(testBranchDirectory).also {
            it.writeBytes(Random.nextBytes(1024 * 1024))
        }
        val dbFiles = dbFiles(testBranchDirectory.name, file1, file2)

        Mockito.doReturn(dbFiles).`when`(fileService).findAllByIds(any())
        Mockito.doNothing().`when`(overviewService).updateHistory(any(), any())

        val resultFiles = mapOf(
            1 to createTempFile(TEMP_DIRECTORY),
            2 to createTempFile(TEMP_DIRECTORY)
        )
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, object : SimpleChannelInboundHandler<Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
                when (msg) {
                    is UploadFileMessage -> {
                        RandomAccessFile(resultFiles[msg.fileId]!!.toFile(), "rw").use {
                            it.seek(msg.position)
                            it.channel.write(msg.data)
                        }
                    }
                }
            }
        }).channel
        val sendMessagePromise = channel.writeAndFlush(RequestGameFilesMessage(UUID.randomUUID(), listOf(1, 2)))

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            sendMessagePromise.isDone
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            channel.closeFuture().isDone
        }

        Assertions.assertThat(file1.md5().contentEquals(resultFiles[1]!!.md5())).isTrue
        Assertions.assertThat(file2.md5().contentEquals(resultFiles[2]!!.md5())).isTrue
    }

    @Test
    fun `download launcher successfully`() {
        val launcherDirectory = Files.createDirectories(TEMP_DIRECTORY.resolve(SystemType.WINDOWS.name))
        val file = createTempFile(launcherDirectory).also {
            it.writeBytes(Random.nextBytes(1024 * 1024))
        }
        val launcher = Launcher(
            id = 1,
            system = SystemType.WINDOWS,
            version = "1.0.0",
            md5 = file.md5(),
            fileName = file.name,
            size = file.fileSize()
        )

        Mockito.doReturn(listOf(launcher)).`when`(launcherService).findAll()

        val resultFile = createTempFile(TEMP_DIRECTORY)
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, object : SimpleChannelInboundHandler<Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
                when (msg) {
                    is UploadFileMessage -> {
                        RandomAccessFile(resultFile.toFile(), "rw").use {
                            it.seek(msg.position)
                            it.channel.write(msg.data)
                        }
                    }
                }
            }
        }).channel
        val sendMessagePromise = channel.writeAndFlush(RequestLauncherFilesMessage(systemType = SystemType.WINDOWS))

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            sendMessagePromise.isDone
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            channel.closeFuture().isDone
        }

        Assertions.assertThat(file.md5().contentEquals(resultFile.md5())).isTrue
    }

    @Test
    fun `timeout without correct message from client`() {
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, object : SimpleChannelInboundHandler<Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
            }
        }).channel

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            channel.closeFuture().isDone
        }
    }

    @Test
    fun `maintenance exception for download game files`() {
        maintenanceService.setUp(MaintenanceService.Type.GAME)
        val mock = spy<ChannelInboundHandler>()
        val argumentCaptor = argumentCaptor<Message>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, mock).channel
        channel.writeAndFlush(RequestGameFilesMessage(UUID.randomUUID(), listOf(1)))
        verify(mock, timeout(5000).atLeastOnce()).channelRead(any(), argumentCaptor.capture())
        Assertions.assertThat(argumentCaptor.firstValue).isInstanceOf(ServerMaintenanceMessage::class.java)
    }

    @Test
    fun `maintenance launcher not valid for download game files`() {
        maintenanceService.setUp(MaintenanceService.Type.LAUNCHER)
        val mock = spy<ChannelInboundHandler>()
        val argumentCaptor = argumentCaptor<Message>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, mock).channel
        channel.writeAndFlush(RequestGameFilesMessage(UUID.randomUUID(), listOf(1)))
        verify(mock, timeout(5000).atLeastOnce()).channelRead(any(), argumentCaptor.capture())
        Assertions.assertThat(argumentCaptor.firstValue).isInstanceOf(EndSessionMessage::class.java)
    }

    @Test
    fun `maintenance exception for download launcher files`() {
        maintenanceService.setUp(MaintenanceService.Type.LAUNCHER)
        val mock = spy<ChannelInboundHandler>()
        val argumentCaptor = argumentCaptor<Message>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, mock).channel
        channel.writeAndFlush(RequestLauncherFilesMessage(systemType = SystemType.WINDOWS))
        verify(mock, timeout(5000).atLeastOnce()).channelRead(any(), argumentCaptor.capture())
        Assertions.assertThat(argumentCaptor.firstValue).isInstanceOf(ServerMaintenanceMessage::class.java)
    }

    @Test
    fun `maintenance game not valid for download launcher files`() {
        maintenanceService.setUp(MaintenanceService.Type.GAME)
        val mock = spy<ChannelInboundHandler>()
        val argumentCaptor = argumentCaptor<Message>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, mock).channel
        channel.writeAndFlush(RequestLauncherFilesMessage(systemType = SystemType.WINDOWS))
        verify(mock, timeout(5000).atLeastOnce()).channelRead(any(), argumentCaptor.capture())
        Assertions.assertThat(argumentCaptor.firstValue).isInstanceOf(EndSessionMessage::class.java)
    }

    @Test
    fun `server send exception and close session after error`() {
        Mockito.doThrow(RuntimeException::class.java).`when`(launcherService).findAll()
        val mock = spy<ChannelInboundHandler>()
        val argumentCaptor = argumentCaptor<Message>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, mock).channel
        channel.writeAndFlush(RequestLauncherFilesMessage(systemType = SystemType.WINDOWS))
        verify(mock, timeout(5000).atLeastOnce()).channelRead(any(), argumentCaptor.capture())
        Assertions.assertThat(argumentCaptor.firstValue).isInstanceOf(ServerExceptionMessage::class.java)
        Assertions.assertThat(channel.isOpen).isFalse
    }

    @Test
    fun `unsupported message type exception`() {
        val mock = spy<ChannelInboundHandler>()
        val captor = argumentCaptor<Message>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, mock).channel
        channel.writeAndFlush(EndSessionMessage()).sync()
        verify(mock, timeout(5000).atLeastOnce()).channelRead(any(), captor.capture())
        Assertions.assertThat(captor.firstValue).isInstanceOf(ServerExceptionMessage::class.java)
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
            channel.closeFuture().isDone
        }
    }

    @Test
    fun `pause work correctly for download game files`() {
        val testBranchDirectory = createTempDirectory(TEMP_DIRECTORY)
        val file1 = createTempFile(testBranchDirectory).also {
            it.writeBytes(Random.nextBytes(1024 * 1024 * 10))
        }
        val file2 = createTempFile(testBranchDirectory).also {
            it.writeBytes(Random.nextBytes(1024 * 1024 * 10))
        }
        val dbFiles = dbFiles(testBranchDirectory.name, file1, file2)

        Mockito.doReturn(dbFiles).`when`(fileService).findAllByIds(any())
        Mockito.doNothing().`when`(overviewService).updateHistory(any(), any())

        val resultFiles = mapOf(
            1 to createTempFile(TEMP_DIRECTORY),
            2 to createTempFile(TEMP_DIRECTORY)
        )
        val mock = mock<(Any) -> Unit>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, object : SimpleChannelInboundHandler<Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
                mock(msg)
                when (msg) {
                    is UploadFileMessage -> {
                        RandomAccessFile(resultFiles[msg.fileId]!!.toFile(), "rw").use {
                            it.seek(msg.position)
                            it.channel.write(msg.data)
                        }
                    }
                }
            }
        }).channel
        log.info("Netty client connected")

        channel.writeAndFlush(RequestGameFilesMessage(UUID.randomUUID(), listOf(1, 2)))
        log.info("File request sent")

        Mockito.verify(mock, Mockito.timeout(1000).atLeastOnce()).invoke(any())
        log.info("First package received")

        channel.writeAndFlush(RequestChangeState(false)).sync()
        log.info("Pause request sent")

        log.info("Waiting until packages will over")
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
            kotlin.runCatching { verifyNoMoreInteractions(mock); true }.getOrElse { false }.also {
                reset(mock)
            }
        }

        log.info("Check that files are not fully downloaded")
        Assertions.assertThat(
            file1.md5().contentEquals(resultFiles[1]!!.md5()).not()
                    || file2.md5().contentEquals(resultFiles[2]!!.md5()).not()
        ).isTrue

        log.info("5 second wait without receive packages")
        Awaitility.await().during(5, TimeUnit.SECONDS).until {
            kotlin.runCatching { verifyNoMoreInteractions(mock); true }.getOrElse { false }.also {
                reset(mock)
            }
        }

        channel.writeAndFlush(RequestChangeState(true)).sync()
        log.info("Continue request sent")

        log.info("Wait until connection will close")
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            channel.closeFuture().isDone
        }

        log.info("Check files for fully download")
        Assertions.assertThat(file1.md5().contentEquals(resultFiles[1]!!.md5())).isTrue
        Assertions.assertThat(file2.md5().contentEquals(resultFiles[2]!!.md5())).isTrue
    }

    @Test
    fun `pause work correctly for download launcher files`() {
        val launcherDirectory = Files.createDirectories(TEMP_DIRECTORY.resolve(SystemType.WINDOWS.name))
        val file = createTempFile(launcherDirectory).also {
            it.writeBytes(Random.nextBytes(1024 * 1024 * 10))
        }
        val launcher = Launcher(
            id = 1,
            system = SystemType.WINDOWS,
            version = "1.0.0",
            md5 = file.md5(),
            fileName = file.name,
            size = file.fileSize()
        )

        Mockito.doReturn(listOf(launcher)).`when`(launcherService).findAll()

        val resultFile = createTempFile(TEMP_DIRECTORY)
        val mock = mock<(Any) -> Unit>()
        val channel = NettyClient(nettyPort, WORK_GROUP, factory, object : SimpleChannelInboundHandler<Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
                mock(msg)
                when (msg) {
                    is UploadFileMessage -> {
                        RandomAccessFile(resultFile.toFile(), "rw").use {
                            it.seek(msg.position)
                            it.channel.write(msg.data)
                        }
                    }
                }
            }
        }).channel
        log.info("Netty client connected")

        channel.writeAndFlush(RequestLauncherFilesMessage(systemType = SystemType.WINDOWS))
        log.info("File request sent")

        Mockito.verify(mock, Mockito.timeout(1000).atLeastOnce()).invoke(any())
        log.info("First package received")

        channel.writeAndFlush(RequestChangeState(false)).sync()
        log.info("Pause request sent")

        log.info("Waiting until packages will over")
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
            kotlin.runCatching { verifyNoMoreInteractions(mock); true }.getOrElse { false }.also {
                reset(mock)
            }
        }

        log.info("Check that file isn't fully downloaded")
        Assertions.assertThat(file.md5().contentEquals(resultFile.md5())).isFalse

        log.info("5 second wait without receive packages")
        Awaitility.await().during(5, TimeUnit.SECONDS).until {
            kotlin.runCatching { verifyNoMoreInteractions(mock); true }.getOrElse { false }.also {
                reset(mock)
            }
        }

        channel.writeAndFlush(RequestChangeState(true)).sync()
        log.info("Continue request sent")

        log.info("Wait until connection will close")
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            channel.closeFuture().isDone
        }

        log.info("Check file for fully download")
        Assertions.assertThat(file.md5().contentEquals(resultFile.md5())).isTrue
    }

    private fun dbFiles(branchName: String, file1: Path, file2: Path) = Build(
        id = 1,
        name = "testBuild",
        branch = branchName,
        default = true
    ).let { build ->
        Category(
            id = 1,
            type = ContentType.MAIN,
            required = true,
            build = build
        ).let { category ->
            Item(
                id = 1,
                name = "testItem",
                category = category
            ).let { item ->
                listOf(
                    File(
                        id = 1,
                        path = file1.name,
                        active = true,
                        md5 = file1.md5(),
                        lastChangeDate = LocalDateTime.now(),
                        file1.fileSize(),
                        item = item
                    ),
                    File(
                        id = 2,
                        path = file2.name,
                        active = true,
                        md5 = file2.md5(),
                        lastChangeDate = LocalDateTime.now(),
                        file2.fileSize(),
                        item = item
                    )
                )
            }
        }
    }

    companion object {
        private val WORK_GROUP = NioEventLoopGroup(1)
        private val TEMP_DIRECTORY = createTempDirectory()

        @JvmStatic
        @BeforeAll
        fun initNettySettings() {
            System.setProperty("NettyServerTest.port", "${ServerSocket(0).localPort}")
            System.setProperty("NettyServerTest.serverDirectory", TEMP_DIRECTORY.absolutePathString())
        }

        @JvmStatic
        @AfterAll
        fun setDown() {
            WORK_GROUP.shutdownGracefully()
        }

        private val log by Logger()
    }
}