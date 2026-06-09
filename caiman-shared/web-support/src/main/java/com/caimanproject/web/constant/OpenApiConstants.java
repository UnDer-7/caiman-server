package com.caimanproject.web.constant;

public class OpenApiConstants {

    private OpenApiConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final class Examples {
        public static final String DATE_TIME = "2025-01-15T14:30:00Z";
        public static final String TIME = "14:30:00";
        public static final String DATE = "2025-01-15";
        public static final String TIMEZONE = "America/Sao_Paulo";

        public static final String ZERO = "0";
        public static final String TEN = "10";

        public static final String FALSE = "false";
        public static final String TRUE = "true";
        public static final String ID = "42";

        private Examples() {
            throw new IllegalStateException("Utility class");
        }
    }

    public static final class HttpStatusCodes {
        public static final String OK = "200";
        public static final String CREATED = "201";
        public static final String NO_CONTENT = "204";
        public static final String BAD_REQUEST = "400";
        public static final String NOT_FOUND = "404";
        public static final String UNPROCESSABLE_ENTITY = "422";
        public static final String INTERNAL_SERVER_ERROR = "500";

        private HttpStatusCodes() {
            throw new IllegalStateException("Utility class");
        }
    }
}
