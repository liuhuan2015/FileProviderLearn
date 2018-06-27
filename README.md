# FileProviderLearn
Android 7.0行为变更：FileProvider的使用
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
<root-path/> 代表设备的根目录new File("/");
<files-path/> 代表context.getFilesDir()
<cache-path/> 代表context.getCacheDir()
<external-path/> 代表Environment.getExternalStorageDirectory()
<external-files-path>代表context.getExternalFilesDirs()
<external-cache-path>代表getExternalCacheDirs()



