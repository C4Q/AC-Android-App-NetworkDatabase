package nyc.c4q.networkingdatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    static TextView tv;
    static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.text_view);
        context = this;

        DataDownloader dd = new DataDownloader();
        dd.execute("https://raw.githubusercontent.com/operable/cog/master/priv/css-color-names.json");
    }


    class DataDownloader extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            return downloadData(urls[0]);
        }

        @Override
        protected void onPostExecute(String data) {
            // UI code that uses data here.

            if(data == null) {
                Toast.makeText(context, "Data is null", Toast.LENGTH_LONG).show();
                data = "{'blue': '#0000ff'}";
                // return;
            }
            tv.setText(data);

            Type collectionType = new TypeToken<HashMap<String, String>>() {
            }.getType();
            Gson gs = new Gson();
            HashMap<String, String> res = gs.fromJson(new StringReader(data), collectionType);

            // save res to db
            DbHelper db = new DbHelper(context);
            for(Map.Entry<String, String> entry: res.entrySet()) {
                db.insertColor(entry.getKey(), entry.getValue());
            }

            tv.setText(tv.getText() + " " + String.valueOf(db.numberOfRows()));
        }


        private String downloadData(String urlString) {
            InputStream is = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                is = conn.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
                return new String(total);
            } catch (Exception e) {
                return null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void makeRequestWithOkHttp(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                // UI code that responds to failed request
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String data = response.body().string();
                // UI code that uses data here.
            }
        });
    }

    class DbHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "colors.db";
        private static final String TABLE_NAME = "colors_table";
        private static final int SCHEMA_VERSION = 1;
        private static final String NAME_COLUMN = "color_names";
        private static final String VALUE_COLUMN = "color_values";

        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, SCHEMA_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(String.format("CREATE TABLE %s (%s STRING PRIMARY KEY, %s STRING);", TABLE_NAME, NAME_COLUMN, VALUE_COLUMN));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public boolean insertColor(String name, String value) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put(NAME_COLUMN, name);
            contentValues.put(VALUE_COLUMN, value);

            db.insert(TABLE_NAME, null, contentValues);
            return true;
        }

        public int numberOfRows() {
            SQLiteDatabase db = this.getReadableDatabase();
            int numRows = (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
            return numRows;
        }
    }
}
