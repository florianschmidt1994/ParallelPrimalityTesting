import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    val limit = Math.pow(10.0, 5.0).toLong()
    val numberOfThreads = 10

    profileImplementation(limit, numberOfThreads, SingleThreadedPrimeCounter::class)
    profileImplementation(limit, numberOfThreads, FixedRangePrimeCounter::class)
    profileImplementation(limit, numberOfThreads, AtomicLongPrimeCounter::class)
    profileImplementation(limit, numberOfThreads, UnsafeSharedCounterCounter::class)
    profileImplementation(limit, numberOfThreads, SafeSharedCounterCounter::class)

}

data class Measurement(val time: Long, val count: Int)

fun profileImplementation(limit: Long, numberOfThreads: Int, clazz: KClass<out PrimeCounter>) {
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

interface PrimeCounter {
    fun count(limit: Long, numberOfThreads: Int): Int
}

class SingleThreadedPrimeCounter : PrimeCounter {

    override fun count(limit: Long, numberOfThreads: Int): Int {
        val numberOfPrimes = AtomicInteger();
        for (i in 0..limit) {
            if (isPrime(i)) numberOfPrimes.getAndIncrement()
        }
        return numberOfPrimes.get()
    }
}

class FixedRangePrimeCounter : PrimeCounter {
    override fun count(limit: Long, numberOfThreads: Int): Int {

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
}

class AtomicLongPrimeCounter : PrimeCounter {
    override fun count(limit: Long, numberOfThreads: Int): Int {
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
}

class UnsafeSharedCounterCounter : PrimeCounter {
    override fun count(limit: Long, numberOfThreads: Int): Int {
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
}

class SafeSharedCounterCounter : PrimeCounter {


    override fun count(limit: Long, numberOfThreads: Int): Int {
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
