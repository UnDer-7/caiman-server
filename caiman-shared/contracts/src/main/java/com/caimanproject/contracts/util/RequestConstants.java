package com.caimanproject.contracts.util;

public final class RequestConstants {

    private RequestConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final class Path {
        public static final String ID = "id";

        private Path() {
            throw new IllegalStateException("Utility class");
        }
    }

    public static final class Query {
        public static final String PAGE_SIZE = "pageSize";
        public static final String PAGE_NUMBER = "pageNumber";
        public static final String TIMEZONE = "timezone";

        private Query() {
            throw new IllegalStateException("Utility class");
        }
    }

    public static final class Headers {

        public static final String X_CORRELATION_ID = "X-Correlation-ID";
        public static final String X_CHANNEL = "X-Channel";

        private Headers() {
            throw new IllegalStateException("Utility class");
        }
    }
}
