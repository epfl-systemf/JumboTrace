package com.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry;

import java.util.Objects;

public interface BalancingResult {

    boolean isSuccess();

    final class Success implements BalancingResult {
        private final BalancedEquation equation;

        public Success(BalancedEquation equation) {
            this.equation = equation;
        }

        public BalancedEquation getEquation() {
            return equation;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Success success = (Success) o;
            return Objects.equals(getEquation(), success.getEquation());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getEquation());
        }

        @Override
        public String toString() {
            return equation.toString();
        }
    }

    final class Failure implements BalancingResult {
        private final String message;

        public Failure(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Failure failure = (Failure) o;
            return Objects.equals(getMessage(), failure.getMessage());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getMessage());
        }

        @Override
        public String toString() {
            return message;
        }
    }

}
