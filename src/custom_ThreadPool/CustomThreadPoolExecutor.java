package custom_ThreadPool;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Кастомизированный пул потоков с балансировкой задач по наименьшей загруженности.
 */
public class CustomThreadPoolExecutor implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    private final List<BlockingQueue<Runnable>> taskQueues; // Очереди задач для каждого потока
    private final List<Worker> workers; // Список рабочих потоков
    private final ThreadFactory threadFactory;
    private final AtomicInteger activeThreads = new AtomicInteger(0); // Количество активных потоков
    private volatile boolean isShutdown = false; // Флаг завершения работы пула

    public CustomThreadPoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit, int queueSize, int minSpareThreads) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;

        this.taskQueues = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.threadFactory = new CustomThreadFactory();

        for (int i = 0; i < corePoolSize; i++) {
            addWorker(); // Создание базовых потоков
        }
    }

    /**
     * Добавляет новый рабочий поток, если пул не достиг максимального размера.
     */
    private void addWorker() {
        if (workers.size() < maxPoolSize) {
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
            Worker worker = new Worker(queue);
            Thread thread = threadFactory.newThread(worker);
            worker.setThread(thread); // Связываем поток с Worker
            taskQueues.add(queue);
            workers.add(worker);
            thread.start();
            activeThreads.incrementAndGet();
        }
    }


    /**
     * Находит очередь с наименьшей загруженностью.
     */
    private BlockingQueue<Runnable> getLeastLoadedQueue() {
        return taskQueues.stream().min(Comparator.comparingInt(BlockingQueue::size)).orElse(null);
    }

    // Возвращает количество созданных потоков
    public int getCreatedThreads() {
        return workers.size();
    }

    /**
     * Добавляет задачу в очередь с наименьшей загрузкой.
    * Если количество активных потоков меньше corePoolSize или количество свободных потоков меньше minSpareThreads, создается новый поток.
    * Если очередь переполнена, задача отклоняется и обрабатывается методом handleRejectedTask.
     */
    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPool is shutting down");
        }
        BlockingQueue<Runnable> queue = getLeastLoadedQueue();
        if (queue != null && queue.offer(command)) {
            System.out.println("[Pool] Task accepted into queue: " + command.toString());
            if (activeThreads.get() < corePoolSize || (workers.size() - activeThreads.get() < minSpareThreads)) {
                addWorker(); // Добавление нового потока при необходимости
            }
        } else {
            handleRejectedTask(command);
        }
    }

    /**
     * Обрабатывает отклоненные задачи. Попытка выполнить немедленно или повторное добавление в очередь.
     */
    private void handleRejectedTask(Runnable task) {
        System.out.println("[Rejected] Task " + task.toString() + " was rejected due to overload!");
        if (activeThreads.get() < maxPoolSize) {
            task.run(); // Выполнение задачи в текущем потоке
        } else {
            try {
                Thread.sleep(5); // Ожидание перед повторной попыткой
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        workers.forEach(Worker::stopWorker);
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        taskQueues.forEach(BlockingQueue::clear);
        workers.forEach(Worker::stopWorker);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (Worker worker : workers) {
            long remainingTime = deadline - System.nanoTime();
            if (remainingTime <= 0) return false;
            Thread workerThread = worker.getThread();
            if (workerThread != null) {
                workerThread.join(unit.toMillis(remainingTime));
            }
        }
        return workers.isEmpty();
    }

    /**
     * Рабочий поток, обрабатывающий задачи из закрепленной за ним очереди.
     */
    private class Worker implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private volatile boolean running = true;
        private Thread thread; // Ссылка на поток

        public Worker(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }

        public Thread getThread() {
            return thread;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Runnable task = queue.poll(keepAliveTime, timeUnit);
                    if (task != null) {
                        System.out.println("[Worker] " + thread.getName() + " executes " + task.toString());
                        activeThreads.incrementAndGet();
                        task.run();
                        activeThreads.decrementAndGet();
                    } else if (workers.size() > corePoolSize) {
                        System.out.println("[Worker] " + thread.getName() + " idle timeout, stopping.");
                        workers.remove(this);
                        break;
                    }
                } catch (InterruptedException ignored) {}
            }
            System.out.println("[Worker] " + thread.getName() + " terminated.");
        }

        public void stopWorker() {
            running = false;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    /**
     * Фабрика потоков, создающая потоки с уникальными именами.
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MyPool-worker-" + counter.incrementAndGet());
            System.out.println("[ThreadFactory] Creating new thread: " + thread.getName());
            return thread;
        }
    }
}