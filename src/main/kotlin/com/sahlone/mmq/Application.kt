package com.sahlone.mmq

import com.sahlone.mmq.config.ValidatedAppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.logging.createContextualLogger
import com.sahlone.mmq.models.Command
import com.sahlone.mmq.models.Command.Companion.ValidationResult.Companion
import com.sahlone.mmq.models.EnqDeqResult
import com.sahlone.mmq.queue.MMQueue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.charset.Charset
import kotlin.system.exitProcess

object Application {
    private val logger = createContextualLogger()
    private val appConfig = ValidatedAppConfig.invoke()
    private val mmQueue = MMQueue(appConfig)

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            launch {
                processCommand()
            }
        }
    }

    private fun enqueueCommand(tracingContext: TracingContext, command: Companion.Valid) {
        val data = command.data
        println(
            when (mmQueue.enqueue(tracingContext, data.toByteArray(Charset.defaultCharset()))) {
                is EnqDeqResult.Companion.Success -> "Successfully inserted data"
                is EnqDeqResult.Companion.Failure -> ERROR
            }
        )
    }

    private fun dequeueCommand(tracingContext: TracingContext, command: Companion.Valid) {
        val data = command.data
        kotlin.runCatching {
            val noOfLines = data.toInt()
            repeat(noOfLines) {
                val output = when (val result = mmQueue.dequeue(tracingContext)) {
                    is EnqDeqResult.Companion.Success -> result.data
                    is EnqDeqResult.Companion.Failure -> ERROR.toByteArray()
                }
                println(String(output))
            }
        }.getOrElse {
            logger.errorWithCause(tracingContext, it) { "Error processing dequeue command" }
            invalidCommand()
        }
    }

    private fun shutdownApplication(tracingContext: TracingContext) {
        try {
            logger.debug(tracingContext) { "Shutting down queue" }
            mmQueue.shutdown(tracingContext)
            logger.debug(tracingContext) { "Shutting down successful. Exiting the process" }
            exitProcess(0)
        } catch (err: IOException) {
            logger.errorWithCause(tracingContext, err) { "Error while shutting down the application" }
            exitProcess(1)
        }
    }

    private fun invalidCommand() {
        println("Invalid command")
        println(VALID_COMMANDS)
    }

    private fun processCommand() {
        println("Please enter a command to proceed")
        println(VALID_COMMANDS)
        while (true) {
            val tracingContext = TracingContext()
            val input = (readLine() ?: "EMPTY_INPUT").trim()
            when (val result = Command.validate(input)) {
                is Companion.Invalid -> invalidCommand()
                is Companion.Valid -> {
                    when (result.command) {
                        Command.GET -> dequeueCommand(tracingContext, result)
                        Command.PUT -> enqueueCommand(tracingContext, result)
                        Command.SHUTDOWN -> shutdownApplication(tracingContext)
                    }
                }
            }
        }
    }

    private const val ERROR = "ERR\r\n"
    private const val VALID_COMMANDS = """
        PUT text
            Inserts a text line into the FIFO Queue. text is a single line of an arbitrary
            number of case sensitive alphanumeric words ([A-Z]+[a-z]+[0-9]) separated by
            space characters.
        GET n
            Return n lines from the head of the queue and remove them from the queue.
        SHUTDOWN
            Shutdown the application/server.
    """
}
