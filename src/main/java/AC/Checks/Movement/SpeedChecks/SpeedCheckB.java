package AC.Checks.Movement.SpeedChecks;

public class   SpeedCheckB {
}

// /*
// * SpeedCheckB Logic Overview:
// *
// * - Store the last 20 player positions in a ConcurrentHashMap keyed by UUID.
// * - Each position includes: coordinates, timestamp, and optionally a unique ID or outlier flag.
// * - Once 20 positions are recorded, scan for an outlier—defined as a point significantly
// *   farther from adjacent positions compared to the norm.
// * - Do NOT punish for outliers, but keep them in the map and mark them for exclusion
// *   from speed calculations.
// * - Calculate speed between each pair of consecutive positions:
// *     - Skip speed calculations where the destination position is an outlier.
// *     - Example: if position 14 is flagged, skip the segment from 13 to 14.
// *     - However, do include 14 to 15 if 15 is valid.
// * - After computing valid speeds, average them.
// * - Compare the average speed against a configured threshold.
// *     - If it exceeds the threshold, consider flagging or triggering an event.
// */

// Seperate note from Koskolonium if a packet is recieved and the y level has changed
// we should class it as an outlier