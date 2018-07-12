# FileProviderLearn
【学习笔记】Android 7.0行为变更：FileProvider的使用
>来源为文章[Android 7.0 行为变更 通过FileProvider在应用间共享文件吧](https://blog.csdn.net/lmj623565791/article/details/72859156)<br>
>在官方7.0的以上的系统中，尝试传递 file://URI可能会触发FileUriExposedException
#### 一 . 拍照案例
当我们使用手机拍照并希望获取拍照后的高清图片时，我们会通过Intent传递一个File的Uri给相机应用。下面的写法是适配了Android 7.0及更高版本的，如果没有适配，在Android7.0及更高版本上运行会报错(android.os.FileUriExposedException)：<br>
``` java
06-27 21:54:31.619 1793-1793/com.liuh.fileproviderlearn E/AndroidRuntime: FATAL EXCEPTION: main
                                                                          Process: com.liuh.fileproviderlearn, PID: 1793
                                                                          android.os.FileUriExposedException: file:///storage/emulated/0/20180627-215431.png exposed beyond app through ClipData.Item.getUri()
                                                                          
```
出现这个Crash的原因，官网做了这种解释：
>对于面向 Android 7.0 的应用，Android 框架执行的 StrictMode API 政策禁止在您的应用外部公开 file:// URI。如果一项包含文件 URI 的 intent 离开您的应用，则应用出现故障，并出现 FileUriExposedException 异常。

同样，官网也给出了解决方案：
>要在应用间共享文件，您应发送一项 content:// URI，并授予 URI 临时访问权限。进行此授权的最简单方式是使用 FileProvider 类。如需了解有关权限和共享文件的详细信息，请参阅共享文件。 
https://developer.android.com/about/versions/nougat/android-7.0-changes.html#accessibility
最终写法：<br>
``` java
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
```
接收照片数据：<br>
``` java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            ivPic.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
        }
    }
```

对于xml/file_paths文件的解释：<br>
* \<root-path\/\>代表设备的根目录new File("/");
* \<files-path\/\>代表context.getFilesDir()
* \<cache-path\/\>代表context.getCacheDir()
* \<external-path\/\>代表Environment.getExternalStorageDirectory()
* \<external-files-path\/\>代表context.getExternalFilesDirs()
* \<external-cache-path\/\>代表getExternalCacheDirs()

path即为代表目录下的子目录，比如：
``` xml
<external-path
        name="external"
        path="pics" />
```
代表的目录即为：Environment.getExternalStorageDirectory()/pics，其他同理

#### 二 . 使用FileProvider兼容安装apk
在Android7.0之前，我们编写安装apk的时候，一般是这样：<br>
```java
public void installApk(View view) {
    File file = new File(Environment.getExternalStorageDirectory(), "testandroid7-debug.apk");

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(Uri.fromFile(file),
            "application/vnd.android.package-archive");
    startActivity(intent);
}
```
这种写法到了Android7.0及更高版本上也是会报错的：android.os.FileUriExposedException.<br>
最终写法：<br>
```java
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
```

#### 三 . 总结
使用content://替代file://，主要需要FileProvider的支持，而因为FileProvider是ContentProvider的子类，所以需要在AndroidManifest.xml中注册；而又因为需要对真实的filepath进行映射，所以需要编写一个xml文档，用于描述可使用的文件夹目录，以及通过name去映射该文件夹目录。<br>

对于权限，有两种方式：
* 方式一为Intent.addFlags，该方式主要用于针对intent.setData，setDataAndType以及setClipData相关方式传递uri的。
* 方式二为grantUriPermission来进行授权

相比来说方式二较为麻烦，因为需要指定目标应用包名，很多时候并不清楚，所以需要通过PackageManager进行查找到所有匹配的应用，全部进行授权。不过更为稳妥~<br>
方式一较为简单，对于intent.setData，setDataAndType正常使用即可，但是对于setClipData，由于5.0前后Intent#migrateExtraStreamToClipData，代码发生变化，需要注意~<br>

#### 四 . 作者提供了一种快速适配的方案，即编写一个library类型的module,在项目中需要用到FileProvider的适配时，进行引用使用。这个就不写了，见原文吧。

#### 五 . 今天在做一个项目时里面有一个地方需要选取一张手机中的图片（百度离线人脸识别的demo，但是发现选择从文件管理里面选取图片时会报错，弄了好久，还好在下班前找到了解决方案，这里记录一下）
最初的代码：<br>
```java
  //使用隐式意图，调起系统的选取图片功能
  Intent intent = new Intent(Intent.ACTION_PICK);
  intent.setType("image/*");
  startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
```
这样的话，会调起一个选择框，让我们选择要使用的应用：文件管理；相册。

如果使用相册的话，在数据接收时，下面这种写法是没有问题的：<br>
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AUTO_DETECT && data != null) {
            faceImagePath = data.getStringExtra("file_path");

            Bitmap bitmap = BitmapFactory.decodeFile(faceImagePath);
            ivAvatar.setImageBitmap(bitmap);
            btnRegist.setVisibility(View.VISIBLE);
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String filePath = getRealPathFromURI(uri);
            detect(filePath);
        }
    }
    
    private String getRealPathFromURI(Uri contentURI) {
            String result = null;
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(contentURI, null, null, null, null);
    
                if (cursor == null) { // Source is Dropbox or other similar local file path
                    result = contentURI.getPath();
                } else {
                    cursor.moveToFirst();
    
                    int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    result = cursor.getString(idx);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
    
            return result;
        }
```
但是如果我们选择了文件管理，那么在数据接收的时候，会报下面这个错误：<br>
```java
Process: com.newabel.facerecognize_usebaidu, PID: 6123
         java.lang.RuntimeException: Failure delivering result ResultInfo{who=null, request=1000, result=-1, data=Intent { dat=content://com.android.fileexplorer.myprovider/external_files/faces_0711/卢振杨[01302789].jpg flg=0x1 }} to activity {com.newabel.facerecognize_usebaidu/com.newabel.facerecognize_usebaidu.usermanage.UserSingleAddActivity}: java.lang.IllegalStateException: Couldn't read row 0, col -1 from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.
         at android.app.ActivityThread.deliverResults(ActivityThread.java:4224)
         at android.app.ActivityThread.handleSendResult(ActivityThread.java:4267)
         at android.app.ActivityThread.-wrap20(ActivityThread.java)
         at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1616)
         at android.os.Handler.dispatchMessage(Handler.java:102)
         at android.os.Looper.loop(Looper.java:163)
         at android.app.ActivityThread.main(ActivityThread.java:6396)
         at java.lang.reflect.Method.invoke(Native Method)
         at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:904)
         at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:794)
     Caused by: java.lang.IllegalStateException: Couldn't read row 0, col -1 from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.
         at android.database.CursorWindow.nativeGetString(Native Method)
         at android.database.CursorWindow.getString(CursorWindow.java:438)
         at android.database.AbstractWindowedCursor.getString(AbstractWindowedCursor.java:51)
         at android.database.CursorWrapper.getString(CursorWrapper.java:137)
         at com.newabel.facerecognize_usebaidu.usermanage.UserSingleAddActivity.getRealPathFromURI(UserSingleAddActivity.java:189)
         at com.newabel.facerecognize_usebaidu.usermanage.UserSingleAddActivity.onActivityResult(UserSingleAddActivity.java:173)
         at android.app.Activity.dispatchActivityResult(Activity.java:7139)
         at android.app.ActivityThread.deliverResults(ActivityThread.java:4220)
         at android.app.ActivityThread.handleSendResult(ActivityThread.java:4267) 
         at android.app.ActivityThread.-wrap20(ActivityThread.java) 
         at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1616) 
         at android.os.Handler.dispatchMessage(Handler.java:102) 
         at android.os.Looper.loop(Looper.java:163) 
         at android.app.ActivityThread.main(ActivityThread.java:6396) 
         at java.lang.reflect.Method.invoke(Native Method) 
         at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:904) 
         at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:794) 
```
我们打印两种情况下的Uri，会发现：<br>

相册：<br>
>content://com.miui.gallery.open/raw/%2Fstorage%2Femulated%2F0%2FDCIM%2FCamera%2FIMG_20180703_152243.jpg<br>

文件管理：<br>
>content://com.android.fileexplorer.myprovider/external_files/faces_0711/%E5%8D%A2%E6%8C%AF%E6%9D%A8%5B01302789%5D.jpg<br>

上面的那种解析Uri的方式对相册来说是正常可用的；但是对于文件管理来说，则是不能正常使用的。<br>

网上找了找资料，找到一篇文章[Android使用系统文件管理器选择文件，并将Uri转换为File](https://blog.csdn.net/weixin_40255793/article/details/79496076),按照其所讲，把代码敲下来，运行了一下，可以正常使用。<br>
这篇文章基本也是组合的别人的成果，作者也给出了借鉴的出处。<br>
[Android调用系统自带的文件管理器进行文件选择](https://blog.csdn.net/Gen_Ye/article/details/53537139 )<br>
[Get real path from Uri, Android KitKat new storage access framework](https://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework/20559175#20559175)

这里把代码也给贴上来吧。<br>
```java
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
```
大概是这么大一长串。这里记录一下。<br>




