package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.data.StockContract;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  public final String SendSymbol = "Stock_id";
  boolean isConnected;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);



    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    isConnected = activeNetwork != null &&
        activeNetwork.isConnectedOrConnecting();
    setContentView(R.layout.activity_my_stocks);
    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null){
      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected){
        startService(mServiceIntent);

      } else{
        networkToast();
      }
    }
    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null);
    recyclerView.setAdapter(mCursorAdapter);
    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override public void onItemClick(View v, int position) {
                Intent goTo = new Intent(getBaseContext(), DetailActivity.class);
               TextView textView = (TextView) v.findViewById(R.id.stock_symbol);
                Log.d("test",textView.getText().toString());
                String symbol = (String) textView.getText();
                  goTo.putExtra(SendSymbol, symbol);
                  startActivity(goTo);

              }
            }));



    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (isConnected){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly
                  Cursor c = getContentResolver().query(StockContract.StockEntry.CONTENT_URI,
                      new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                      new String[] { input.toString() }, null);
                  if (c.getCount() != 0) {
                    Toast toast =
                        Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                    toast.show();
                    return;
                  } else {
                    // Add the stock to DB
                    mServiceIntent.putExtra("tag", "add");
                    mServiceIntent.putExtra("symbol", input.toString());
                    startService(mServiceIntent);

                  }
                }
              })
              .show();
        } else {
          networkToast();
        }

      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);

    mTitle = getTitle();
    if (isConnected){
      long period = 1200L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }else{
      Log.v("Error Connecting", "Test");
    }
  }


  @Override
  public void onResume() {
    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
  }

  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    // This narrows the return to only the stocks that are most current.
    String Selection = "(" + StockContract.StockEntry.COLUMN_STOCK_SYMBOL + " NOT NULL) GROUP BY (" + StockContract.StockEntry.COLUMN_STOCK_SYMBOL + ")";
    String Selection2 = StockContract.StockEntry.COLUMN_ISCURRENT + "=?";
    CursorLoader loader = new CursorLoader(this, StockContract.StockEntry.CONTENT_URI,
            new String[]{StockContract.StockEntry._ID,StockContract.StockEntry.COLUMN_STOCK_SYMBOL  , StockContract.StockEntry.COLUMN_BIDPRICE,
                    StockContract.StockEntry.COLUMN_CHANGE, StockContract.StockEntry.COLUMN_PERCENT_CHANGE, StockContract.StockEntry.COLUMN_ISUP},
            Selection2, new String[]{String.valueOf(StockContract.CURRENT)}, null);
    return loader;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);
    mCursor = data;
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }
  public void notifyInvalid(){
    Toast toast =
            Toast.makeText(MyStocksActivity.this, "This stock name is invalid!",
                    Toast.LENGTH_LONG);
    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
    toast.show();
  }
  static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                              int appWidgetId) {

    //CharSequence widgetText = context.getString(R.string.appwidget_text);
    // Construct the RemoteViews object
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_info);
    //views.setTextViewText(R.id.appwidget_text, widgetText);
    // Create an Intent to launch MainActivity
    Intent intent = new Intent(context, MyStocksActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    views.setOnClickPendingIntent(R.id.widget_button,pendingIntent);

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views);
  }
}
