package waterbird.space.birdasync;

import android.content.Context;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by 高文文 on 2016/12/15.
 * 简单缓存任务，缓存任务的结果，并且方法安全会捕获方法中发生的异常
 */

public abstract class SimpleCachedAsyncTask<T extends Serializable> extends CachedAsyncTask<Object, Object, T> {

    public SimpleCachedAsyncTask(Context context, String key, long expireTime, TimeUnit unit) {
        super(context, key, expireTime, unit);
    }

    @Override
    protected final T retriveDataFromNetwork(Object... params) throws Exception {
        return retriveDataFromNetwork();
    }
    protected abstract T retriveDataFromNetwork() throws Exception;
}
