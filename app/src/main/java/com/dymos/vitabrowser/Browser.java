package com.dymos.vitabrowser;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.SearchView;

public class Browser extends WebViewClient {
    EditText searchBar;
    WebView webView;
    public Browser(EditText searchBar,WebView webView) {
        this.searchBar = searchBar;
        this.webView = webView;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String s) {
       view.loadUrl(s);
       return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        searchBar.setHint(webView.getUrl());

    }
}
