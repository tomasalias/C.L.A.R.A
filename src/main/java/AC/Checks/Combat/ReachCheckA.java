package AC.Checks.Combat;

public class ReachCheckA {
}

/**
 * ReachCheckA calculates precise reach distances by reconstructing the spatial context of combat interactions.
 *
 * It leverages historical player position data stored in each player's PlayerData instance.
 * To account for network latency, we use a method like getPositionAtTime to retrieve the victim's position
 * as perceived by the attacker—offset by:
 * - The victim's ping (delay in sending their position to the server)
 * - The attacker's ping (delay in receiving that position from the server)
 *
 * In total, the victim's perceived position is sampled from (attackerPing + victimPing) milliseconds ago.
 * The attacker's own position is sampled from attackerPing milliseconds ago.
 *
 * With these temporally accurate positions, we raycast from the attacker's eye-level hitbox center,
 * adjusting for crouch state, yaw, and pitch to determine the attack direction.
 *
 * If the ray intersects the victim's hitbox, we compute the point of closest intersection along the ray—
 * effectively measuring the reach distance as if using a ruler.
 *
 * Supported features:
 * - Configurable hitbox inflation for tolerance
 * - Pose-aware ray origin (e.g., crouching, swimming)
 * - Debug logging for ray origin, direction, and intersection point
 * - Future extensibility for reach modifiers (e.g., knockback, sprinting, potion effects)
 */