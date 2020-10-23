package com.sahlone.mmq.models

sealed class EnqDeqResult(open val resultType: EnqDeqResultType) {
    companion object {
        public data class Success(override val resultType: EnqDeqResultType, val data: ByteArray) :
            EnqDeqResult(resultType) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Success
                if (!data.contentEquals(other.data)) return false
                return true
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }

        public sealed class Failure(override val resultType: EnqDeqResultType) : EnqDeqResult(resultType) {
            companion object {
                public data class QueueEmpty(override val resultType: EnqDeqResultType) : Failure(resultType)
                public data class QueueFull(override val resultType: EnqDeqResultType) : Failure(resultType)
                public data class QueueFault(override val resultType: EnqDeqResultType) : Failure(resultType)
                public data class ProcessingException(override val resultType: EnqDeqResultType) : Failure(resultType)
            }
        }
    }
}

enum class EnqDeqResultType {
    ENQUEUE,
    DEQUEUE
}

enum class Command(val validatorRegex: Regex, val commandLength: Int) {
    GET("GET\\s[1-9]+".toRegex(), Command.COMMAND_LENGTH_GET_PUT),
    PUT("PUT\\s(.)+".toRegex(), Command.COMMAND_LENGTH_GET_PUT),
    SHUTDOWN("^SHUTDOWN$".toRegex(), Command.COMMAND_LENGTH_SHUTDOWN);

    companion object {
        const val COMMAND_LENGTH_GET_PUT = 4
        const val COMMAND_LENGTH_SHUTDOWN = 8
        fun validate(input: String): ValidationResult {
            val result = Command.values().toList().firstOrNull {
                it.validatorRegex.matches(input)
            }
            return result?.let {
                ValidationResult.Companion.Valid(it, input.drop(it.commandLength))
            } ?: ValidationResult.Companion.Invalid
        }

        sealed class ValidationResult {
            companion object {
                data class Valid(val command: Command, val data: String) : ValidationResult()
                object Invalid : ValidationResult()
            }
        }
    }
}
