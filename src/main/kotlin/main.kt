import java.io.File

fun main(args: Array<String>) {
    if (args[0] == "iid") {
        val f = File("movie.txt")
        val ds = f.readLines().map { it.toDouble() }.sorted().reversed()
        val env = Environment(ds.toDoubleArray(), 239)
        val T = args[1].toInt()
        val K = args[2].toInt()
        val m = args[3].toInt()
        for (seed in 0 until 100) {
            collabTop(env, m - 1, 239, K, T)
            SAR(env, m - 1, K, T)
            SAR(env, m - 1, 1, T)
            batchedSAR(env, m - 1, K, T)
        }
    }
    if (args[0] == "non") {
        val f = File("movienon.txt")
        val ds = f.readLines().map { it.split(' ').map { it.toDouble() }.toDoubleArray() }.sortedBy { -it.sum() }
            .toTypedArray()
        val T = args[1].toInt()
        val m = args[3].toInt()
        repeat(200) {
            Uniform(DistributedEnvironment(ds, it * 239), m - 1, 10, T)
            top(DistributedEnvironment(ds, it * 239), m - 1, 10, T)
        }
    }
}