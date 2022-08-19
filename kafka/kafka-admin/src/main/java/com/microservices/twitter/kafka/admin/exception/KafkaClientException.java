package com.microservices.twitter.kafka.admin.exception;


/**
 * Exception class for kafka client error situations.
 */
public class KafkaClientException extends RuntimeException {

    public KafkaClientException() {
        super();
    }

    public KafkaClientException(String message) {
        super(message);
    }

    public KafkaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
