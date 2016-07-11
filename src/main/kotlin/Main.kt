import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    val limit = Math.pow(10.0, 5.0)
    val numberOfThreads = 10

    var time = measureNanoTime { printPrimeSingleThread(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with single threaded implementation")

    time = measureNanoTime { printPrimeFixedRange(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with fixed range")

    time = measureNanoTime { printPrimeSharedCounter(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with shared counter");

    time = measureNanoTime { printPrimeUnsafeSharedCounter(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with unsafe shared counter");

    time = measureNanoTime { printPrimeSafeSharedCounter(limit.toLong(), numberOfThreads) }
    println("$time nanoseconds with safe shared counter");
}

fun printPrimeSingleThread(limit: Long, numberOfThreads: Int) {
    val numberOfPrimes = AtomicInteger();
    for (i in 0..limit) {
        if (isPrime(i)) numberOfPrimes.getAndIncrement()
    }
    println(numberOfPrimes)
}

fun printPrimeFixedRange(limit: Long, numberOfThreads: Int) {

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
    println(numberOfPrimes)
}

fun printPrimeSharedCounter(limit: Long, numberOfThreads: Int) {

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
    println(numberOfPrimes)
}

fun printPrimeUnsafeSharedCounter(limit: Long, numberOfThreads: Int) {
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
    println(numberOfPrimes)

}

fun printPrimeSafeSharedCounter(limit: Long, numberOfThreads: Int) {
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
    println(numberOfPrimes)

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
//    private var value = AtomicLong()

    @Synchronized
    fun get(): Long {
//        synchronized(this, { return value })
//        return value.get()
        return value
    }

    @Synchronized
    fun getAndIncrement(): Long {
//        synchronized(this, {
//            return value++;
//        })
//        return value.getAndIncrement()
        return value++;
    }
}
