package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.StockContract;

import org.hogel.android.linechartview.LineChartView;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {
    private static final double MAX_Y = 100;
    private LineChartView chartView;
    List<LineChartView.Point> pricePoints = new ArrayList<>();
    List<LineChartView.Point> changePoints = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        EditText text = (EditText) findViewById(R.id.title_editText);
        text.setOnKeyListener(onSoftKeyboardDonePress);
        text.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // do what action you want
                    Toast.makeText(DetailActivity.this, "checking event", Toast.LENGTH_LONG).show();
                }

                return false;
            }


        });



        Intent recieved = getIntent();
        if(recieved != null) {
           /* String symbol = recieved.getStringExtra("Stock_id");
             Cursor c = getContentResolver().query(StockContract.StockEntry.CONTENT_URI,
                    new String[] { QuoteColumns.CHANGE, QuoteColumns.BIDPRICE }, QuoteColumns.SYMBOL + "= ?",
                    new String[] { symbol }, null);
            nextChartData(symbol,true);*/
        }


        chartView  = (LineChartView) findViewById(R.id.chart_view);
        chartView.setManualMinY(0);

        View nextButton = findViewById(R.id.next_data);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextChartData("",true);
            }
        });
    }

    private void nextChartData(String symbol, boolean isPricePoint) {

        for (int i = 0; i < 5; i++) {
            int y = (int) (Math.random() * MAX_Y);
            pricePoints.add(new LineChartView.Point(i, y));
        }
        chartView.setPoints(pricePoints);
    }
    private View.OnKeyListener onSoftKeyboardDonePress=new View.OnKeyListener()
    {
        public boolean onKey(View v, int keyCode, KeyEvent event)
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_1)
            {
                Toast.makeText(DetailActivity.this, "checking event", Toast.LENGTH_LONG).show();
            }
            return false;
        }
    };

}