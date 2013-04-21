package jp.co.tdkn.view.example.hlistview;

import jp.co.tdkn.view.HorizontalListView;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public final class HorizontalListViewActivity extends Activity {

    private HorizontalListView mHListView;
    private ArrayAdapter<CharSequence> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hlist);
        mHListView = (HorizontalListView) findViewById(R.id.list);
        final String[] texts = new String[50];
        for (int i = 0; i < 50; i++) {
            texts[i] = String.valueOf(i + 1);
        }
        mAdapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_list_item_1, texts);
        mHListView.setAdapter(mAdapter);
    }

}
