package waterbird.space.birdasync;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by 高文文 on 2016/12/15.
 */

public abstract class CachedAsyncTask<Program, Progress, Result extends Serializable> extends SafeAsyncTask<Program, Progress, Result> {
    private static final String TAG = "CachedAsyncTask";
    private static final String DEFAULT_PATH = "/cachedTash";
    private static String cachedPath;
    private static ConcurrentHashMap<String, Long> cachedMap = new ConcurrentHashMap<>();
    private long expireTime = 0;
    private String key;

    /**
     * 缓存任务的初始化
     * @param context app context
     * @param key     used to identify cached task
     * @param expireTime  expired time for cached task
     * @param unit        expired time TimeUnit, if null, default is Millis
     */
    public CachedAsyncTask(Context context, String key, long expireTime, TimeUnit unit) {
        if(context == null) {
            throw new RuntimeException("initialization error: context can not be null");
        }
        if(key == null) {
            throw new RuntimeException("key must not null to identify this cached task");
        }
        this.key = key;
        cachedPath = context.getFilesDir().getAbsolutePath() + DEFAULT_PATH;
        if(unit != null) {
            this.expireTime = unit.toMillis(expireTime);
        } else {
            this.expireTime = expireTime;
        }
    }


    /**
     * 清空缓存，以及缓存的文件
     * @param context
     */
    public static void clearCache(Context context) {
        cachedMap.clear();
        cachedPath = context.getFilesDir().getAbsolutePath() + DEFAULT_PATH;
        File cachedFiles = new File(cachedPath);
        for(final File file : cachedFiles.listFiles()) {
            TaskExecutor.start(new Runnable() {
                @Override
                public void run() {
                    if(file.isFile()) {
                        file.delete();
                    }
                }
            });
        }
    }

    /**
     * 清空缓存Map中的值
     * @param key
     */
    public static void removeValue(String key) {
        cachedMap.remove(key);
    }

    /**
     * 保存结果到cache中
     * @param result
     * @return
     */
    public boolean putToCache(Result result) {
        ObjectOutputStream oos = null;
        try {
            File cachedDir = new File(cachedPath);
            if(!cachedDir.exists()) {
                cachedDir.mkdir();
            }
            oos = new ObjectOutputStream(new FileOutputStream(new File(cachedDir, key)));
            oos.writeObject(result);
            if(Log.isPrint) {
                Log.d(TAG, "SUCCESS to cache result to file with key=" + key);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(Log.isPrint) {
            Log.d(TAG, "FAIL to cache result to file with key=" + key);
        }
        return false;
    }

    public Result getFromCache() {
        ObjectInputStream ois = null;
        Result result = null;

        try {
            ois = new ObjectInputStream(new FileInputStream(new File(cachedPath, key)));
            result = (Result) ois.readObject();
            if(result != null) {
                if(Log.isPrint) {
                    Log.d(TAG, "SUCCESS to get result from cache key=" + key);
                }
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (ClassNotFoundException e) {
          e.printStackTrace();
        } finally {
            try {
                if(ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(Log.isPrint) {
            Log.d(TAG, "FAIL to get result from cache key=" + key);
        }
        return null;
    }

    /**
     * 从网络中获取数据，对网络数据的缓存工作放在doInBackgroundSafely中
     * @param params
     * @return
     */
    protected abstract Result retriveDataFromNetwork(Program... params) throws Exception;


    /**
     * 方法安全，会抛出异常方便用户捕获
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    protected Result doInBackgroundSafely(Program... params) throws Exception {
        Result result = null;
        try {
            Long time = cachedMap.get(key);
            long lastAccessTime = time == null ? 0 : time;
            if(System.currentTimeMillis() - lastAccessTime > expireTime) {
                result = retriveDataFromNetwork(params);
                if(result != null) {
                    if(Log.isPrint) Log.d(TAG, " SUCCESS: Retrive Data From Network key = " + key);
                    cachedMap.put(key, System.currentTimeMillis());
                    putToCache(result);
                } else {
                    if(Log.isPrint) Log.d(TAG, " FAILED: Retrive Data From Network key = " + key);
                    result = getFromCache();
                }
            } else {
                result = getFromCache();
                if(result == null) {
                    result = retriveDataFromNetwork(params);
                    if(result != null) {
                        if(Log.isPrint) Log.d(TAG, " SUCCESS: Retrive Data From Network key = " + key);
                        cachedMap.put(key, System.currentTimeMillis());
                        putToCache(result);
                    } else {
                        if(Log.isPrint) Log.d(TAG, " FAILED: Retrive Data From Network key = " + key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
