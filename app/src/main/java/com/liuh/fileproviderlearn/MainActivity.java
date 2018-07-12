package com.liuh.fileproviderlearn;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
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
    private static final int REQUEST_CODE_PICK_PHOTO = 0X112;

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

    @OnClick({R.id.btn_take_pic, R.id.btn_choose_pic, R.id.btn_install_apk})
    void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_take_pic:
                //拍照并获取一张图片
                takePhotoNoCompress();
                break;
            case R.id.btn_choose_pic:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
                break;
            case R.id.btn_install_apk:
                //安装一个apk文件
                installApk();
                break;
        }
    }

    public void takePhotoNoCompress() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            String filename = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA)
                    .format(new Date()) + ".png";
            Log.e("------", "Environment.getExternalStorageDirectory() : " + Environment.getExternalStorageDirectory());
            Log.e("------", "this.getFilesDir().toString() : " + this.getFilesDir().toString());
            Log.e("------", "this.getCacheDir().toString() : " + this.getCacheDir().toString());

            File file = new File(Environment.getExternalStorageDirectory(), filename);
            mCurrentPhotoPath = file.getAbsolutePath();

            //当我们需要得到一张拍照图的时候，需要通过Intent传递一个File的Uri给相机应用
            //当我们使用Android7.0及以上系统版本时，这里会发生crash
            //android.os.FileUriExposedException: file:///storage/emulated/0/20180627-160841.png exposed beyond app through ClipData.Item.getUri()

            //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            //startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);

            //为了对旧版本进行兼容
            Uri fileUri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(this, "com.liuh.fileproviderlearn.fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

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
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            String filePath = getRealPathFromUri2(uri);

            Log.e("-------", "filePath: " + filePath);
        }
    }

    private String getRealPathFromUri(Uri contentUri) {
        String result = null;
        Cursor cursor = null;

        Log.e("-------", " contentUri.toString(): " + contentUri.toString());

        try {
            cursor = getContentResolver().query(contentUri, null, null, null, null);

            if (cursor == null) {
                result = contentUri.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                result = cursor.getString(idx);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private String getRealPathFromUri2(Uri contentUri) {
        boolean isAboveKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        //DocumentProvider
        if (isAboveKitKat && DocumentsContract.isDocumentUri(this, contentUri)) {


            if (isExternalStorageDocument(contentUri)) {
                //ExternalStorageProvider
                String docId = DocumentsContract.getDocumentId(contentUri);
                String[] split = docId.split(":");
                String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                //TODO handle non-primary volumes

            } else if (isDownloadsDocument(contentUri)) {
                //DownloadsProvider
                String id = DocumentsContract.getDocumentId(contentUri);
                Uri uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id)
                );
                return getDataColumn(this, uri, null, null);
            } else if (isMediaDocument(contentUri)) {
                //MediaProvider
                String docId = DocumentsContract.getDocumentId(contentUri);
                String[] split = docId.split(":");
                String type = split[0];

                Uri uri = null;

                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                String selection = "_id=?";
                String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(this, uri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(contentUri.getScheme())) {
            //MediaStore ( and general )
            return getDataColumn(this, contentUri, null, null);
        } else if ("file".equalsIgnoreCase(contentUri.getScheme())) {
            //File
            return contentUri.getPath();
        }

        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;

        String column = "_data";

        String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToNext()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
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

    public void installApk() {
        File file = new File(Environment.getExternalStorageDirectory(), "Nfc.apk");
        Log.e("-------------", "file.exists() : " + file.exists());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(this, "com.liuh.fileproviderlearn.fileprovider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }

        intent.setDataAndType(fileUri,
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivity(intent);
    }


    /**
     * @param uri The Uri to check
     * @return Whether the Uri authority is ExternalStorageProvider
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check
     * @return Whether the Uri authority is MediaProvider
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
