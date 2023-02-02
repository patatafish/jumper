/**
 * physics holds items we use to calculate movement changes.
 * gravity is a constant that we expect to be added to vertical movement (inertia.rise)
 * windResist is a constant that we expect to be multiplied against horizontal movement (inertia.run)
 * friction is a constant that we expect to be subtracted from horizontal movement when on the ground (inertia.run)
 */
data class Physics(
    val gravity: Double = -0.8,
    val windResist: Double = 0.99,
    val friction: Double = 1.0,
    val terminalVelocity: Double = -100.0,
)
