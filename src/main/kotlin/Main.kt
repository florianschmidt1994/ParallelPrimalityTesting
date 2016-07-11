import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    val limit = Math.pow(10.0, 5.0).toLong()
    val numberOfThreads = 10

    profileImplementation(limit, numberOfThreads, SingleThreadedCounter::class)
    profileImplementation(limit, numberOfThreads, FixedRangeCounter::class)
    profileImplementation(limit, numberOfThreads, AtomicLongCounter::class)
    profileImplementation(limit, numberOfThreads, NotThreadsafeCounter::class)
    profileImplementation(limit, numberOfThreads, ThreadsafeCounter::class)

}

data class Measurement(val time: Long, val count: Int)

fun profileImplementation(limit: Long, numberOfThreads: Int, clazz: KClass<out Countable>) {
    try {
        val instance = clazz.constructors.first().call()
        val measurement = measureNanoTimeAndCount { instance.count(limit, numberOfThreads) }
        println("Found ${measurement.count} primes in ${measurement.time} nanoseconds with $clazz");
    } catch (e: Exception) {
        throw Exception("Can't instantiate class $clazz via reflection", e)
    }
}

fun measureNanoTimeAndCount(f: () -> Int): Measurement {
    var count = 0;
    val time = measureNanoTime { count = f() }
    return Measurement(time, count)
}

/**
 * Simple interface for the different implementations
 */
interface Countable {
    fun count(limit: Long, numberOfThreads: Int): Int
}

/**
 * This implementation iterates over all numbers between
 * 0 and limit and checks whether the number is a prime
 * sequentially
 */
class SingleThreadedCounter : Countable {

    override fun count(limit: Long, numberOfThreads: Int): Int {
        val numberOfPrimes = AtomicInteger();
        for (i in 0..limit) {
            if (isPrime(i)) numberOfPrimes.getAndIncrement()
        }
        return numberOfPrimes.get()
    }
}

/**
 * This implementation splits the input set (0 .. limit) in #threads
 * identically sized splits. Each split then gets checked for primes
 * by another threads
 */
class FixedRangeCounter : Countable {
    override fun count(limit: Long, numberOfThreads: Int): Int {

        val threadPool = Executors.newFixedThreadPool(numberOfThreads);
        val numberOfPrimes = AtomicInteger();

        for (i in 0L..numberOfThreads - 1) {

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
}

/**
 * This implementation provides a counter using an AtomicLong as a shared
 * counter. Each thread picks the next number by calling getAndIncrement()
 * on the shared counter until the limit is reached
 */
class AtomicLongCounter : Countable {
    override fun count(limit: Long, numberOfThreads: Int): Int {
        val counter = AtomicLong();
        val numberOfPrimes = AtomicInteger();
        val threadPool = Executors.newFixedThreadPool(numberOfThreads);

        for (i in 0L..numberOfThreads - 1) {
            threadPool.submit {
                var j = counter.get()
                while (j < limit) {
                    if (isPrime(j)) {
                        numberOfPrimes.getAndIncrement()
                    }
                    j = counter.getAndIncrement()
                }
            }
        }

        threadPool.shutdown()
        threadPool.awaitTermination(1, TimeUnit.HOURS);
        return numberOfPrimes.get()
    }
}

/**
 * This implementation works the same like the AtomicLongCounter,
 * but uses an unsafe custom counter implementation. This demonstrates
 * how multithreading yields false results, because the number of primes
 * produced by this Countable will probably be too high
 */
class NotThreadsafeCounter : Countable {
    override fun count(limit: Long, numberOfThreads: Int): Int {
        val counter = NotThreadsafeLong()
        val numberOfPrimes = AtomicInteger();
        val threadPool = Executors.newFixedThreadPool(numberOfThreads);

        for (i in 0L..numberOfThreads - 1) {
            threadPool.submit {
                var j = counter.get()
                while (j < limit) {
                    if (isPrime(j)) {
                        numberOfPrimes.getAndIncrement()
                    }
                    j = counter.getAndIncrement()
                }
            }
        }

        threadPool.shutdown()
        threadPool.awaitTermination(1, TimeUnit.HOURS);
        return numberOfPrimes.get()
    }
}

/**
 * This implementation works the same like the AtomicLongCounter
 * but uses a custom threadsafe "AtomicLong" implementation
 */
class ThreadsafeCounter : Countable {

    override fun count(limit: Long, numberOfThreads: Int): Int {
        val counter = ThreadsafeLong()
        val numberOfPrimes = AtomicInteger();
        val threadPool = Executors.newFixedThreadPool(numberOfThreads);

        for (i in 0L..numberOfThreads - 1) {
            threadPool.submit {
                var j = counter.get()
                while (j < limit) {
                    if (isPrime(j)) {
                        numberOfPrimes.getAndIncrement()
                    }
                    j = counter.getAndIncrement()
                }
            }
        }

        threadPool.shutdown()
        threadPool.awaitTermination(1, TimeUnit.HOURS);
        return numberOfPrimes.get()

    }
}

/**
 * Check whether a number is a prime
 * This is just some basic implementation grabbed from somewhere on
 * the internet, but that's not the important party anyhow
 */
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

class NotThreadsafeLong {
    private var value: Long = 0

    fun get(): Long {
        return value
    }

    fun getAndIncrement(): Long {
        return value++
    }
}

class ThreadsafeLong {
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
