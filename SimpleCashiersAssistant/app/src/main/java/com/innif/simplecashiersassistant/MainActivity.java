package com.innif.simplecashiersassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//TODO custom colors
//TODO custom grid
//TODO import-export

public class MainActivity extends AppCompatActivity {

    List<Product> products = new LinkedList<>();
    GridLayout gl;
    TextView sumView;
    float sum;
    int totalCells;
    String currency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gl = findViewById(R.id.glButtons);
        sumView = findViewById(R.id.tvSum);

        Button buttonClear = findViewById(R.id.bClear);
        buttonClear.setOnClickListener((view)->clear());
        findViewById(R.id.bSettings).setOnClickListener(this::settingsDialog);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        currency = prefs.getString("currency", "€");

        setGrid(4, 3);

        load();
        update();
        updatePrice();
    }

    private void clear(){
        sum = 0;
        updatePrice();
        for (Product p : products) {
            runOnUiThread(p::resetCount);
        }
    }

    private void addArticle(String name, float price){
        products.add(new Product(name, price));
    }

    private void setGrid(int rows, int columns){
        gl.setRowCount(rows);
        gl.setColumnCount(columns);
        totalCells = rows * columns;
        update();
    }

    private void update(){
        if (products.isEmpty())
            findViewById(R.id.tvInstructions).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.tvInstructions).setVisibility(View.GONE);
        gl.removeAllViews();
        for (Product p : products) {
            p.getView(this, gl, new Product.ProductSelectListener() {
                @Override
                public void productClicked(Product p) {
                    sum += p.price;
                    runOnUiThread(p::add);
                    updatePrice();
                }

                @Override
                public boolean productLongClicked(Product p) {
                    return longClick(p);
                }
            }, currency);
        }
        for (int i = 0; i < totalCells - products.size(); i++) {
            View v = this.getLayoutInflater().inflate(R.layout.empty_button, gl);
        }
    }

    @SuppressLint("DefaultLocale")
    private void updatePrice(){
        sumView.setText(String.format("%.2f %s", sum, currency));
    }

    private boolean longClick(Product p){
        configDialog(p);
        return true;
    }

    private void save(){
        LinkedList<String> productNames = new LinkedList<>();
        for (Product p : products) {
            p.save();
            productNames.add(p.title);
        }
        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("products", new HashSet<>(productNames));
        editor.apply();
    }

    private void load(){
        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        Set<String> products =  sharedPref.getStringSet("products", new HashSet<>());
        for (String p : products){
            this.products.add(new Product(p, this));
        }
    }

    private void settingsDialog(View view){
        View v = getLayoutInflater().inflate(R.layout.settings_dialog, null);
        final EditText[] currency = {v.findViewById(R.id.etCurrency)};
        currency[0].setText(this.currency);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v).setTitle("Settings");
        builder.setPositiveButton("Apply", (dialogInterface, i) -> {
            this.currency = currency[0].getText().toString();
            SharedPreferences.Editor prefs = getSharedPreferences("settings", MODE_PRIVATE).edit();
            prefs.putString("currency", this.currency);
            prefs.apply();
            update();
            updatePrice();
            dialogInterface.dismiss();
        });
        AlertDialog dialog = builder.create();
        v.findViewById(R.id.bAddProduct).setOnClickListener((view2)->{
            Product p = new Product("New Product", 1);
            configDialog(p);
            products.add(p);
            dialog.dismiss();
        });
        dialog.show();
    }

    private boolean checkForDoubleName(String name, Product p){
        for (Product p2 : products){
            if (p == p2)
                continue;
            if (name.equals(p2.title))
                return true;
        }
        return false;
    }

    private void configDialog(Product p){
        View v = getLayoutInflater().inflate(R.layout.product_config_dialog, null);
        EditText etName = v.findViewById(R.id.etName);
        EditText etPrice = v.findViewById(R.id.etPrice);
        TextView tvCurrency = v.findViewById(R.id.tvPriceCurrency);
        tvCurrency.setText(currency);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v).setTitle("Edit Product Info");
        builder.setPositiveButton("Apply", (dialogInterface, i) -> {
            String title = etName.getText().toString();
            if (checkForDoubleName(title, p)){
                Toast toast = Toast.makeText(this, getString(R.string.instruction), Toast.LENGTH_LONG);
                toast.show();
            }
            else {
                p.title = title;
            }
            p.price = Float.parseFloat(etPrice.getText().toString());
            p.invalidateView();
            update();
            save();
        });
        AlertDialog dialog = builder.create();

        dialog.show();
        dialog.findViewById(R.id.bUp).setOnClickListener(view1 -> {
            int index = products.indexOf(p);
            if (index > 0){
                products.remove(p);
                products.add(index - 1, p);
            }
            update();
        });
        dialog.findViewById(R.id.bDown).setOnClickListener(view1 -> {
            int index = products.indexOf(p);
            if (index < products.size()-1){
                products.remove(p);
                products.add(index + 1, p);
            }
            update();
        });
        dialog.findViewById(R.id.bDelete).setOnClickListener((view)->{
            products.remove(p);
            update();
            save();
            dialog.dismiss();
        });
        etName.setText(p.title);
        etPrice.setText(Float.toString(p.price));
    }
}