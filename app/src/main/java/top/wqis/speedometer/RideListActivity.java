package top.wqis.speedometer;

import android.R.layout;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class RideListActivity extends AppCompatActivity {

    private ListView rideListView;
    private final ArrayList<String> rideList = new ArrayList<>();
    private ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_list);

        assert getSupportActionBar() != null;
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.ride_list) + "</font>"));
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.light_green)));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.light_green));

        //Read the data directory for all files
        readRideData();

        // Inflate fake data
//        rideList.add("2022-01-07 09-56-52");
//        rideList.add("2022-01-22 17-31-41");
//        rideList.add("2022-03-23 22-08-03");
//        rideList.add("2022-01-13 09-39-29");
//        rideList.add("2022-02-06 14-25-32");

        Log.d("Ride list length", String.valueOf(rideList.size()));

        // Display empty view
        TextView emptyView = findViewById(R.id.emptyView);

        if (rideList.size() == 0){
            emptyView.setVisibility(View.VISIBLE);
        }
        else{
            emptyView.setVisibility(View.GONE);
        }

        // Sort the data according to data
        ArrayList<String> rideListNew = new ArrayList<>();
        DateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        DateFormat outFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        // Modify data from inFormat to outFormat for sorting
        for (String i : rideList
             ) {
            try {
                Date date = inFormat.parse(i);
                String d = outFormat.format(date);
                rideListNew.add(d);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Clear the original list containing the records in inFormat
        rideList.clear();

        // Sort the list with outFormat
        Collections.sort(rideListNew);

        // Convert the records in outFormat (sorted) back to inFormat
        for (String i : rideListNew){
            try {
                Date date = outFormat.parse(i);
                String r = inFormat.format(date);
                rideList.add(r);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Add text watcher to do live search actions
        EditText editText = findViewById(R.id.search_ride_list);
        new Thread(() -> {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    arrayAdapter.getFilter().filter(charSequence);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }).run();

        // Init array adapter
        rideListView = findViewById(R.id.LVRideList);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice,rideList);
        rideListView.setAdapter(arrayAdapter);
        //Log.i("setadapater 地址", String.valueOf(rideList.hashCode()));
        rideListView.setOnItemClickListener(onItemClickListener);
        rideListView.setEmptyView(findViewById(R.id.emptyView));
        rideListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        rideListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater menuInflater = getMenuInflater();
                menuInflater.inflate(R.menu.ride_list_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case (R.id.delete_ride_list_action):

                        ArrayList<String> valuesToRemove = new ArrayList<>();

                        SparseBooleanArray array = rideListView.getCheckedItemPositions();

                        AlertDialog.Builder builder = new AlertDialog.Builder(RideListActivity.this);
                        builder.setMessage(getString(R.string.confirm_delete) + array.size() + getString(R.string.confirm_delete_2));
                        builder.setPositiveButton(getString(R.string.delete), (dialogInterface, x) -> {
                            for (int i = 0; i < array.size(); i++){
                                Log.i("array key at", String.valueOf(array.keyAt(i)));
                                Log.i("array get key", String.valueOf(array.get(array.keyAt(i))));
                                Log.i("item checked", rideList.get(array.keyAt(i)));

                                String fname = rideList.get(array.keyAt(i));
                                File f = new File(getFilesDir() + "/rideInfo", fname + ".txt");

                                if (f.delete()){
                                    Log.i("delete", String.valueOf(true));
                                }
                                else{
                                    Log.i("delete", String.valueOf(false));
                                }

                                valuesToRemove.add(fname);
                            }

                            for (String i : valuesToRemove
                            ) {
                                rideList.remove(i);
                            }

                            // TODO 找到为什么notify无效
                            arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice,rideList);
                            rideListView.setAdapter(arrayAdapter);
                            rideListView.clearChoices();
                            rideListView.cancelLongPress();
                        });

                        builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                            rideListView.clearChoices();
                        });

                        builder.create().show();

                        return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    public void readRideData(){
        new Thread(() -> {
            File rideInfoDir = new File(getFilesDir() + "/rideInfo");
            rideInfoDir.mkdirs();

            File[] listFiles = rideInfoDir.listFiles();

            for (File f : listFiles
            ) {
                rideList.add(f.getName().substring(0, f.getName().indexOf(".txt")));
            }
        }).run();
    }

    private final AdapterView.OnItemClickListener onItemClickListener = (adapterView, view, i, l) -> {
        //Toast.makeText(getApplicationContext(), i + " is clicked", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, RideActivity.class);
        intent.putExtra("rideFileInfo", rideList.get(i));
        startActivity(intent);
    };

}