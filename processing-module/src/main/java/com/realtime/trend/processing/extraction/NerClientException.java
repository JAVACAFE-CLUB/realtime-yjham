package com.realtime.trend.processing.extraction;

/**
 * NER 클라이언트 예외
 */
public class NerClientException extends RuntimeException {

    public NerClientException(String message) {
        super(message);
    }

    public NerClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
