package top.wqis.speedometer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.UriUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import pub.devrel.easypermissions.EasyPermissions;

public class SettingsActivity extends AppCompatActivity {

    private final String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        assert getSupportActionBar() != null;
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.setttings) + "</font>"));
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.light_green)));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.light_green));

        // add event listeners
        findViewById(R.id.gps_settings_card).setOnClickListener(view -> openGpsSettings(null));
        findViewById(R.id.permissions_card).setOnClickListener(view -> openPermissionSettings(null));
        findViewById(R.id.import_data_card).setOnClickListener(view -> importData(null));
        findViewById(R.id.export_data_card).setOnClickListener(view -> exportData(null));
        findViewById(R.id.about_card).setOnClickListener(view -> openAppInfo(null));
        findViewById(R.id.operator_mode_card).setOnClickListener(v -> operatorModeSwitch(null));
    }

    public void operatorModeSwitch(View view){
        SharedPreferences sp = getApplicationContext().getSharedPreferences("operator", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        // 定义UI
        TextView opModeText = findViewById(R.id.operator_mode_title_tv);
        ImageView opModeIcon = findViewById(R.id.operator_mode_icon_iv);

        // 当opMode == 0, 是电单车模式。当opMode == 1，是自行车模式
        int opMode = sp.getInt("operator_mode", 0);

        Log.i("Current opMode", String.valueOf(opMode));

        if (opMode == 1){
            // 切换到自行车模式
            opMode = 0;

            opModeText.setText("自行车模式");
            opModeIcon.setImageResource(R.drawable.icon_bike_top);

            Log.i("opMode changed to", "自行车模式");
        }
        else{
            // 切换到摩托车模式
            opMode = 1;

            opModeText.setText("电单车模式");
            opModeIcon.setImageResource(R.drawable.icon_motorcycle);
            Log.i("opMode changed to", "电单车模式");
        }

        // 写入新的模式
        editor.putInt("operator_mode", opMode);
        editor.commit();

        Toast.makeText(getApplicationContext(), "骑行模式已变更", Toast.LENGTH_SHORT).show();
    }

    public void openGpsSettings(View view){
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    public void openPermissionSettings(View view){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public void openAppInfo(View view){
        try{
            String appVersionName = getPackageManager().getPackageInfo(getPackageName(),0).versionName;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.app_info));
            builder.setMessage(getString(R.string.app_name) + " V" + appVersionName + "\n" + getString(R.string.copyright) + "\n" + "图标来自icons8.com");
            builder.setCancelable(true);
            builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {

            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.package_info_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void importData(View view){
        // Check permissions
        if(EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)){

            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("text/plain");

            Intent intent = Intent.createChooser(chooseFile, getString(R.string.choose_import_file));

            try{
                startActivityForResult(intent, 1);
            }
            catch (ActivityNotFoundException e){
                Toast.makeText(getApplicationContext(), getString(R.string.cannot_find_file_manager), Toast.LENGTH_SHORT).show();
            }
        }
        else{
            EasyPermissions.requestPermissions(this, getString(R.string.need_file_perms), 3, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        String path = "";

        if (requestCode == 1) {
            Uri uri = data.getData();

            if (uri != null){
                File file = UriUtils.uri2File(uri);
                Log.i("File path", file.getAbsolutePath());
                String date;

                // Read first line of file to get time
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    date = reader.readLine();
                    reader.close();

                    // Create destination file
                    File destFile = new File(getFilesDir() + "/rideInfo", date + ".txt");

                    if(!destFile.exists()){
                        try {
                            destFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Copy files
                    org.apache.commons.io.FileUtils.copyFile(file,destFile);

                    Toast.makeText(getApplicationContext(), getString(R.string.file_import_ok), Toast.LENGTH_SHORT).show();

                    // Remove the old file
                    file.delete();

                    startActivity(new Intent(this, RideListActivity.class));
                    finish();

                } catch (IOException e) {
                    Toast.makeText(this,  getString(R.string.file_import_failed) + "：" + e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    public void exportData(View view){
//        // Check permissions
        if(EasyPermissions.hasPermissions(this, perms)){

            File destPath;

            // Check system version, if system version > android 10, then a separate dir is required.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                destPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "speedometer" + File.separator);
            }
            else{
                destPath = new File(Environment.getExternalStorageDirectory() + File.separator + "speedometer" + File.separator);
            }

            if (!destPath.exists())
                destPath.mkdirs();

            // Copy files to external storage directory
            File originPath = new File(getFilesDir() + "/rideInfo");

            if (!originPath.exists()){
                Toast.makeText(getApplicationContext(), getString(R.string.no_data_folder), Toast.LENGTH_SHORT).show();
            }
            else if(originPath.listFiles().length == 0){
                Toast.makeText(getApplicationContext(), getString(R.string.no_data), Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getApplicationContext(), getString(R.string.exporting), Toast.LENGTH_SHORT).show();

                new Thread(() -> {
                    try{
                        for (File f: originPath.listFiles()
                        ) {
                            Log.i("File in list", f.getAbsolutePath());
                            try {
                                Log.i("File in list name", f.getName());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Create variables and copy files
                            File destFile = new File(destPath, f.getName());

                            // Copy files
                            try {
                                org.apache.commons.io.FileUtils.copyFile(f, destFile);

                                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "文件已导出至 " + destPath.getPath(), Toast.LENGTH_SHORT).show());

                            } catch (IOException e) {
                                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "导出错误：" + e.getMessage(), Toast.LENGTH_LONG).show());
                                e.printStackTrace();
                            }
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }
        }
        else{
            EasyPermissions.requestPermissions(this, getString(R.string.need_file_perms), 2, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}