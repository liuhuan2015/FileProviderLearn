package com.liuh.fileproviderlearn;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * FileProvider
 * 文章：https://blog.csdn.net/lmj623565791/article/details/72859156
 * <p>
 * Android 7.0(api 24)行为变更 FileProvider
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.iv_pic)
    ImageView ivPic;

    private static final int REQUEST_CODE_PERMISSION_CAMERA = 0x110;
    private static final int REQUEST_CODE_TAKE_PHOTO = 0x111;

    private String mCurrentPhotoPath;

    static final String[] PERMISSIONS_REQUEST_ALL = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    List<String> permissions_request_denied = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < PERMISSIONS_REQUEST_ALL.length; i++) {
                if (ContextCompat.checkSelfPermission(this, PERMISSIONS_REQUEST_ALL[i]) != PackageManager.PERMISSION_GRANTED) {
                    //如果未授权
                    permissions_request_denied.add(PERMISSIONS_REQUEST_ALL[i]);
                }
            }

            if (permissions_request_denied.size() > 0) {
                for (int i = 0; i < permissions_request_denied.size(); i++) {
                    ActivityCompat.requestPermissions(this,
                            permissions_request_denied.toArray(new String[permissions_request_denied.size()]),
                            REQUEST_CODE_PERMISSION_CAMERA);
                }
            }
        }
    }

    @OnClick({R.id.btn_take_pic})
    void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_take_pic:
                takePhotoNoCompress();
                break;
        }
    }

    public void takePhotoNoCompress() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            String filename = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA)
                    .format(new Date()) + ".png";
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            mCurrentPhotoPath = file.getAbsolutePath();

            //当我们需要得到一张拍照图的时候，需要通过Intent传递一个File的Uri给相机应用
            //当我们使用Android7.0及以上系统版本时，这里会发生crash
            //android.os.FileUriExposedException: file:///storage/emulated/0/20180627-160841.png exposed beyond app through ClipData.Item.getUri()

            //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            //startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);

            Uri fileUri = FileProvider.getUriForFile(this, "com.liuh.fileproviderlearn.fileprovider", file);
            Log.e("------", fileUri.toString());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            ivPic.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "相机权限授权成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "相机权限授权失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
