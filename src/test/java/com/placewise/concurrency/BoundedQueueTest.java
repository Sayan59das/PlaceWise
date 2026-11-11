package com.placewise.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.Await;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("concurrency")
@Timeout(60)
class BoundedQueueTest {

    @Test
    void itIsFifoInASingleThread() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(5);
        for (int i = 0; i < 5; i++) {
            queue.put(i);
        }
        assertThat(queue.size()).isEqualTo(5);
        for (int i = 0; i < 5; i++) {
            assertThat(queue.take()).contains(i);
        }
        assertThat(queue.size()).isZero();
        assertThat(queue.capacity()).isEqualTo(5);
    }

    @Test
    void capacityMustBePositiveAndNullsAreRefused() {
        assertThatThrownBy(() -> new BoundedQueue<String>(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedQueue<String>(-3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedQueue<String>(1).put(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aProducerWaitsWhileTheQueueIsFullAndResumesWhenSpaceAppears() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(2);
        queue.put(1);
        queue.put(2);
        Thread producer = new Thread(() -> {
            try {
                queue.put(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");
        producer.start();
        Await.state(producer, Thread.State.WAITING);      // blocked in wait(): the queue is full
        assertThat(queue.size()).isEqualTo(2);              // and it really did not sneak in
        assertThat(queue.take()).contains(1);               // makes room, notifyAll wakes the producer
        Await.finished(producer);
        assertThat(queue.take()).contains(2);
        assertThat(queue.take()).contains(3);
    }

    @Test
    void aConsumerWaitsWhileTheQueueIsEmptyAndGetsTheNextItem() throws InterruptedException {
        BoundedQueue<String> queue = new BoundedQueue<>(2);
        AtomicReference<Optional<String>> received = new AtomicReference<>();
        Thread consumer = new Thread(() -> {
            try {
                received.set(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");
        consumer.start();
        Await.state(consumer, Thread.State.WAITING);
        queue.put("hello");
        Await.finished(consumer);
        assertThat(received.get()).contains("hello");
    }

    /**
     * Why wait() sits in a while loop: notifyAll() wakes EVERY waiting producer, but room exists for only
     * one. With "if" instead of "while" all three would insert and the queue would exceed its capacity.
     */
    @Test
    void manyBlockedProducersWokenTogetherStillRespectTheCapacity() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(1);
        queue.put(0);
        List<Thread> producers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            int item = i;
            Thread producer = new Thread(() -> {
                try {
                    queue.put(item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "producer-" + i);
            producers.add(producer);
            producer.start();
        }
        for (Thread producer : producers) {
            Await.state(producer, Thread.State.WAITING);
        }
        assertThat(queue.take()).contains(0);   // room for exactly one; notifyAll wakes all three producers
        Await.until(() -> producers.stream().filter(p -> p.getState() == Thread.State.TERMINATED).count() == 1
                && producers.stream().filter(p -> p.getState() == Thread.State.WAITING).count() == 2,
                "one producer to insert and the other two to go back to waiting");
        assertThat(queue.size()).as("capacity must never be exceeded").isEqualTo(1);
        queue.close();                          // release the two that are still waiting
        for (Thread producer : producers) {
            Await.finished(producer);
        }
    }

    /** The mirror image: one item arrives, three consumers wake, only one may take it. */
    @Test
    void manyBlockedConsumersWokenTogetherOnlyOneTakesTheItem() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(5);
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Thread> consumers = new ArrayList<>();
        List<Integer> received = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 1; i <= 3; i++) {
            Thread consumer = new Thread(() -> {
                try {
                    queue.take().ifPresent(received::add);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "consumer-" + i);
            consumer.setUncaughtExceptionHandler((t, e) -> failures.add(e));
            consumers.add(consumer);
            consumer.start();
        }
        for (Thread consumer : consumers) {
            Await.state(consumer, Thread.State.WAITING);
        }
        queue.put(99);                          // notifyAll wakes all three consumers
        Await.until(() -> consumers.stream().filter(c -> c.getState() == Thread.State.TERMINATED).count() >= 1
                && consumers.stream().filter(c -> c.getState() == Thread.State.WAITING).count() == 2,
                "one consumer to finish and the other two to wait again");
        assertThat(failures).as("a consumer that did not re-check the condition would crash").isEmpty();
        assertThat(received).containsExactly(99);
        queue.close();
        for (Thread consumer : consumers) {
            Await.finished(consumer);
        }
    }

    @Test
    void closeWakesEveryWaitingConsumerWithEmpty() throws InterruptedException {
        BoundedQueue<String> queue = new BoundedQueue<>(2);
        List<AtomicReference<Optional<String>>> results = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            AtomicReference<Optional<String>> result = new AtomicReference<>();
            results.add(result);
            Thread consumer = new Thread(() -> {
                try {
                    result.set(queue.take());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "consumer-" + i);
            consumers.add(consumer);
            consumer.start();
        }
        for (Thread consumer : consumers) {
            Await.state(consumer, Thread.State.WAITING);
        }
        queue.close();
        for (Thread consumer : consumers) {
            Await.finished(consumer);
        }
        assertThat(results).allSatisfy(r -> assertThat(r.get()).isEmpty());
        assertThat(queue.isClosed()).isTrue();
    }

    @Test
    void closeWakesABlockedProducerWhichThenFailsInsteadOfHanging() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(1);
        queue.put(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread producer = new Thread(() -> {
            try {
                queue.put(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IllegalStateException e) {
                failure.set(e);
            }
        }, "producer");
        producer.start();
        Await.state(producer, Thread.State.WAITING);
        queue.close();
        Await.finished(producer);
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void afterCloseConsumersStillDrainWhatIsLeftThenGetEmpty() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(5);
        queue.put(1);
        queue.put(2);
        queue.close();
        queue.close(); // idempotent
        assertThat(queue.take()).contains(1);
        assertThat(queue.take()).contains(2);
        assertThat(queue.take()).isEmpty();
        assertThatThrownBy(() -> queue.put(3)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anInterruptedWaiterGetsInterruptedExceptionAndIsNotSwallowed() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(1);
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Thread consumer = new Thread(() -> {
            try {
                queue.take();
            } catch (InterruptedException e) {
                caught.set(e);
            }
        }, "consumer");
        consumer.start();
        Await.state(consumer, Thread.State.WAITING);
        consumer.interrupt();
        Await.finished(consumer);
        assertThat(caught.get()).isInstanceOf(InterruptedException.class);
        // the queue is still usable afterwards
        queue.put(7);
        assertThat(queue.take()).contains(7);
    }

    @Test
    void waitingIsInsideTheMonitorSoOtherThreadsCanStillUseTheQueue() throws InterruptedException {
        BoundedQueue<Integer> queue = new BoundedQueue<>(1);
        Thread consumer = new Thread(() -> {
            try {
                queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");
        consumer.start();
        Await.state(consumer, Thread.State.WAITING);
        // wait() released the lock: these synchronized calls must not block
        assertThat(queue.size()).isZero();
        assertThat(queue.isClosed()).isFalse();
        queue.put(1);
        Await.finished(consumer);
    }

    @Test
    void fourProducersAndFourConsumersLoseAndDuplicateNothing() throws InterruptedException {
        int producers = 4;
        int consumers = 4;
        int perProducer = 10_000;
        BoundedQueue<Integer> queue = new BoundedQueue<>(16);
        List<List<Integer>> taken = new ArrayList<>();
        List<Thread> producerThreads = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();

        for (int c = 0; c < consumers; c++) {
            List<Integer> mine = new ArrayList<>(); // each consumer writes only its own list
            taken.add(mine);
            consumerThreads.add(new Thread(() -> {
                try {
                    Optional<Integer> item = queue.take();
                    while (item.isPresent()) {
                        mine.add(item.get());
                        item = queue.take();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "consumer-" + c));
        }
        for (int p = 0; p < producers; p++) {
            int id = p;
            producerThreads.add(new Thread(() -> {
                try {
                    for (int i = 0; i < perProducer; i++) {
                        queue.put(id * 1_000_000 + i); // encodes who made it and its sequence number
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "producer-" + p));
        }
        consumerThreads.forEach(Thread::start);
        producerThreads.forEach(Thread::start);
        for (Thread producer : producerThreads) {
            producer.join();
        }
        queue.close(); // graceful shutdown: consumers drain, then see "closed and empty"
        for (Thread consumer : consumerThreads) {
            consumer.join();
        }

        Set<Integer> unique = new HashSet<>();
        int total = 0;
        for (List<Integer> mine : taken) {
            total += mine.size();
            unique.addAll(mine);
            int[] last = new int[producers];
            java.util.Arrays.fill(last, -1);
            for (int value : mine) { // per consumer, one producer's items must arrive in sending order
                int producer = value / 1_000_000;
                int sequence = value % 1_000_000;
                assertThat(sequence).isGreaterThan(last[producer]);
                last[producer] = sequence;
            }
        }
        assertThat(total).as("items taken").isEqualTo(producers * perProducer);
        assertThat(unique).as("distinct items").hasSize(producers * perProducer);
        assertThat(queue.size()).isZero();
    }

    @Test
    void startGateReleasesEveryWaiterAtOnceAndStaysOpen() throws InterruptedException {
        StartGate gate = new StartGate();
        List<Thread> waiters = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread waiter = new Thread(() -> {
                try {
                    gate.awaitOpen();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "waiter-" + i);
            waiters.add(waiter);
            waiter.start();
        }
        for (Thread waiter : waiters) {
            Await.state(waiter, Thread.State.WAITING);
        }
        gate.open();
        for (Thread waiter : waiters) {
            Await.finished(waiter);
        }
        gate.awaitOpen(); // already open: returns immediately
        gate.open();      // idempotent
    }

    @Test
    void threadLifecycleDemoShowsEverySixStatesInOrder() throws InterruptedException {
        List<Thread.State> seen = new ThreadLifecycleDemo().run();
        assertThat(seen).containsExactly(Thread.State.NEW, Thread.State.RUNNABLE, Thread.State.BLOCKED,
                Thread.State.WAITING, Thread.State.TIMED_WAITING, Thread.State.TERMINATED);
        for (Thread.State state : Thread.State.values()) {
            assertThat(ThreadLifecycleDemo.explain(state)).isNotBlank();
        }
    }
}
