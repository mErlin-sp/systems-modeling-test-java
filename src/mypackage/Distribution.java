package mypackage;

import java.util.function.BinaryOperator;

public class Distribution {
    /**
     * A general distribution from which all other distributions will inherit.
     */

    @Override
    public String toString() {
        return "Distribution";
    }

    public double sample(double t, int ind) {
        return 0.0;
    }

    private double _sample(double t, int ind) {
        /**
         * Performs validity checks before sampling.
         */
        double s = sample(t, ind);
        if (s >= 0) {
            return s;
        } else {
            throw new IllegalArgumentException("Invalid time sampled.");
        }
    }

    public CombinedDistribution __add__(Distribution dist) {
        /**
         * Add two distributions such that sampling is the sum of the samples.
         */
        return new CombinedDistribution(this, dist, (a, b) -> a + b);
    }

    public CombinedDistribution __sub__(Distribution dist) {
        /**
         * Subtract two distributions such that sampling is the difference of the samples.
         */
        return new CombinedDistribution(this, dist, (a, b) -> a - b);
    }

    public CombinedDistribution __mul__(Distribution dist) {
        /**
         * Multiply two distributions such that sampling is the product of the samples.
         */
        return new CombinedDistribution(this, dist, (a, b) -> a * b);
    }

    public CombinedDistribution __truediv__(Distribution dist) {
        /**
         * Divide two distributions such that sampling is the ratio of the samples.
         */
        return new CombinedDistribution(this, dist, (a, b) -> a / b);
    }
}

class Exponential extends Distribution {

    /**
     * The Exponential distribution.
     * <p>
     * Takes:
     * - `rate` the rate parameter, lambda
     */

    private final double rate;

    public Exponential(double rate) {
        if (rate <= 0.0) {
            throw new IllegalArgumentException("Exponential distribution must sample positive numbers only.");
        }
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "Exponential: " + rate;
    }

    @Override
    public double sample(double t, int ind) {
        return expovariate(rate);
    }

    public double expovariate(double lambda) {
        return Math.log(1 - Math.random()) / (-lambda);
    }
}

class Deterministic extends Distribution {
    /**
     * The Deterministic distribution.
     * <p>
     * Takes:
     * - `value` the value to return
     */

    private final double value;

    public Deterministic(double value) {
        if (value < 0.0) {
            throw new IllegalArgumentException("Deterministic distribution must sample positive numbers only.");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return "Deterministic: " + value;
    }

    @Override
    public double sample(double t, int ind) {
        return value;
    }
}

class CombinedDistribution extends Distribution {
    /**
     * A distribution that combines the samples of two other distributions, `dist1`
     * and `dist2`, using `operator`.
     */

    private Distribution d1;
    private Distribution d2;
    private BinaryOperator<Double> operator;

    public CombinedDistribution(Distribution dist1, Distribution dist2, BinaryOperator<Double> operator) {
        this.d1 = dist1;
        this.d2 = dist2;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "CombinedDistribution";
    }

    @Override
    public double sample(double t, int ind) {
        double s1 = d1.sample(t, ind);
        double s2 = d2.sample(t, ind);
        return operator.apply(s1, s2);
    }
}
