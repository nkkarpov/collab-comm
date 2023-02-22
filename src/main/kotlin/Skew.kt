import kotlin.math.max

fun Uniform(environment: DistributedEnvironment, m: Int, K: Int, T: Int): List<Int> {
    val cnt = Array(environment.n) { DoubleArray(K) { 0.0 } }
    val est = Array(environment.n) { DoubleArray(K) { 0.0 } }
    for (agent in 0 until K) {
        for (arm in 0 until environment.n) {
            repeat(T / environment.n) {
                est[arm][agent] += environment.pull(arm, agent)
                cnt[arm][agent] += 1.0
            }
        }
    }
    val Q = est.indices.sortedBy { est[it].sum() / cnt[it].sum() }.reversed().take(m + 1)
    println("U,$K,$T,${if (Q.sorted() == (0..m).toList()) 1 else 0},${environment.n * K}")
    return Q
}

fun top(environment: DistributedEnvironment, m: Int, K: Int, B: Int): List<Int> {
    val cnt = Array(environment.n) { DoubleArray(K) { 0.0 } }
    val est = Array(environment.n) { DoubleArray(K) { 0.0 } }
    var cntWords = 0
    val n = emptyList<Int>().toMutableList()
    fun mean(arm: Int, agent: Int) = est[arm][agent] / cnt[arm][agent]

    val Q = mutableListOf<Int>()
    var I = (0 until environment.n).toMutableList()
    var t = environment.n
    while (t > 1) {
        n.add(t)
        t /= 2
    }
    n.add(0)
    fun compute(n: List<Int>, budget: Int): List<Int> {
        val R = IntArray(n.size - 1) { 0 }
        var left = 0.toDouble()
        var right = budget.toDouble()
        repeat(60) {
            val ave = (left + right) / 2
            for (i in R.indices) {
                R[i] = (ave / n[i]).toInt()
            }
            if (R.indices.sumOf { R[it] * n[it] } <= budget) {
                left = ave
            } else {
                right = ave
            }
        }
        for (i in R.indices) {
            R[i] = (left / n[i]).toInt()
        }
        return R.toList()
    }

    val T = compute(n, B)
    val means = DoubleArray(environment.n) { 0.0 }
    val delta = DoubleArray(environment.n) { 0.0 }
    for (r in T.indices) {
        cntWords += 2 * I.size * K
        for (agent in 0 until K) {
            for (arm in 0 until environment.n) {
                repeat(T[r]) {
                    est[arm][agent] += environment.pull(arm, agent)
                    cnt[arm][agent] += 1.0
                }
            }
        }
        for (arm in I) {
            means[arm] = (0 until K).sumOf { mean(arm, it) } / K
        }

        I.sortBy { -means[it] }
//        println(Q)
        val m0 = m - Q.size
        if (m0 < 0) continue
        val a = means[m0]
        val b = means[m0 + 1]
        for (arm in I) delta[arm] = max(a - means[arm], means[arm] - b)
        I.sortBy { delta[it] }
//        println(I)
//        println(I.map { means[it] })
        Q.addAll(I.drop(n[r + 1]).filter { means[it] > b })
        I = I.take(n[r + 1]).toMutableList()
    }
    println("T,$K,$B,${if (Q.sorted() == (0..m).toList()) 1 else 0},$cntWords")
    return Q
}