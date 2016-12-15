package waterbird.space.birdasync;

/**
 * Created by 高文文 on 2016/12/15.
 */

public abstract class SafeAsyncTask<Program, Progress, Result> extends AsyncTask<Program, Progress, Result> {

    private Exception exception;
    private boolean printStackTrace = true;

    @Override
    protected final void onPreExecute() {
        try {
            onPreExecuteSafely();
        } catch (Exception e) {
            if(printStackTrace) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected  final Result doInBackground(Program... params) {
        try {
            return doInBackgroundSafely(params);
        } catch (Exception e) {
            if(printStackTrace) {
                e.printStackTrace();
            }
            /** 后台任务执行期间的错误传递给用户处理 */
            exception = e;
        }
        return null;
    }

    @Override
    protected final void onPostExecute(Result result) {
        try {
            onPostExecuteSafely(result, exception);
        } catch (Exception e) {
            if(printStackTrace) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected final void onProgressUpdate(Progress... values) {
        try {
            onProgressUpdateSafely(values);
        } catch (Exception e) {
            if(printStackTrace) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 如果用户取消了任务，那么会将回调{@link #onCancelled()}
     * @param result The result, if any, computed in
     *               {@link #doInBackground(Object[])}, can be null
     */
    @Override
    protected void onCancelled(Result result) {
        onCancelled();
    }

    /*********************     API exposed       **********************/

    /**
     * <p>
     *     取代{@link AsyncTask#onPreExecute()}f方法，会抛出任何执行过程中的异常；
     * </p>
     * <p>运行在UI Main Thread中</p>
     * @throws Exception
     */
    protected void onPreExecuteSafely() throws Exception {}

    /**
     * <p>
     *     取代{@link AsyncTask#doInBackground(Object[])}方法，可以捕获任何抛出的异常，方法时安全的；
     * </p>
     * <p>运行子线程中</p>
     * @param params
     * @return
     * @throws Exception
     */
    protected abstract Result doInBackgroundSafely(Program... params) throws Exception;

    /**
     * <p>
     *     取代{@link AsyncTask#onPostExecute(Object)}方法，参数e表示在执行{@link AsyncTask#doInBackground(Object[])}
     * 方法时出现的异常，该异常会传递到主线程中，方便用户处理；同时该方法会抛出执行过程中的任何异常，方法时安全的；
     * </p>
     * <p>运行在UI Main Thread中</p>
     * @param result
     * @param e
     * @throws Exception
     */
    protected void onPostExecuteSafely(Result result, Exception e)  throws Exception{}

    /**
     * <p>
     *     取代{@link AsyncTask#onProgressUpdate(Object[])}方法，用于在子任务执行过程中根据进度，该方法
     *     运行在主线程中，会抛出运行出现的异常，方法时安全的；
     * </p>
     * <p>运行在 UI Main Thread中</p>
     * @param values
     * @throws Exception
     */
    protected void onProgressUpdateSafely(Progress ...values) throws Exception{}

}
