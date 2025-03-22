package custom_ThreadPool;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor(2, 4, 5, TimeUnit.SECONDS, 5, 1);

        for (int i = 0; i < 25; i++) {
            final int taskId = i;
            pool.execute(() -> {
                System.out.println("[Task] Task " + taskId + " is starting");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
                System.out.println("[Task] Task " + taskId + " is completed");
            });
        }

        pool.shutdown();
    }
}