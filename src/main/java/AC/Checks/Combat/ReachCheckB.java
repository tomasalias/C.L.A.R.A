package AC.Checks.Combat;


/**
 * ReachCheckB validates block-breaking reach by reconstructing the spatial context of the interaction.
 *
 * The attacker is defined as the player attempting to break a block. To ensure fair reach enforcement,
 * we simulate the attacker's perspective at the time they initiated the block break.
 *
 * Temporal reconstruction:
 * - The attacker's position is sampled from attackerPing milliseconds ago, representing their perceived location.
 * - The block is treated as a static target, but its position may be adjusted for server-side desync compensation
 *   or future support for moving blocks (e.g., pistons, falling entities).
 *
 * From the attacker's eye-level hitbox center, we raycast toward the target block, factoring in:
 * - Crouch state, yaw, and pitch for accurate direction
 * - Configurable reach threshold (e.g., 4.25 blocks for vanilla, adjustable for gameplay balance)
 *
 * If the ray intersects the block's bounding box within the allowed reach, the interaction is considered valid.
 * Otherwise, it may be flagged for reach violation.
 *
 * Supported features:
 * - Configurable reach threshold and block inflation tolerance
 * - Pose-aware ray origin (e.g., crouching, swimming)
 * - Debug logging for ray origin, direction, and intersection point
 * - Future extensibility for reach modifiers (e.g., haste effects, sprinting, block-specific offsets)
 */