import kotlin.random.Random

class DistributedEnvironment(private val means: Array<DoubleArray>, seed: Int) {
    private val rnd = Random(seed)
    val n = means.size
    val k = means.first().size
    fun pull(arm: Int, agent: Int) = if (rnd.nextDouble() < means[arm][agent]) 1.0 else 0.0
}