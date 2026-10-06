package com.Mohammad.Elahi.terpsichore.core.sources

enum class SourceFailureReason {
    NO_NETWORK,
    TIMEOUT,
    RATE_LIMITED,
    SERVER_ERROR,
    BAD_REQUEST,
    NOT_FOUND,
    PARSE_ERROR,
    UNKNOWN,
}

class SourceException(
    val source: Source,
    val reason: SourceFailureReason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
