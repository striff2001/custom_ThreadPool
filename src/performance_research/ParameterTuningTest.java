package performance_research;

import custom_ThreadPool.CustomThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class ParameterTuningTest {
    private static final int TASK_COUNT = 1000;
    private static final int TASK_DURATION_MS = 10;
    private static final List<int[]> PARAMETER_SETS = Arrays.asList(
            // Тест влияния corePoolSize
            new int[]{2, 8, 50, 5, 2}, // core=2, max=8, queue=50, keepAlive=5s, minSpare=2
            new int[]{4, 8, 50, 5, 2},
            new int[]{8, 8, 50, 5, 2},

            //Тест влияния maxPoolSize
            new int[]{4, 8, 50, 5, 2},
            new int[]{4, 16, 50, 5, 2},
            new int[]{4, 32, 50, 5, 2},

            //Тест влияния queueSize
            new int[]{4, 8, 10, 5, 2},
            new int[]{4, 8, 50, 5, 2},
            new int[]{4, 8, 100, 5, 2},

            //Тест влияния keepAliveTime
            new int[]{4, 8, 50, 5, 2},
            new int[]{4, 8, 50, 10, 2},

            //Тест влияния minSpareThreads
            new int[]{4, 8, 50, 5, 2},
            new int[]{4, 8, 50, 5, 1},
            new int[]{4, 8, 50, 5, 4}
    );

    public static void main(String[] args) {
        List<TestResult> results = new ArrayList<>();

        for (int[] params : PARAMETER_SETS) {
            results.add(testCustomThreadPool(params[0], params[1], params[2], params[3], params[4]));
        }

        printResults(results);
    }

    private static TestResult testCustomThreadPool(int corePoolSize, int maxPoolSize, int queueSize, int keepAliveTime, int minSpareThreads) {
        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, queueSize, minSpareThreads
        );

        long startTime = System.currentTimeMillis();
        int createdThreads = 0;

        for (int i = 0; i < TASK_COUNT; i++) {
            executor.execute(() -> {
                try { Thread.sleep(TASK_DURATION_MS); } catch (InterruptedException ignored) {}
            });
        }

        executor.shutdown();
        try { executor.awaitTermination(2, TimeUnit.MINUTES); } catch (InterruptedException ignored) {}

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTaskTime = (double) totalTime / TASK_COUNT;
        createdThreads = executor.getCreatedThreads();

        return new TestResult(corePoolSize, maxPoolSize, queueSize, keepAliveTime, minSpareThreads, totalTime, avgTaskTime, createdThreads);
    }

    private static void printResults(List<TestResult> results) {
        System.out.println("\n--- Parameter Tuning Results ---");
        System.out.printf("%-10s %-10s %-10s  %-10s %-10s %-15s %-20s %-15s%n",
                "CoreSize", "MaxSize", "Queue", "KeepAlive", "MinSpare", "Total Time (ms)", "Avg Task Time (ms)", "Threads Created");
        System.out.println("-------------------------------------------------------------------------------------");

        for (TestResult result : results) {
            System.out.printf("%-10d %-10d %-10d %-10d %-10d %-15d %-20.2f %-15d%n",
                    result.coreSize, result.maxSize, result.queueSize, result.keepAlive, result.minSpare,
                    result.totalTime, result.avgTaskTime, result.threadsCreated);
        }
    }

    private static class TestResult {
        int coreSize, maxSize, queueSize, keepAlive, minSpare;
        long totalTime;
        double avgTaskTime;
        int threadsCreated;

        public TestResult(int coreSize, int maxSize, int queueSize, int keepAlive, int minSpare, long totalTime, double avgTaskTime, int threadsCreated) {
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueSize = queueSize;
            this.keepAlive = keepAlive;
            this.minSpare = minSpare;
            this.totalTime = totalTime;
            this.avgTaskTime = avgTaskTime;
            this.threadsCreated = threadsCreated;
        }
    }
}


