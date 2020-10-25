package com.sahlone.mmq.queue

import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.logging.createContextualLogger
import com.sahlone.mmq.models.EnqDeqResult
import com.sahlone.mmq.models.EnqDeqResult.Companion.Failure.Companion.ProcessingException
import com.sahlone.mmq.models.EnqDeqResult.Companion.Failure.Companion.QueueEmpty
import com.sahlone.mmq.models.EnqDeqResult.Companion.Failure.Companion.QueueFull
import com.sahlone.mmq.models.EnqDeqResult.Companion.Success
import com.sahlone.mmq.models.EnqDeqResultType
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode.READ_WRITE
import java.nio.file.Files
import java.nio.file.Paths

class MMQueue constructor(private val appConfig: AppConfig) : Queue {

    private val logger = createContextualLogger()
    private val fileName = "${appConfig.baseQueueDir}/${appConfig.queueName}"

    init {
        if (!Files.exists(Paths.get(appConfig.baseQueueDir))) {
            Files.createDirectories(Paths.get(appConfig.baseQueueDir))
        }
        val baseDirAbsPath = Paths.get(appConfig.baseQueueDir).toAbsolutePath()
        logger.debug(TracingContext(), { "base_dir_path" to baseDirAbsPath }) {
            "Data directory is present at $baseDirAbsPath"
        }
    }

    private val dataFile = RandomAccessFile("$fileName.data", appConfig.mmFileMode)
    private val metaFile = RandomAccessFile("$fileName.meta", appConfig.mmFileMode)
    private val dataBuffer = dataFile.channel.map(
        READ_WRITE, 0,
        appConfig.maxQueueSizeBytes.toLong()
    )
    private val metaBuffer = metaFile.channel.map(
        READ_WRITE, 0,
        AppConfig.QUEUE_META_SIZE.toLong()
    )

    private fun head(): Int {
        return metaBuffer.getInt(HEAD_BYTE_LOC)
    }

    private fun head(newHead: Int) {
        metaBuffer.putInt(HEAD_BYTE_LOC, newHead)
    }

    private fun tail(): Int {
        return metaBuffer.getInt(TAIL_BYTE_LOC)
    }

    private fun tail(newTail: Int) {
        metaBuffer.putInt(TAIL_BYTE_LOC, newTail)
    }

    private fun isEmpty(): Boolean = head() == tail()

    /*
        we should not let enqueue data that will make
        head == tail as that is condition for empty, then we will not detect if queue is full
     */
    private fun isFull(messageSize: Int): Boolean {
        val head = head()
        val tail = tail()
        val totalMessageSize = messageSize + AppConfig.MESSAGE_META_SIZE
        val dataSize = when {
            head < tail -> {
                tail - head
            }
            head > tail -> {
                tail + (appConfig.maxQueueSizeBytes - head)
            }
            else -> {
                0
            }
        }
        val freeSpace = appConfig.maxQueueSizeBytes - dataSize
        return if ((tail + totalMessageSize) % appConfig.maxQueueSizeBytes == head) {
            true
        } else {
            totalMessageSize > freeSpace
        }
    }

    override fun enqueue(tracingContext: TracingContext, data: ByteArray): EnqDeqResult =
        processingHandler(tracingContext, EnqDeqResultType.ENQUEUE, "Error while en-queueing") {
            when {
                isFull(data.size) -> {
                    logger.debug(tracingContext, { "data_size" to data.size }) { "Queue is full to insert new data" }
                    QueueFull(EnqDeqResultType.ENQUEUE)
                }
                else -> {
                    val tail = tail()
                    dataBuffer.position(tail)
                    if (tail + AppConfig.MESSAGE_META_SIZE + data.size >= appConfig.maxQueueSizeBytes) {
                        val messageSizeData = ByteBuffer.allocate(AppConfig.MESSAGE_META_SIZE).putInt(data.size).array()
                        val allData = messageSizeData + data
                        allData.mapIndexed { index, byte ->
                            dataBuffer.put((tail + index) % appConfig.maxQueueSizeBytes, byte)
                        }
                    } else {
                        dataBuffer.putInt(data.size)
                        dataBuffer.put(data)
                    }
                    tail((tail + AppConfig.MESSAGE_META_SIZE + data.size) % appConfig.maxQueueSizeBytes)
                    Success(EnqDeqResultType.ENQUEUE, data)
                }
            }
        }

    override fun dequeue(tracingContext: TracingContext): EnqDeqResult =
        processingHandler(tracingContext, EnqDeqResultType.DEQUEUE, "Error while de-queueing") {
            if (isEmpty()) {
                logger.debug(tracingContext) { "Queue is empty to retrieve new data" }
                QueueEmpty(EnqDeqResultType.DEQUEUE)
            } else {
                val head = head()
                dataBuffer.position(head)
                val messageSize = if (head + AppConfig.MESSAGE_META_SIZE > appConfig.maxQueueSizeBytes) {
                    val messageSizeBytes = (0 until AppConfig.MESSAGE_META_SIZE).map { index ->
                        dataBuffer.get((head + index) % appConfig.maxQueueSizeBytes)
                    }.toByteArray()
                    ByteBuffer.wrap(messageSizeBytes).int
                } else {
                    dataBuffer.int
                }
                val data: ByteArray =
                    if (head + AppConfig.MESSAGE_META_SIZE + messageSize > appConfig.maxQueueSizeBytes) {
                        (0 until messageSize).fold(byteArrayOf()) { acc, index ->
                            acc + dataBuffer.get(
                                (head + AppConfig.MESSAGE_META_SIZE + index) % appConfig.maxQueueSizeBytes
                            )
                        }
                    } else {
                        val messageByteArray = ByteArray(messageSize)
                        dataBuffer.get(messageByteArray)
                        messageByteArray
                    }
                head((head + AppConfig.MESSAGE_META_SIZE + messageSize) % appConfig.maxQueueSizeBytes)
                Success(EnqDeqResultType.DEQUEUE, data)
            }
        }

    @Throws(IOException::class)
    fun shutdown(tracingContext: TracingContext) {
        try {
            logger.debug(tracingContext) { "Shutting down queue ${appConfig.baseQueueDir}/${appConfig.queueName}" }
            dataFile.close()
            metaFile.close()
            dataBuffer.clear()
            metaBuffer.clear()
            logger.debug(tracingContext) {
                "Successfully shutdown the queue ${appConfig.baseQueueDir}/${appConfig.queueName}"
            }
        } catch (err: IOException) {
            logger.errorWithCause(
                tracingContext,
                err
            ) { "Error while shutting down the queue ${appConfig.baseQueueDir}/${appConfig.queueName}" }
            throw err
        }
    }

    private inline fun processingHandler(
        tracingContext: TracingContext,
        enqDeqResultType: EnqDeqResultType,
        errorMessage: String,
        crossinline block: () -> EnqDeqResult
    ) = kotlin.runCatching {
        block()
    }.getOrElse {
        logger.errorWithCause(
            tracingContext,
            it
        ) { errorMessage }
        ProcessingException(enqDeqResultType, it)
    }

    companion object {
        const val TAIL_BYTE_LOC = 4
        const val HEAD_BYTE_LOC = 0
    }
}
