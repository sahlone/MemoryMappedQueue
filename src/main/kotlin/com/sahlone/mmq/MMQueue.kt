package com.sahlone.mmq

import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.logging.createContextualLogger
import com.sahlone.mmq.models.EnqDeqResult
import com.sahlone.mmq.models.EnqDeqResult.Companion.Failure.Companion.QueueFault
import com.sahlone.mmq.models.EnqDeqResult.Companion.Failure.Companion.QueueFull
import com.sahlone.mmq.models.EnqDeqResult.Companion.Success
import com.sahlone.mmq.models.EnqDeqResultType
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode.READ_WRITE
import java.nio.file.Files
import java.nio.file.Paths

class MMQueue constructor(private val appConfig: AppConfig) {

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
        return totalMessageSize > freeSpace
    }

    @Throws(IOException::class)
    fun enqueue(tracingContext: TracingContext, data: ByteArray): EnqDeqResult =
        if (isFull(data.size)) {
            logger.debug(tracingContext, { "data_size" to data.size }) { "Queue is full to insert new data" }
            QueueFull(EnqDeqResultType.ENQUEUE)
        }
        // we should let enqueue data that will make
        // head == tail as that is condition for empty we will not detect if queue is full
        else if (((tail() + AppConfig.MESSAGE_META_SIZE + data.size) % appConfig.maxQueueSizeBytes) == head()) {
            logger.debug(tracingContext, { "data_size" to data.size }) { "Queue is full to insert new data" }
            QueueFault(EnqDeqResultType.ENQUEUE)
        } else {
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

    @Throws(IOException::class)
    fun dequeue(tracingContext: TracingContext): EnqDeqResult {
        if (isEmpty()) {
            logger.debug(tracingContext) { "Queue is empty to retrieve new data" }
            return EnqDeqResult.Companion.Failure.Companion.QueueEmpty(EnqDeqResultType.DEQUEUE)
        }
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
                    acc + dataBuffer.get((head + AppConfig.MESSAGE_META_SIZE + index) % appConfig.maxQueueSizeBytes)
                }
            } else {
                val messageByteArray = ByteArray(messageSize)
                dataBuffer.get(messageByteArray)
                messageByteArray
            }
        head((head + AppConfig.MESSAGE_META_SIZE + messageSize) % appConfig.maxQueueSizeBytes)
        return Success(EnqDeqResultType.DEQUEUE, data)
    }

    @Throws(IOException::class)
    fun shutdown() {
        dataFile.close()
        metaFile.close()
        dataBuffer.clear()
        metaBuffer.clear()
    }

    companion object {
        const val TAIL_BYTE_LOC = 4
        const val HEAD_BYTE_LOC = 0
    }
}
