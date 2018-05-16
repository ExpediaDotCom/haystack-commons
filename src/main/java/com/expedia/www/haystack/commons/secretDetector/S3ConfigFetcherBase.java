package com.expedia.www.haystack.commons.secretDetector;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class S3ConfigFetcherBase {
    protected final Logger logger;
    protected final String bucket;
    protected final String key;
    final AmazonS3 amazonS3;
    final Factory factory;
    private final int itemCount;

    S3ConfigFetcherBase(Logger logger, String bucket, String key, AmazonS3 amazonS3, Factory factory, int itemCount) {
        this.logger = logger;
        this.bucket = bucket;
        this.key = key;
        this.amazonS3 = amazonS3;
        this.factory = factory;
        this.itemCount = itemCount;
    }

    BufferedReader getBufferedReader(S3Object s3Object) {
        final InputStream inputStream = s3Object.getObjectContent();
        final InputStreamReader inputStreamReader = factory.createInputStreamReader(inputStream);
        return factory.createBufferedReader(inputStreamReader);
    }

    static class Factory {
        long createCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        InputStreamReader createInputStreamReader(InputStream inputStream) {
            return new InputStreamReader(inputStream);
        }

        BufferedReader createBufferedReader(InputStreamReader inputStreamReader) {
            return new BufferedReader(inputStreamReader);
        }
    }

    static class InvalidWhitelistItemInputException extends Exception {
        InvalidWhitelistItemInputException(String line) {
            super(String.format(SpanS3ConfigFetcher.INVALID_DATA_MSG, line));
        }
    }
}
