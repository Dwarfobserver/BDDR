package common

import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable

// Allow to produce and consume values in a thread-safe way
class Channel[T]() {
    private val lock = new ReentrantLock
    private val queue = new mutable.ArrayBuffer[T]
    private var position = 0

    // Used by the producer to add a value
    def push(value: T): Unit = {
        lock.lock()
        try
            queue += value
        finally
            lock.unlock()
    }

    // Used by the consumer to attempt to get the next value
    def pop() : Option[T] = {
        lock.lock()
        try
            if (position == queue.size)
                None
            else {
                val value = queue(position)
                position += 1
                Some(value)
            }
        finally
            lock.unlock()
    }
}
