package com.expedia.www.haystack.commons.secretDetector;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
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

    /**
     * Reads a line from S3 and transforms it to an appropriate subclass of WhiteListItemBase
     *
     * @param reader the reader
     * @return a non-null WhiteListItemBase if the read was successful, else null (which indicates all lines have been read)
     * @throws IOException                        if a problem occurs reading from S3
     * @throws InvalidWhitelistItemInputException if an input line in the S3 file is not formatted properly
     */
    WhiteListItemBase readSingleWhiteListItemFromS3(BufferedReader reader)
            throws IOException, InvalidWhitelistItemInputException {
        final String line = reader.readLine();
        if (line == null) {
            return null;
        }
        final String[] strings = line.split(";");
        if (strings.length >= itemCount) {
            return factory.createWhiteListItem(strings);
        }
        throw new InvalidWhitelistItemInputException(line);
    }


    abstract static class Factory<T extends WhiteListItemBase> {
        long createCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        InputStreamReader createInputStreamReader(InputStream inputStream) {
            return new InputStreamReader(inputStream);
        }

        BufferedReader createBufferedReader(InputStreamReader inputStreamReader) {
            return new BufferedReader(inputStreamReader);
        }

        abstract T createWhiteListItem(String... items);
    }

    static class InvalidWhitelistItemInputException extends Exception {
        InvalidWhitelistItemInputException(String line) {
            super(String.format(SpanS3ConfigFetcher.INVALID_DATA_MSG, line));
        }
    }
}
