package waterbird.space.birdasync;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by 高文文 on 2016/12/14.
 * the {@link TaskExecutor} can be executed in many ways:
 * <ul>
 * <li>1. Ordered Tasks, 能都序列化执行任务</li>
 * <li>2. CyclicBarrierTask, 并发执行任务，任务执行完成后会在公共屏障点等待直到所有任务都执行完成</li>
 * <li>3. Delayed Tasks, 延迟执行任务</li>
 * <li>4. Timer Runnable, 定时执行任务</li>
 * </ul>
 */

public class TaskExecutor {

    /**
     *  添加任务到线程池中去执行
     * @param task
     */
    public static void start(Runnable task) {
        AsyncTask.execute(task);
    }

    /**
     * 添加任务到线程池中去执行,且允许该线程被丢失(因为高并发超出线程数量限制时会被抛弃)
     * @param task
     */
    public static void startAllowingLoss(Runnable task) {
        AsyncTask.executeAllowingLoss(task);
    }

    /**
     * 延迟执行任务
     * @param task
     * @param time
     * @param unit
     */
    public static void startDelayedTask(final AsyncTask<?, ?, ?> task, long time, TimeUnit unit) {
        long delay = time;
        if(unit != null) delay = unit.toMillis(time);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                task.execute();
            }
        }, delay);
    }

    /**
     *  定时执行任务
     * @param task
     * @param delay  > 0   第一次延迟执行时间
     * @param period > 0   心跳间隔时间，即下一次执行的时间隔间
     */
    public static void startTimerTask(final Runnable task, long delay, long period) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };
        timer.schedule(timerTask, delay, period);
    }
    /** 序列化执行器 */
    public static OrderedTaskExecutor newOrderedTaskExecutor() {
        return new OrderedTaskExecutor();
    }
    /**  带屏障的执行器 */
    public static  CyclicBarrierExecutor newCyclicBarrierExecutor() {
        return new CyclicBarrierExecutor();
    }

    /**
     * 序列化执行器
     */
    public static class OrderedTaskExecutor {
        /** 序列任务队列 */
        LinkedList<AsyncTask<?, ?, ?>> taskList = new LinkedList<>();
        /**  */
        private transient boolean isRunning  = false;

        /** 添加序列化任务 */
        public OrderedTaskExecutor put(AsyncTask<?, ?, ?> task) {
            synchronized (taskList) {
                if(task != null) {
                    taskList.add(task);
                }
            }
            return this;
        }

        public void start() {
            if(isRunning)return;
            isRunning = true;
            /** 为了任务设置监听器，以实现序列化执行任务 */
            for(AsyncTask<?, ?, ?> each : taskList) {
                final AsyncTask<?, ?, ?> task = each;
                task.setFinishedListener(new AsyncTask.FinishedListener() {
                    @Override
                    public void onCancelled() {
                        synchronized (taskList) {
                            taskList.remove(task);
                            if(task.getStatus() == AsyncTask.Status.RUNNING) {
                                scheduleNext();
                            }
                        }
                    }

                    @Override
                    public void onPostExecute() {
                        /** 序列执行taskList中的任务 */
                        synchronized (taskList) {
                            scheduleNext();
                        }
                    }
                });
            }
            /** 设置监听器后， 从taskList中的第一个任务开始执行 */
            scheduleNext();
        }

        private  void scheduleNext() {
            AsyncTask<?, ?, ?> nextTask = null;
            if(taskList.size() > 0) {
                nextTask = taskList.removeFirst();
            }
            if(nextTask != null) {
                nextTask.execute();
            } else {
                //序列化执行器结束
                Log.d("OrderedTaskExecutor", "scheduleNext: 序列化执行器结束");
                isRunning = false;
            }
        }
    }

    /**
     * 含有屏障点的执行器
     */
    public static class CyclicBarrierExecutor {
        /** 执行完成后，需要在公共屏障点等待的任务列表 */
        ArrayList<AsyncTask<?, ?, ?>> taskList = new ArrayList<>();
        private transient boolean isRunning = false;
        /** 链式编程， 允许连续添加任务 */
        public CyclicBarrierExecutor put(AsyncTask<?, ?, ?> task) {
            if(task != null)taskList.add(task);
            return this;
        }

        public void start(final AsyncTask<?, ?, ?> endOnUiTask, final long time, final TimeUnit unit) {
            if(isRunning) {
                throw new RuntimeException("CyclicBarrierExecutor can only start once!");
            }
            isRunning = true;
            final CountDownLatch latch = new CountDownLatch(taskList.size());
            /** 添加屏障点后要执行的任务 endOnUiTask */
            new SimpleAsyncTask<Boolean>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        if(unit == null) {
                            latch.await();
                        } else {
                            latch.await(time, unit);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    endOnUiTask.execute();
                }
            }.execute();

            startInternal(latch);
        }
        public void start(AsyncTask<?, ?, ?> finishTask) {
            start(finishTask, 0, null);
        }

        public void start(final Runnable endOnUiTask, final long time, final TimeUnit unit) {
            if(isRunning) {
                throw new RuntimeException("CyclicBarrierExecutor can only start once!");
            }
            isRunning = true;
            final CountDownLatch latch = new CountDownLatch(taskList.size());
            /** 添加屏障点后要执行的任务 endOnUiTask */
            new SimpleAsyncTask<Boolean>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        if(unit == null) {
                            latch.await();
                        } else {
                            latch.await(time, unit);
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    endOnUiTask.run();
                }
            }.execute();

            startInternal(latch);
        }
        public void start(Runnable finishTask) {
            start(finishTask, 0, null);
        }

        /**
         * 执行任务列表的任务，并且在任务完成是使用CountDownLatch实现屏障点等待
         * @param latch
         */
        private void startInternal(final CountDownLatch latch) {
            for( AsyncTask<?, ?, ?> task : taskList) {
                task.setFinishedListener(new AsyncTask.FinishedListener() {
                    @Override
                    public void onCancelled() {
                        latch.countDown();
                    }

                    @Override
                    public void onPostExecute() {
                        latch.countDown();
                    }
                });
                task.execute();
            }
        }

    }
}
