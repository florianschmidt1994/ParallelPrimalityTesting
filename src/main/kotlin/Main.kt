import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    val limit = Math.pow(10.0, 5.0)
    val numberOfThreads = 10

    var count = 0;
    var time = measureNanoTime { count = countPrimesSingleThreaded(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with single threaded implementation found $count primes")

    time = measureNanoTime { count = countPrimesFixedRange(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with fixed range $count primes")

    time = measureNanoTime { count = countPrimesAtomicGetAndIncrement(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with shared counter found $count primes");

    time = measureNanoTime { count = countPrimesUnsafeSharedCounter(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with unsafe shared counter found $count primes");

    time = measureNanoTime { count = countPrimesSafeSharedCounter(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with safe shared counter found $count primes");
}

fun countPrimesSingleThreaded(limit: Long, numberOfThreads: Int): Int {
    val numberOfPrimes = AtomicInteger();
    for (i in 0..limit) {
        if (isPrime(i)) numberOfPrimes.getAndIncrement()
    }
    return numberOfPrimes.get()
}

fun countPrimesFixedRange(limit: Long, numberOfThreads: Int): Int {

    val threadPool = Executors.newFixedThreadPool(numberOfThreads);
    val numberOfPrimes = AtomicInteger();

    for (i in 0L..9L) {

        val begin = i * (limit / 10) + 1
        val end = (i + 1) * (limit / 10);

        threadPool.submit {
            for (number in begin..end) {
                if (isPrime(number)) {
                    numberOfPrimes.getAndIncrement()
                };
            }
        }
    }
    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS);
    return numberOfPrimes.get()
}

fun countPrimesAtomicGetAndIncrement(limit: Long, numberOfThreads: Int): Int {

    val counter = AtomicLong();
    val numberOfPrimes = AtomicInteger();
    val threadPool = Executors.newFixedThreadPool(numberOfThreads);

    for (i in 0L..9L) {
        threadPool.submit {
            var j = counter.get()
            while (j < limit) {
                j = counter.getAndIncrement()
                if (isPrime(j)) {
                    numberOfPrimes.getAndIncrement()
                }
            }
        }
    }

    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS);
    return numberOfPrimes.get()
}

fun countPrimesUnsafeSharedCounter(limit: Long, numberOfThreads: Int): Int {
    val counter = UnsafeSharedCounter()
    val numberOfPrimes = AtomicInteger();
    val threadPool = Executors.newFixedThreadPool(numberOfThreads);

    for (i in 0L..9L) {
        threadPool.submit {
            var j = counter.get()
            while (j < limit) {
                j = counter.getAndIncrement()
                if (isPrime(j)) {
                    numberOfPrimes.getAndIncrement()
                }
            }
        }
    }

    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS);
    return numberOfPrimes.get()
}

fun countPrimesSafeSharedCounter(limit: Long, numberOfThreads: Int): Int {
    val counter = SafeSharedCounter()
    val numberOfPrimes = AtomicInteger();
    val threadPool = Executors.newFixedThreadPool(numberOfThreads);

    for (i in 0L..9L) {
        threadPool.submit {
            var j = counter.get()
            while (j < limit) {
                j = counter.getAndIncrement()
                if (isPrime(j)) {
                    numberOfPrimes.getAndIncrement()
                }
            }
        }
    }

    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS);
    return numberOfPrimes.get()

}

fun isPrime(number: Long): Boolean {

    if (number == 2L) return true
    else if (number == 3L) return true
    else if (number % 2L == 0L) return false
    else if (number % 3L == 0L) return false

    var i = 5
    var w = 2

    while (i * i <= number) {
        if (number % i == 0L) return false
        i += w
        w = 6 - w
    }

    return true
}

class UnsafeSharedCounter {
    private var value: Long = 0

    fun get(): Long {
        return value
    }

    fun getAndIncrement(): Long {
        return value++
    }
}

class SafeSharedCounter {
    private var value: Long = 0

    @Synchronized
    fun get(): Long {
        return value
    }

    @Synchronized
    fun getAndIncrement(): Long {
        return value++;
    }
}
