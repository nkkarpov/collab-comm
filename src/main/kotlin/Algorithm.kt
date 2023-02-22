import java.lang.Math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

fun batchedSAR(env: Environment, m: Int, K: Int, budget: Int): List<Int> {
    val n = emptyList<Int>().toMutableList()
    val Q = mutableListOf<Int>()
    var I = (0 until env.n).toMutableList()
    val est = DoubleArray(env.n) { 0.0 }
    val cnt = DoubleArray(env.n) { 0.0 }
    val delta = DoubleArray(env.n) { 0.0 }
    fun mean(arm: Int) = est[arm] / cnt[arm] + 1e-9 * arm
    var t = env.n
    while (t > 1) {
        n.add(t)
        t /= 2
    }
    n.add(0)
    var time = 0
    var cntWords = 0
    val T = compute2(n, budget * K)
    for (r in 0 until T.size) {
        for (arm in I) {
            repeat(T[r]) {
                est[arm] += env.pull(arm)
                cnt[arm] += 1.0
            }
        }
        time += ceil(1.0 * I.size * (T[r]) / K).toInt()
        cntWords += 2 * (K * I.size)
        I.sortBy { -mean(it) }
        val m0 = m - Q.size
        val a = mean(I[m0])
        val b = mean(I[m0 + 1])
        for (arm in I) delta[arm] = max(a - mean(arm), mean(arm) - b)
        I.sortBy { delta[it] }
        Q.addAll(I.drop(n[r + 1]).filter { mean(it) > b })
        I = I.take(n[r + 1]).toMutableList()
    }
//    println(Q)
    println("B,$K,$budget,${if (Q.sorted() == (0..m).toList()) 1 else 0},$cntWords")
//    println("time = $time")
//    assert(time <= budget)
    return Q
}

fun SAR(env: Environment, m: Int, K: Int, budget: Int): List<Int> {
    val coef = 0.5 + (2..env.n).sumOf { 1.0 / it }
    val T =
        (0 until env.n).map { if (it == 0) 0.toInt() else floor(1.0 / coef * (budget * K - env.n) / (env.n + 1 - it)).toInt() }
    val Q = mutableListOf<Int>()
    val I = (0 until env.n).toMutableList()
    val est = DoubleArray(env.n) { 0.0 }
    val cnt = DoubleArray(env.n) { 0.0 }
    val delta = DoubleArray(env.n) { 0.0 }
    fun mean(arm: Int) = est[arm] / cnt[arm]
    for (r in 1 until env.n) {
        for (arm in I) {
            repeat(T[r] - T[r - 1]) {
                est[arm] += env.pull(arm)
                cnt[arm] += 1.0
            }
        }
        I.sortBy { -mean(it) }
        val m0 = m - Q.size
        val a = mean(I[m0])
        val b = mean(I[m0 + 1])
        for (arm in I) delta[arm] = max(a - mean(arm), mean(arm) - b)
        I.sortBy { delta[it] }
        if (mean(I.last()) > b) Q.add(I.last())
        I.removeLast()
    }
    val m0 = m - Q.size
    Q.addAll(I.take(m0 + 1))
    if (K > 1) {
        println("SK,$K,$budget,${if (Q.sorted() == (0..m).toList()) 1 else 0},0")
    } else {
        println("S1,$K,$budget,${if (Q.sorted() == (0..m).toList()) 1 else 0},0")
    }
    return Q.sorted()
}

fun compute1(n: List<Int>, budget : Int): List<Int> {
    val T = IntArray(n.size - 1) { 0 }
    var left = 0.toDouble()
    var right = budget.toDouble()
    repeat(60) {
        val ave = (left + right) / 2
        for (i in T.indices) {
            T[i] = (ave  / n[i]).toInt()
        }
        if (T.indices.sumOf { 2 * T[it] * n[it] } <= budget) {
            left = ave
        } else {
            right = ave
        }
    }
    for (i in T.indices) {
        T[i] = (left  / n[i]).toInt()
    }
    return T.toList()
}

fun compute2(n: List<Int>, budget : Int): List<Int> {
    val T = IntArray(n.size - 1) { 0 }
    var left = 0.toDouble()
    var right = budget.toDouble()
    repeat(60) {
        val ave = (left + right) / 2
        for (i in T.indices) {
            T[i] = (ave  / n[i]).toInt()
        }
        if (T.indices.sumOf { T[it] * n[it] } <= budget) {
            left = ave
        } else {
            right = ave
        }
    }
    for (i in T.indices) {
        T[i] = (left  / n[i]).toInt()
    }
    return T.toList()
}

fun collabTop(env: Environment, m: Int, seed: Int, K: Int, budget: Int): List<Int> {
    val Q = mutableListOf<Int>()
    val n = emptyList<Int>().toMutableList()
    val est = DoubleArray(env.n) { 0.0 }
    val cnt = DoubleArray(env.n) { 0.0 }
    val delta = DoubleArray(env.n) { 0.0 }
    fun mean(arm: Int) = est[arm] / cnt[arm] + 1e-9 * arm

    var t = env.n
    while (t > 1) {
        n.add(t)
        t /= 2
    }
    n.add(0)
    val T = compute1(n, budget * K)
//    println(T)
    var s = 0
    for (r in T.indices) {
        s += n[r] * (T[r])
    }
//    println("KT = ${budget * K}")
//    println("s = $s")
    val rnd = Random(seed)
    val I = Array(K) { emptyList<Int>().toMutableList() }
    for (i in 0 until env.n) {
        I[rnd.nextInt(K)].add(i)
    }
    var cntWords = 0
    var time = 0
    var r = 0

    while (n[r + 1] != 2 && m - Q.size >= 0) {
        cntWords += K
        val maxSize = I.maxOf { it.size }
        val minSize = I.minOf { it.size }
        if (maxSize > 2 * minSize) {
            break
        }
        time += maxSize * (T[r])
        for (k in I.indices) {
            for (arm in I[k]) {
                repeat(T[r]) {
                    est[arm] += env.pull(arm)
                    cnt[arm] += 1.0
                }
            }
        }

        val result0 = order(m - Q.size, I, (0 until env.n).map { -mean(it) }.toDoubleArray(), K)
        val result1 = order(m + 1 - Q.size, I, (0 until env.n).map { -mean(it) }.toDoubleArray(), K)
        cntWords += result0.second
        cntWords += result1.second
//        println("${result0.first} ${result1.first}")
//        println("${mean(result0.first)} ${mean(result1.first)}")
//        if (result0.first == result1.first) {

//            println((0 until env.n).map { mean(it) }.sorted())
//        }
//        assert(result0.first != result1.first)
//        assert(mean(result0.first) > mean(result1.first))
//        println((0 until env.n).sortedBy { mean(it) }.map { mean(it) })
        for (i in 0 until env.n) delta[i] = max(mean(result0.first) - mean(i), mean(i) - mean(result1.first))
        val result2 = order(n[r + 1], I, delta, K)
        cntWords += result2.second
//        println("result = ${result2.first}")
//        println("split ${delta[result2.first]}")
//        println(delta.toList())
        for (k in I.indices) {
            Q.addAll(I[k].filter {
                cmp(Pair(delta[it], it), Pair(delta[result2.first], result2.first)) >= 0 && cmp(
                    Pair(
                        -mean(it),
                        it
                    ), Pair(-mean(result0.first), result0.first)
                ) == -1
            })
            I[k] = I[k].filter { cmp(Pair(delta[it], it), Pair(delta[result2.first], result2.first)) == -1 }
                .toMutableList()
        }
        r++
    }
    var Ig = I.flatMap { it }.toMutableList()
    cntWords += Q.size + Ig.size
    while (Ig.size > 0 && m >= Q.size && m + 1 < Ig.size + Q.size) {
        cntWords += 2 * (Ig.size + K)
        time += ceil(1.0 * Ig.size * (2 * T[r]) / K).toInt()
        for (arm in Ig) {
            repeat(2 * T[r]) {
                est[arm] += env.pull(arm)
                cnt[arm] += 1.0
            }
        }
        Ig.sortBy { -mean(it) }
        val m0 = m - Q.size
        val a = mean(Ig[m0])
        val b = mean(Ig[m0 + 1])
        for (arm in Ig) delta[arm] = max(a - mean(arm), mean(arm) - b)

        Ig.sortBy { delta[it] }
        Q.addAll(Ig.drop(n[r + 1]).filter { mean(it) > b })
        Ig = Ig.take(n[r + 1]).toMutableList()
        r++
    }
    if (Q.size < m) Q.addAll(Ig)
    println("C,$K,$budget,${if (Q.sorted() == (0..m).toList()) 1 else 0},$cntWords")
//    println("time = $time")
    assert(time <= budget)
    return Q.sorted()
}

fun cmp(a: Pair<Double, Int>, b: Pair<Double, Int>) = when {
    a.first < b.first -> -1
    a.first > b.first -> 1
    a.second < b.first -> -1
    a.second > b.second -> 1
    else -> 0
}

private fun order(
    m0: Int,
    I: Array<MutableList<Int>>,
    means: DoubleArray,
    K: Int
): Pair<Int, Int> {
    val left = IntArray(K) { 0 }
    val right = IntArray(K) { I[it].size }
    var cntWords = 0
    var result = -1
    var m: Int
    for (k in I.indices) {
        I[k].sortBy { means[it] }
    }
    while (I.indices.any { right[it] - left[it] > 1 }) {
        cntWords += K
        val arr = I.indices.map { i -> I[i][(left[i] + right[i]) / 2] }.map { Pair(means[it], it) }.withIndex()
            .toMutableList()
            .sortedWith { a, b ->
                when {
                    a.value.first < b.value.first -> -1
                    a.value.first > b.value.first -> 1
                    a.value.second < b.value.first -> -1
                    a.value.second > b.value.second -> 1
                    else -> 0
                }
            }
        var tmp = 0
        m = m0 - left.sum()
        for (k in arr.indices) {
            val ss = (left[arr[k].index] + right[arr[k].index]) / 2
            val size = ss - left[arr[k].index]
            if (tmp + size < m) {
                left[arr[k].index] = ss
            } else {
                right[arr[k].index] = ss
            }
            tmp += size
        }
    }
    cntWords += K
    val arr =
        I.indices.map { i -> I[i][(left[i] + right[i]) / 2] }.map { Pair(means[it], it) }.withIndex().toMutableList()
            .sortedWith { a, b ->
                when {
                    a.value.first < b.value.first -> -1
                    a.value.first > b.value.first -> 1
                    a.value.second < b.value.first -> -1
                    a.value.second > b.value.second -> 1
                    else -> 0
                }
            }
    var tmp = 0
    var cnt = m0 - left.sum()
    for (k in arr.indices) {
        val ss = left[arr[k].index]
        if (cnt == 0) {
            result = arr[k].value.second
            break
        }
        cnt--
        tmp += ss
    }
    assert(result != -1)
    return Pair(result, cntWords)
}