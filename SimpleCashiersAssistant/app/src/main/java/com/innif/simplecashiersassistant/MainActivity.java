package com.innif.simplecashiersassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//TODO custom colors
//TODO import-export

public class MainActivity extends AppCompatActivity {
    List<Product> products = new LinkedList<>();
    GridLayout gl;
    TextView sumView;
    float sum;
    int totalCells;
    String currency;
    private int rows;
    private int columns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gl = findViewById(R.id.glButtons);
        sumView = findViewById(R.id.tvSum);

        Button buttonClear = findViewById(R.id.bClear);
        buttonClear.setOnClickListener((view)->clear());
        findViewById(R.id.bSettings).setOnClickListener(this::onOptionsClicked);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.settings_setting_file), MODE_PRIVATE);
        currency = prefs.getString(getString(R.string.settings_currency), getString(R.string.standard_currency));
        rows = prefs.getInt(getString(R.string.settings_rows), 4);
        columns = prefs.getInt(getString(R.string.settings_columns), 3);

        load();
        setGrid(rows, columns);
        updatePrice();
    }

    private void clear(){
        sum = 0;
        updatePrice();
        for (Product p : products) {
            runOnUiThread(p::resetCount);
        }
    }

    private void setGrid(int rows, int columns){
        gl.removeAllViews();
        gl.setRowCount(rows);
        gl.setColumnCount(columns);
        this.rows = rows;
        this.columns = columns;
        totalCells = rows * columns;

        SharedPreferences.Editor prefEditor = getSharedPreferences(getString(R.string.settings_setting_file), MODE_PRIVATE).edit();
        prefEditor.putInt(getString(R.string.settings_columns), columns);
        prefEditor.putInt(getString(R.string.settings_rows), rows);
        prefEditor.apply();
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
        sumView.setText(String.format("%.2f %s", sum, currency)); //FIXME unterschiedliche Darstellung nach Währung
    }

    private boolean longClick(Product p){
        configDialog(p, false);
        return true;
    }

    private void save(){
        LinkedList<String> productNames = new LinkedList<>();
        for (Product p : products) {
            p.save(this);
            productNames.add(p.title);
        }
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_setting_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(getString(R.string.settings_products), new HashSet<>(productNames));
        editor.apply();
    }

    private void load(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_setting_file), Context.MODE_PRIVATE);
        Set<String> products =  sharedPref.getStringSet(getString(R.string.settings_products), new HashSet<>());
        for (String p : products){
            this.products.add(new Product(p, this));
        }
    }

    private void onOptionsClicked(View view){
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);

        popupMenu.getMenuInflater().inflate(R.menu.options_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()){
                case (R.id.menuitem_settings):
                    settingsDialog();
                    break;
                case (R.id.menuitem_addProduct):
                    configDialog(new Product("", 1), true);
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    private void settingsDialog(){
        View v = getLayoutInflater().inflate(R.layout.settings_dialog, null);
        final EditText[] currency = {v.findViewById(R.id.etCurrency)};
        currency[0].setText(this.currency);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Spinner spinnerRows = v.findViewById(R.id.spinnerRows);
        Spinner spinnerColumns = v.findViewById(R.id.spinnerCollumns);
        spinnerRows.setSelection(rows-3); //FIXME;
        spinnerColumns.setSelection(columns-1); //FIXME
        builder.setView(v).setTitle(R.string.settings);
        builder.setPositiveButton(R.string.apply, null);
        Button deleteAll = v.findViewById(R.id.buttonDeleteAll);
        Button bImport = v.findViewById(R.id.buttonImport);
        Button bExport = v.findViewById(R.id.buttonExport);
        deleteAll.setOnClickListener(view -> deleteAll());
        bImport.setOnClickListener(view -> onImport());
        bExport.setOnClickListener(view -> onExport());

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view)->{
            int rows = Integer.parseInt(spinnerRows.getSelectedItem().toString());
            int columns = Integer.parseInt(spinnerColumns.getSelectedItem().toString());
            if (rows * columns < products.size()){
                Toast.makeText(getApplicationContext(), R.string.grid_to_small, Toast.LENGTH_SHORT).show();
                return;
            }
            setGrid(rows, columns);
            this.currency = currency[0].getText().toString();
            SharedPreferences.Editor prefs = getSharedPreferences(getString(R.string.settings_setting_file), MODE_PRIVATE).edit();
            prefs.putString(getString(R.string.settings_currency), this.currency);
            prefs.apply();
            update();
            updatePrice();
            dialog.dismiss();
        });
    }

    private boolean checkProductName(Product p){
        if (p.title.contains(getString(R.string.seperator))){
            Toast.makeText(getApplicationContext(), R.string.seperator_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (p.title.equals("")){
            Toast.makeText(getApplicationContext(), R.string.not_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        for (Product p2 : products){
            if (p == p2)
                continue;
            if (p.title.equals(p2.title)) {
                Toast.makeText(getApplicationContext(), R.string.instruction, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void configDialog(Product p, boolean newProduct){
        View v = getLayoutInflater().inflate(R.layout.product_config_dialog, null);
        EditText etName = v.findViewById(R.id.etName);
        EditText etPrice = v.findViewById(R.id.etPrice);
        TextView tvCurrency = v.findViewById(R.id.tvPriceCurrency);
        tvCurrency.setText(currency);

        if(newProduct){
            v.findViewById(R.id.bDelete).setVisibility(View.GONE);
            v.findViewById(R.id.tableRowMove).setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v).setTitle(R.string.edit_product_info);
        builder.setPositiveButton(R.string.apply, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            p.title = etName.getText().toString();
            if (products.size() >= totalCells){
                Toast.makeText(getApplicationContext(), R.string.grid_full, Toast.LENGTH_LONG).show();
                return;
            }
            if (checkProductName(p)){
                p.price = Float.parseFloat(etPrice.getText().toString());
                p.invalidateView();
                if (newProduct)
                    products.add(p);
                update();
                save();
                dialog.dismiss();
            }
        });
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

    private void deleteAll(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.are_aou_sure_delete)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    products.clear();
                    update();
                    save();
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void onImport(){
        View v = getLayoutInflater().inflate(R.layout.import_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.import_config)
                .setView(v)
                .setPositiveButton(R.string.import_button, null)
                .setNegativeButton(R.string.cancel, null).create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            EditText code = v.findViewById(R.id.editTextImportCode);
            String s = code.getText().toString();
            if(loadFromString(s)){
                dialog.dismiss();
            }
        });
    }

    private void onExport(){
        String exportData = saveToString();

        View v = getLayoutInflater().inflate(R.layout.export_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.export_config)
                .setView(v)
                .setPositiveButton(R.string.done, null).create();
        dialog.show();

        v.findViewById(R.id.buttonExportClipboard).setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.export_title), exportData);
            clipboard.setPrimaryClip(clip);
        });
        v.findViewById(R.id.buttonExportShare).setOnClickListener(view -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TITLE, R.string.export_title);
            sendIntent.putExtra(Intent.EXTRA_TEXT, exportData);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        });
        v.findViewById(R.id.buttonExportQR).setOnClickListener(view -> {

            Toast.makeText(this, "not yet implemented", Toast.LENGTH_LONG).show();
        });
    }

    private static final int CONFIG_HEAD_SIZE = 3;

    private String saveToString(){
        String sep = getString(R.string.seperator);
        StringBuilder builder = new StringBuilder();
        builder.append(rows).append(sep)
                .append(columns).append(sep)
                .append(products.size()).append(sep);
        for (Product p : products) {
            builder.append(p.title).append(sep);
        }
        for (Product p : products) {
            builder.append(p.price).append(sep);
        }
        return builder.toString();
    }

    private boolean loadFromString(String s){
        List<Product> newProducts = new LinkedList<>();
        int rows, columns, nProducts;
        try {
            String[] elements = s.split(getString(R.string.seperator));
            if(elements.length < CONFIG_HEAD_SIZE){
                Toast.makeText(getApplicationContext(), R.string.error_loading, Toast.LENGTH_LONG).show();
                return false;
            }
            rows = Integer.parseInt(elements[0]);
            columns = Integer.parseInt(elements[1]);
            nProducts = Integer.parseInt(elements[2]);
            if (elements.length < CONFIG_HEAD_SIZE + nProducts * 2 || rows <= 0 || columns <= 0){
                Toast.makeText(getApplicationContext(), R.string.error_loading, Toast.LENGTH_LONG).show();
                return false;
            }
            for (int i = 0; i < nProducts; i++){
                Product p = new Product(
                        elements[CONFIG_HEAD_SIZE+i],
                        Float.parseFloat(elements[CONFIG_HEAD_SIZE+nProducts+i]));
                newProducts.add(p);
            }
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(), R.string.error_loading, Toast.LENGTH_LONG).show();
            return false;
        }
        // apply loaded data
        products.clear();
        setGrid(rows, columns);
        products.addAll(newProducts);
        save();
        update();
        updatePrice();
        return true;
    }
}