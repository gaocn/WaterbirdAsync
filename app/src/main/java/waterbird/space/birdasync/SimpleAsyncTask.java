package waterbird.space.birdasync;

/**
 * Created by 高文文 on 2016/12/14.
 * 简单异步任务， 仅仅返回结果，没有输入参数
 */

public abstract class SimpleAsyncTask<T> extends AsyncTask<Object, Object, T> {
    @Override
    protected T doInBackground(Object... params) {
        return doInBackground();
    }

    protected abstract T doInBackground();
}
