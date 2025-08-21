package AC.Utils.CheckUtils;

import lombok.experimental.UtilityClass;
import java.util.List;

/**
 * FastMath is a utility class for optimized mathematical operations.
 * This class provides reusable and efficient methods for common mathematical
 * calculations, such as vector operations, angle normalization, and adaptive filtering.
 */
@UtilityClass
public class FastMath {

    /**
     * Normalizes an angle (e.g., yaw) to fall within the range [0, 360).
     *
     * @param angle The angle to normalize
     * @return The normalized angle in the range [0, 360)
     */
    public float normalizeAngle(float angle) {
        return (angle % 360 + 360) % 360;
    }

    /**
     * Computes the average of a list of ping values.
     *
     * @param pings List of ping measurements
     * @return The average ping, or 0 if the list is empty
     */
    public double getAverage(List<Long> pings) {
        if (pings.isEmpty()) return 0;
        long sum = 0;
        for (long ping : pings) {
            sum += ping;
        }
        return (double) sum / pings.size();
    }

    /**
     * KalmanFilter is a lightweight adaptive filter for smoothing noisy measurements.
     * Useful for packet timing, ping stabilization, and subtle drift detection.
     */
    public static class KalmanFilter {
        private double estimate;
        private double errorEstimate;
        private final double processNoise;
        private final double measurementNoise;

        public KalmanFilter(double initialEstimate, double initialError,
                            double processNoise, double measurementNoise) {
            this.estimate = initialEstimate;
            this.errorEstimate = initialError;
            this.processNoise = processNoise;
            this.measurementNoise = measurementNoise;
        }

        public double update(double measurement) {
            double kalmanGain = errorEstimate / (errorEstimate + measurementNoise);
            estimate = estimate + kalmanGain * (measurement - estimate);
            errorEstimate = (1 - kalmanGain) * errorEstimate + processNoise;
            return estimate;
        }

        public double getEstimate() {
            return estimate;
        }

        public void reset(double newEstimate, double newError) {
            this.estimate = newEstimate;
            this.errorEstimate = newError;
        }
    }
}