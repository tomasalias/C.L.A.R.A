package AC.Utils.CheckUtils;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import lombok.Getter;
import lombok.Setter;
import java.util.LinkedList;

@Setter
@Getter
public class PlayerData {


    private Long pingTimestamp; // Timestamp of the current ping value

    // A list to store the last 100 positions of the player (newest position at the front)
    private LinkedList<Position> positionHistory = new LinkedList<>();

    // Timestamp to track the last time a position was added to the history
    private long lastUpdatedTimestamp = 0;

    // Lock duration (in milliseconds) between position updates, set to 5ms to throttle position updates
    private final long LOCK_DURATION = 5; // 5ms lock duration to avoid excessive updates

    // List of ping values that will be stored (only keeping the last 50 pings)
    private LinkedList<Long> pingHistory = new LinkedList<>();
    private static final int MAX_SIZE = 12; // Max number of pings to store
    private boolean isLoggingActive = false; // Flag to prevent multiple threads from logging ping
    private double lastAveragePing = 0.0; // Last calculated average ping
    private Thread pingLoggingThread = null;
    // Stores the previous position look packet for comparison
    private WrapperPlayClientPlayerPositionAndRotation previousPositionLookPacket;



    // Method to record the player's ping value
    public synchronized void setPing(long playerPing) {
        pingHistory.addFirst(playerPing); // Add new ping to the front of the list
        if (pingHistory.size() > MAX_SIZE) { // If the list exceeds the max size
            pingHistory.removeLast(); // Remove the oldest ping value to keep the list size constant

            // Trigger the calculation and logging of the average ping
            calculateAndRepeatAverage();
        }
    }

    // Method to continuously log the average ping value every second
    private synchronized void calculateAndRepeatAverage() {
        if (pingLoggingThread != null && pingLoggingThread.isAlive()) {
            return; // Logging is already active for this player
        }
        isLoggingActive = true;
        pingLoggingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Calculate the average ping using the FastMath utility
                    double averagePing = FastMath.getAverage(pingHistory);
                    lastAveragePing = averagePing;
                    Thread.sleep(1000); // Wait for 1 second before recalculating
                } catch (InterruptedException e) {
                    // Allow thread to exit if interrupted
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pingLoggingThread.start();
    }

    // Method to get the current average ping from the last calculated value
    public synchronized double getCurrentPing() {
        // Optionally, you can check if pingHistory is empty,
        // but if you want to return the last computed value regardless, just return lastAveragePing.
        if (pingHistory == null || pingHistory.isEmpty()) {
            return 0.0;
        }
        return lastAveragePing;
    }

    public synchronized void stopPingLogging() {
        if (pingLoggingThread != null && pingLoggingThread.isAlive()) {
            pingLoggingThread.interrupt();
        }
        pingLoggingThread = null;
        isLoggingActive = false;
    }
}