package com.placewise.concurrency;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * A hand-written, thread-safe, bounded FIFO queue: the classic <em>producer-consumer</em> monitor.
 *
 * <ul>
 *   <li>{@link #put} blocks (waits) while the queue is full;</li>
 *   <li>{@link #take} blocks while it is empty;</li>
 *   <li>{@link #close} lets the producers finish: consumers drain what is left and then get an empty
 *       {@link Optional}, which is how they learn "no more work, shut down" without a poison pill.</li>
 * </ul>
 *
 * <p>Every method is {@code synchronized} on the queue itself, so all fields are guarded by one lock.
 * Producers and consumers wait on the <em>same</em> monitor, hence {@code notifyAll()} (not
 * {@code notify()}): a single notification could wake a thread of the wrong kind and be wasted.
 * Every {@code wait()} sits inside a {@code while} loop because a thread may wake up spuriously, or
 * after another thread already took the item it was woken for, so the condition must be re-checked.
 *
 * <p>Interrupts are never swallowed: {@code InterruptedException} propagates to the caller, who
 * decides (our workers restore the interrupt flag and stop).
 *
 * @param <T> element type
 */
public final class BoundedQueue<T> {

    private final LinkedList<T> items = new LinkedList<>();
    private final int capacity;
    private boolean closed; // guarded by "this"

    /**
     * Creates a queue.
     *
     * @param capacity maximum number of waiting items, at least 1
     * @throws IllegalArgumentException if {@code capacity} is below 1
     */
    public BoundedQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
        }
        this.capacity = capacity;
    }

    /**
     * Appends an item, waiting while the queue is full.
     *
     * @param item the item, not {@code null}
     * @throws InterruptedException  if interrupted while waiting
     * @throws IllegalStateException if the queue is (or becomes, while waiting) closed
     */
    public synchronized void put(T item) throws InterruptedException {
        Objects.requireNonNull(item, "item");
        while (items.size() == capacity && !closed) {
            wait();
        }
        if (closed) {
            throw new IllegalStateException("The queue is closed");
        }
        items.addLast(item);
        notifyAll(); // wake consumers waiting for an item
    }

    /**
     * Removes the oldest item, waiting while the queue is empty and still open.
     *
     * @return the item, or empty when the queue is closed <em>and</em> fully drained
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized Optional<T> take() throws InterruptedException {
        while (items.isEmpty() && !closed) {
            wait();
        }
        if (items.isEmpty()) {
            return Optional.empty(); // closed and nothing left
        }
        T item = items.removeFirst();
        notifyAll(); // wake producers waiting for space
        return Optional.of(item);
    }

    /** Closes the queue: no more puts; waiting threads wake up; consumers may still drain. Idempotent. */
    public synchronized void close() {
        closed = true;
        notifyAll();
    }

    /**
     * Tells whether {@link #close()} was called.
     *
     * @return {@code true} once closed
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Returns the number of waiting items (a snapshot: it may change right after the call).
     *
     * @return the current size
     */
    public synchronized int size() {
        return items.size();
    }

    /**
     * Returns the capacity given at construction.
     *
     * @return the maximum size
     */
    public int capacity() {
        return capacity;
    }
}
