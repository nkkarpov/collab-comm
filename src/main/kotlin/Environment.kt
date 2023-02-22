import kotlin.random.Random

class Environment(val means: DoubleArray, seed: Int) {
    private val rnd = Random(seed)
    val n = means.size
    fun pull(arm: Int) = if (rnd.nextDouble() < means[arm]) 1.0 else 0.0
}