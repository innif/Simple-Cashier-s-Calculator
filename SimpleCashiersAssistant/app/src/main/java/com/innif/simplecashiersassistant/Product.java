package com.innif.simplecashiersassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

public class Product {
    float price;
    String title;
    View v = null;
    TextView tv = null;
    Button b = null;
    int count = 0;
    Activity context;
    String currency = null;

    public abstract static class ProductSelectListener{
        public abstract void productClicked(Product p);
        public abstract boolean productLongClicked(Product p);
    }

    public Product(String title, float price){
        this.title = title;
        this.price = price;
    }

    public void invalidateView(){
        v = null;
    }

    public View getView(Activity activity, ViewGroup parent, ProductSelectListener selectListener, String currency, Locale locale){
        if (this.currency != null && this.currency.equals(currency) && v != null){
            this.currency = currency;
            parent.addView(v);
            return v;
        }
        activity.getLayoutInflater().inflate(R.layout.article_button, parent);
        v = parent.getChildAt(parent.getChildCount()-1);
        Product p = this;
        b = v.findViewById(R.id.bArticle);
        tv = v.findViewById(R.id.tvCount);
        TextView tvPrice = v.findViewById(R.id.tvPrice);
        tvPrice.setText(String.format(locale, "%.2f %s", price, currency));
        updateCount();
        b.setOnClickListener(view -> selectListener.productClicked(p));
        b.setOnLongClickListener(view -> selectListener.productLongClicked(p));
        b.setText(title);
        context = activity;
        return v;
    }

    public void add(int count){
        this.count += count;
        updateCount();
    }

    public void add(){
        add(1);
        updateCount();
    }

    public void resetCount(){
        count = 0;
        updateCount();
    }

    private void updateCount(){
        if (tv == null || context == null)
            return;

        if (count > 0){
            setButtonColor(R.color.productButtonSelected);
            tv.setText(Integer.toString(count));
        }
        else {
            setButtonColor(R.color.productButton);
            tv.setText("");
        }

        //TODO set Textcolor when >0
    }

    private void setButtonColor(int colorId){
        int color = context.getResources().getColor(colorId, context.getTheme());
        b.setBackgroundTintList(ColorStateList.valueOf(color));
    }
}
