package com.example.news;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.news.models.NewsItem;
import com.example.news.models.RootJsonData;
import com.example.newsItem.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NewsItemAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    public static final String API_KEY = "c2194f57d73e4392ae4ee0bf69e9d391";
    public static final String SORT_ORDER = "popularity";
    public EditText editText;
    public Button button;
    private String keyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        progressBar = findViewById(R.id.progress_circular);
        emptyStateTextView = findViewById(R.id.empty_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        editText = findViewById(R.id.edit_text);
        button = findViewById(R.id.button);

        initEmptyRecyclerView();
        fetchData("");
        swipeRefreshLayout.setOnRefreshListener(() -> fetchData(keyword));

        button.setOnClickListener(view -> searchKeyword(view));
    }

    public void initEmptyRecyclerView() {

        recyclerView = findViewById(R.id.recycler_view);
        adapter = new NewsItemAdapter(mContext, new ArrayList<NewsItem>());
        recyclerView.setAdapter(adapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager
                (this, LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(linearLayoutManager);
    }

    public void fetchData(String keyword) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {

            Call<RootJsonData> rootJsonDataCall = createJsonDataCall(keyword);
            rootJsonDataCall.enqueue(new Callback<RootJsonData>() {
                @Override
                public void onResponse(Call<RootJsonData> call, Response<RootJsonData> response) {
                    swipeRefreshLayout.setRefreshing(false);
                    initRecyclerViewWithResponseData(response);
                }

                @Override
                public void onFailure(Call<RootJsonData> call, Throwable t) {
                    swipeRefreshLayout.setRefreshing(false);
                    emptyStateTextView.setText(t.getMessage());
                }
            });

        } else {
            progressBar.setVisibility(View.GONE);
            emptyStateTextView.setText(R.string.no_internet_connection);
        }

    }

    public Call<RootJsonData> createJsonDataCall(String keyword) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://newsapi.org/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NewsAPI newsAPI = retrofit.create(NewsAPI.class);

        Call<RootJsonData> rootJsonDataCall;

        if (keyword.isEmpty()) {
            rootJsonDataCall = newsAPI.getTopHeadlines();
        } else {
            rootJsonDataCall = newsAPI.getEverythingFromKeyword(keyword, API_KEY, SORT_ORDER);
        }

        return rootJsonDataCall;
    }

    public void initRecyclerViewWithResponseData(Response<RootJsonData> response) {

        RootJsonData rootJsonData = response.body();
        List<NewsItem> newsItemList = rootJsonData.getNewsItems();

        progressBar.setVisibility(View.GONE);
        if (newsItemList.isEmpty()) {
            emptyStateTextView.setText(R.string.no_news_found);
        }

        if (newsItemList != null && !newsItemList.isEmpty()) {
            adapter = new NewsItemAdapter(mContext, newsItemList);
            recyclerView.setAdapter(adapter);
        }
    }

    public void searchKeyword(View view) {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
        initEmptyRecyclerView();
        progressBar.setVisibility(View.VISIBLE);
        keyword = editText.getText().toString();
        fetchData(keyword);
    }
}