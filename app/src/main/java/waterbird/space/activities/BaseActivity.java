package waterbird.space.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import waterbird.space.async.Log;
import waterbird.space.async.R;

import static waterbird.space.async.R.id.container;

/**
 * Created by 高文文 on 2016/12/13.
 */

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BaseActivity";
    private ScrollView mScrollView;
    private LinearLayout mContainer;
    private TextView mSubTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.setTag(TAG);

        mContainer = (LinearLayout) findViewById(container);
        mScrollView = (ScrollView) mContainer.getParent();
        mSubTitle = (TextView) findViewById(R.id.sub_title);

        String[] bttxt = getButtonTexts();
        if (bttxt != null) {
            for (int i = 0; i < bttxt.length; i++) {
                Button bt = new Button(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                int margin = getResources().getDimensionPixelSize(R.dimen.common_marin);
                lp.setMargins(margin, margin, margin, margin);
                bt.setId(i);
                bt.setText(bttxt[i]);
                bt.setOnClickListener(this);
                bt.setLayoutParams(lp);
                mContainer.addView(bt);
            }
        }
    }
    /**
     * 获取主标题
     */
    public abstract String getMainTitle();

    /**
     * 设置二级标题
     */
    public void setSubTitile(String st) {
        mSubTitle.setText(st);
    }

    /**
     * 取button列表
     */
    public abstract String[] getButtonTexts();

    /**
     * 在{@link #onClick(View)} 里调用。
     * id值得含义为：若{@link #getButtonTexts()}的string[]数组长度为len，则id从0,1,2到len-1.
     * 点击第N个按钮，id变为N。
     */
    public abstract Runnable getButtonClickRunnable(final int id);

    @Override
    public void onClick(View view) {
        Runnable r = getButtonClickRunnable(view.getId());
        if (r != null) {
            new Thread(r).start();
        }
    }
}
