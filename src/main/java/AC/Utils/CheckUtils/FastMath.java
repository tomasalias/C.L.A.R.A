package AC.Utils.CheckUtils;

import lombok.experimental.UtilityClass;
import java.util.List;

/**
 * FastMath is a utility class for optimized mathematical operations.
 * This class provides reusable and efficient methods for common mathematical
 * calculations, such as vector operations and angle normalization.
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
        // Normalize using a single modulo operation and addition
        return (angle % 360 + 360) % 360;
    }

    public double getAverage(List<Long> pings) {
        if (pings.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (long ping : pings) {
            sum += ping;
        }
        return (double) sum / pings.size(); // Calculate average
    }
}


