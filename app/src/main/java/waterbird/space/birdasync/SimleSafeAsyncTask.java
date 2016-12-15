package waterbird.space.birdasync;

/**
 * Created by 高文文 on 2016/12/15.
 *
 * 简单安全能够不过所有的在子线程中出现的异常
 */

public abstract class SimleSafeAsyncTask<T> extends SafeAsyncTask<Object, Object, T> {

    @Override
    protected final T doInBackgroundSafely(Object[] params) throws Exception {
        return doInBackgroundSafely();
    }
    protected abstract T doInBackgroundSafely() throws Exception;

}
