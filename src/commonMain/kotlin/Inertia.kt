/**
 * inertia holds the global values for the horizontal and vertical movement
 * of the avatar. these are VAR because we expect them to change through interaction
 * with the physics object and game objects as we encounter them
 */
data class Inertia(
    var rise: Double = 0.0,
    var run: Double = 0.0,
)
