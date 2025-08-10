package AC.Checks.Movement.SpeedChecks;

public class SpeedCheckA {

}

// SpeedCheckA monitors player movement by comparing two consecutive positions.
// Each time a new position is received, the previous "new" position becomes the "last",
// and the current one becomes the new reference for comparison.
//
// We calculate movement deltas (ΔX, ΔY, ΔZ) and validate them against configurable thresholds
// to detect suspicious speed behavior.
//
// Vertical movement (ΔY) is treated separately, with distinct thresholds depending on
// whether the player is rising (e.g., jumping) or falling (e.g., dropping from a ledge).
//
// This system should be modular and extensible, allowing future integration of terrain-aware
// adjustments, potion effects, or velocity modifiers.