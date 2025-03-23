package performance_research;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import custom_ThreadPool.CustomThreadPoolExecutor;

import java.util.concurrent.*;
import java.util.*;

public class PerformanceTest {
    private static final int TASK_COUNT = 1000;
    private static final int TASK_DURATION_MS = 10;

    public static void main(String[] args) {
        List<TestResult> results = new ArrayList<>();

        results.add(testCustomExecutor(new CustomThreadPoolExecutor(4, 16, 5, TimeUnit.SECONDS, 100, 2), "CustomThreadPool"));
        results.add(testExecutor(Executors.newFixedThreadPool(16), "FixedThreadPool"));
        results.add(testExecutor(Executors.newCachedThreadPool(), "CachedThreadPool"));
        results.add(testExecutor(Executors.newSingleThreadExecutor(), "SingleThreadExecutor"));

        printResults(results);
    }

    private static TestResult testExecutor(ExecutorService executor, String name) {
        long startTime = System.currentTimeMillis();
        int tasksCompleted = 0;
        int createdThreads = 0;

        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i;
            executor.execute(() -> {
                try { Thread.sleep(TASK_DURATION_MS); } catch (InterruptedException ignored) {}
            });
            tasksCompleted++;
        }

        executor.shutdown();
        try { executor.awaitTermination(2, TimeUnit.MINUTES); } catch (InterruptedException ignored) {}

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTaskTime = (double) totalTime / TASK_COUNT;

//        if (executor instanceof CustomThreadPoolExecutor) {
//            createdThreads = ((CustomThreadPoolExecutor) executor).getCreatedThreads();
//        }

        return new TestResult(name, totalTime, avgTaskTime, tasksCompleted);
    }

    private static TestResult testCustomExecutor(CustomThreadPoolExecutor executor, String name) {
        long startTime = System.currentTimeMillis();
        int tasksCompleted = 0;

        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i;
            executor.execute(() -> {
                try { Thread.sleep(TASK_DURATION_MS); } catch (InterruptedException ignored) {}
            });
            tasksCompleted++;
        }

        executor.shutdown();
        try { executor.awaitTermination(2, TimeUnit.MINUTES); } catch (InterruptedException ignored) {}

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTaskTime = (double) totalTime / TASK_COUNT;

//        if (executor instanceof CustomThreadPoolExecutor) {
//            createdThreads = ((CustomThreadPoolExecutor) executor).getCreatedThreads();
//        }

        return new TestResult(name, totalTime, avgTaskTime, tasksCompleted);
    }

    private static void printResults(List<TestResult> results) {
        System.out.println("\n--- Performance Test Results ---");
        System.out.printf("%-20s %-15s %-20s %-15s%n", "Executor Type", "Total Time (ms)", "Avg Task Time (ms)", "Tasks Completed");
        System.out.println("--------------------------------------------------------------");

        for (TestResult result : results) {
            System.out.printf("%-20s %-15d %-20.2f %-15d%n",
                    result.executorType, result.totalTime, result.avgTaskTime, result.tasksCompleted);
        }
    }

    private static class TestResult {
        String executorType;
        long totalTime;
        double avgTaskTime;
        int tasksCompleted;

        public TestResult(String executorType, long totalTime, double avgTaskTime, int tasksCompleted) {
            this.executorType = executorType;
            this.totalTime = totalTime;
            this.avgTaskTime = avgTaskTime;
            this.tasksCompleted = tasksCompleted;
        }
    }
}

