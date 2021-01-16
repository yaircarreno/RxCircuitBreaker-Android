package com.yaircarreno.rxcircuitbreaker.pattern;

public enum CircuitBreakerState {

    CLOSED {
        @Override
        public CircuitBreakerState success() {
            return CLOSED;
        }

        @Override
        public CircuitBreakerState fail() {
            return OPEN;
        }
    },
    OPEN {
        @Override
        public CircuitBreakerState success() {
            return HALF_OPEN;
        }

        @Override
        public CircuitBreakerState fail() {
            return OPEN;
        }
    },
    HALF_OPEN {
        @Override
        public CircuitBreakerState success() {
            return CLOSED;
        }

        @Override
        public CircuitBreakerState fail() {
            return OPEN;
        }
    };

    public abstract CircuitBreakerState success();

    public abstract CircuitBreakerState fail();
}
