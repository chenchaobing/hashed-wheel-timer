package com.ifesdjeen.timer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TimerTest {

  HashWheelTimer timer;

  @Before
  public void before() {
    // TODO: run tests on different sequences
    timer = new HashWheelTimer(10, 8, new WaitStrategy.SleepWait());
  }

  @After
  public void after() throws InterruptedException {
    timer.shutdownNow();
    assertTrue(timer.awaitTermination(10, TimeUnit.SECONDS));
  }

  @Test
  public void scheduleOneShotRunnableTest() throws InterruptedException {
    AtomicInteger i = new AtomicInteger(1);
    timer.schedule(() -> {
                     i.decrementAndGet();
                   },
                   100,
                   TimeUnit.MILLISECONDS);

    Thread.sleep(300);
    assertThat(i.get(), is(0));
  }

  @Test
  public void testOneShotRunnableFuture() throws InterruptedException, TimeoutException, ExecutionException {
    AtomicInteger i = new AtomicInteger(1);
    long start = System.currentTimeMillis();
    assertNull(timer.schedule(() -> {
                                i.decrementAndGet();
                              },
                              100,
                              TimeUnit.MILLISECONDS)
                    .get(10, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    assertTrue(end - start > 100);
  }

  @Test
  public void scheduleOneShotCallableTest() throws InterruptedException {
    AtomicInteger i = new AtomicInteger(1);
    timer.schedule(() -> {
                     i.decrementAndGet();
                     return "Hello";
                   },
                   100,
                   TimeUnit.MILLISECONDS);

    Thread.sleep(300);
    assertThat(i.get(), is(0));
  }

  @Test
  public void testOneShotCallableFuture() throws InterruptedException, TimeoutException, ExecutionException {
    AtomicInteger i = new AtomicInteger(1);
    long start = System.currentTimeMillis();
    assertThat(timer.schedule(() -> {
                                i.decrementAndGet();
                                return "Hello";
                              },
                              100,
                              TimeUnit.MILLISECONDS)
                    .get(10, TimeUnit.SECONDS),
               is("Hello"));
    long end = System.currentTimeMillis();
    assertTrue(end - start > 100);
  }

  @Test
  public void fixedRateFirstFireTest() throws InterruptedException, TimeoutException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(1);
    long start = System.currentTimeMillis();
    timer.scheduleAtFixedRate(() -> {
                                latch.countDown();
                              },
                              100,
                              100,
                              TimeUnit.MILLISECONDS);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    assertTrue(end - start > 100);
  }

  @Test
  public void delayBetweenFixedRateEvents() throws InterruptedException, TimeoutException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(2);
    List<Long> r = new ArrayList<>();
    timer.scheduleAtFixedRate(() -> {

                                r.add(System.currentTimeMillis());

                                latch.countDown();

                                if (latch.getCount() == 0)
                                  return; // to avoid sleep interruptions

                                try {
                                  Thread.sleep(50);
                                } catch (InterruptedException e) {
                                  e.printStackTrace();
                                }

                                r.add(System.currentTimeMillis());
                              },
                              100,
                              100,
                              TimeUnit.MILLISECONDS);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    // time difference between the beginning of second tick and end of first one
    assertTrue(r.get(2) - r.get(1) <= 50);
    //    assertTrue(r.get(2) - r.get(1) > 100);
  }

  @Test
  public void delayBetweenFixedDelayEvents() throws InterruptedException, TimeoutException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(2);
    List<Long> r = new ArrayList<>();
    timer.scheduleWithFixedDelay(() -> {

                                r.add(System.currentTimeMillis());

                                latch.countDown();

                                if (latch.getCount() == 0)
                                  return; // to avoid sleep interruptions

                                try {
                                  Thread.sleep(50);
                                } catch (InterruptedException e) {
                                  e.printStackTrace();
                                }

                                r.add(System.currentTimeMillis());
                              },
                              100,
                              100,
                              TimeUnit.MILLISECONDS);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    // time difference between the beginning of second tick and end of first one
    assertTrue(r.get(2) - r.get(1) > 100);
  }

  @Test
  public void fixedRateSubsequentFireTest() throws InterruptedException, TimeoutException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(10);
    long start = System.currentTimeMillis();
    timer.scheduleAtFixedRate(() -> {
                                latch.countDown();
                                //thre
                              },
                              100,
                              100,
                              TimeUnit.MILLISECONDS);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    long end = System.currentTimeMillis();
    assertTrue(end - start > 1000);
  }

  // TODO: precision test
  // capture deadline and check the deviation from the deadline for different amounts of tasks
}