package com.gev.hw1;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.Core.DFT_SCALE;
import static org.opencv.core.Core.transform;
import static org.opencv.core.CvType.CV_8U;

public class Processing extends AppCompatActivity {

    private static final String TAG = "Processing";
    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private ImageView imageView;
    private Mat sampledImage=null;
    private final ArrayList<Mat> currentImage =new ArrayList<Mat>();
    private final ArrayList<Boolean> colors = new ArrayList<Boolean>();
    private final ArrayList<org.opencv.core.Point> corners=new ArrayList<org.opencv.core.Point>();
    private int imageNum = 0;
    private boolean useColor=true;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        imageView=(ImageView)findViewById(R.id.inputImageView);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Log.i(TAG, "event.getX(), event.getY(): " + event.getX() +" "+ event.getY());
                if(sampledImage!=null) {
                    Log.i(TAG, "sampledImage.width(), sampledImage.height(): " + sampledImage.width() +" "+ sampledImage.height());
                    Log.i(TAG, "view.getWidth(), view.getHeight(): " + view.getWidth() +" "+ view.getHeight());
                    int left=(view.getWidth()-sampledImage.width())/2;
                    int top=(view.getHeight()-sampledImage.height())/2;
                    int right=(view.getWidth()+sampledImage.width())/2;
                    int bottom=(view.getHeight()+sampledImage.height())/2;
                    Log.i(TAG, "left: " + left +" right: "+ right +" top: "+ top +" bottom:"+ bottom);
                    if(event.getX()>=left && event.getX()<=right && event.getY()>=top && event.getY()<=bottom) {
                        int projectedX = (int)event.getX()-left;
                        int projectedY = (int)event.getY()-top;
                        org.opencv.core.Point corner = new org.opencv.core.Point(projectedX, projectedY);
                        corners.add(corner);
                        if(corners.size()>4)
                            corners.remove(0);
                        Mat sampleImageCopy=sampledImage.clone();
                        for(org.opencv.core.Point c : corners)
                            Imgproc.circle(sampleImageCopy, c, (int) 5, new Scalar(0, 0, 255), 2);
                        displayImage(sampleImageCopy);
                    }
                }
                return false;
            }
        });
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
        else
            init();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                    Toast.makeText(getApplicationContext(),
                            "OpenCV error",
                            Toast.LENGTH_SHORT).show();
                } break;
            }
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init(){
        Bundle arguments = getIntent().getExtras();
        if(arguments!=null){
             String name = arguments.getString("name");
             if (name != null)
             {
                 Mat img = Imgcodecs.imread(name);
             }
        }
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            int status= ContextCompat.checkSelfPermission(this,permission);
            if (ContextCompat.checkSelfPermission(this,permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
            Map<String, Integer> perms = new HashMap<String, Integer>();
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                perms.put(permissions[i], grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                    allGranted = false;
            }
            // Check for ACCESS_FINE_LOCATION
            if (allGranted) {
                // All Permissions Granted
                init();
            } else {
                // Permission Denied
                Toast.makeText(Processing.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static final int SELECT_PICTURE = 1;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_openGallery:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),
                        SELECT_PICTURE);
                return true;

            case R.id.action_upsize:
                if(isImageLoaded()) {
                    resize(true);
                }
                return true;
            case R.id.action_downsize:
                if(isImageLoaded()) {
                    resize(false);
                }
                return true;
            case R.id.action_enhanced:
                if(isImageLoaded()) {
                    enhanced();
                }
                return true;

            case R.id.action_grayscale:
                if(isImageLoaded()) {
                    grayscale();
                }
                return true;
            case R.id.action_adaptiveBinary:
                if(isImageLoaded()) {
                    adaptiveBinary();
                }
                return true;
            case R.id.action_otsuBinary:
                if(isImageLoaded()) {
                    otsuBinary();
                }
                return true;
            case R.id.action_contrast:
                if(isImageLoaded()) {
                    contrast();
                }
                return true;
            case R.id.action_gamma:
                if(isImageLoaded()) {
                    gammaCorrection();
                }
                return true;
            case R.id.action_equalizehisto:
                if(isImageLoaded()) {
                    equalizeHisto();
                }
                return true;

            case R.id.action_filtering:
                if(isImageLoaded()) {
                    bilateral();
                }
                return true;
            case R.id.action_undo:
                if(isImageLoaded()) {
                    undo();
                }
                return true;

            case R.id.action_transform:
                if(isImageLoaded()) {
                    perspectiveTransform();
                }
                return true;
            case R.id.action_autotransform:
                if(isImageLoaded()) {
                    autoTransform();
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
    private boolean isImageLoaded(){
        if(sampledImage==null)
            Toast.makeText(getApplicationContext(),
                    "It is necessary to open image firstly",
                    Toast.LENGTH_SHORT).show();
        return sampledImage!=null;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_PICTURE && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData(); //The uri with the location of the file
            Log.d(TAG,"uri"+selectedImageUri);
            convertToMat(selectedImageUri);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void convertToMat(Uri selectedImageUri)
    {
        try {
            InputStream ims = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bmp= BitmapFactory.decodeStream(ims);
            Mat rgbImage=new Mat();
            Utils.bitmapToMat(bmp, rgbImage);
            ims.close();
            ims = getContentResolver().openInputStream(selectedImageUri);
            ExifInterface exif = new ExifInterface(ims);//selectedImageUri.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    1);
            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    //get the mirrored image
                    rgbImage=rgbImage.t();
                    //flip on the y-axis
                    Core.flip(rgbImage, rgbImage, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    //get up side down image
                    rgbImage=rgbImage.t();
                    //Flip on the x-axis
                    Core.flip(rgbImage, rgbImage, 0);
                    break;
            }

            Display display = getWindowManager().getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            double downSampleRatio= calculateSubSampleSize(rgbImage,width,height);
            sampledImage=new Mat();
            useColor = true;
            Imgproc.resize(rgbImage, sampledImage, new
                    Size(),downSampleRatio,downSampleRatio,Imgproc.INTER_AREA);
            displayImage(sampledImage);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e+" "+Log.getStackTraceString(e));
            sampledImage=null;
        }
    }
    private static double calculateSubSampleSize(Mat srcImage, int reqWidth,
                                                 int reqHeight) {
        final int height = srcImage.height();
        final int width = srcImage.width();
        double inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final double heightRatio = (double) reqHeight / (double) height;
            final double widthRatio = (double) reqWidth / (double) width;
            inSampleSize = heightRatio<widthRatio ? heightRatio :widthRatio;
        }
        return inSampleSize;
    }

    private void grayscale(){
        Mat grayImage=new Mat();
        useColor = false;
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);
        displayImage(grayImage);

    }
    private void resize(boolean up){
        Size old_size = sampledImage.size();
        Mat resized = new Mat();
        if (up)
        {
            Imgproc.resize(sampledImage, resized, new Size(old_size.width * 1.25, old_size.height * 1.25));
        }
        else
            {
                Imgproc.resize(sampledImage, resized, new Size(old_size.width * 0.75, old_size.height * 0.75));
            }
        displayImage(resized);
    }
    private void enhanced(){
        Mat rotated = autorotate();

        Mat grayImage = new Mat();
        Imgproc.cvtColor(rotated,grayImage, Imgproc.COLOR_RGB2GRAY);
        Mat out=new Mat();

        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(rotated, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);

            for(int channel=1;channel<=2;++channel) {
                Core.MinMaxLocResult minMaxLocRes = Core.minMaxLoc(hsv_list.get(channel));
                double minVal = minMaxLocRes.minVal;//+20;
                double maxVal = minMaxLocRes.maxVal;//-50;
                Mat corrected = new Mat();
                hsv_list.get(channel).convertTo(corrected, CV_8U, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
                hsv_list.set(channel, corrected);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Core.MinMaxLocResult minMaxLocRes = Core.minMaxLoc(grayImage);
            double minVal = minMaxLocRes.minVal;//+20;
            double maxVal = minMaxLocRes.maxVal;//-50;
            grayImage.convertTo(out, CV_8U, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
        }

        if (useColor) {
            Imgproc.cvtColor(out, grayImage, Imgproc.COLOR_RGB2GRAY);
            useColor = false;
        }
        else {
            grayImage = out;
        }

        Imgproc.GaussianBlur(grayImage,grayImage,new Size(5,5),0,0);
        Mat binImage = new Mat();
        Imgproc.adaptiveThreshold(grayImage, binImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 12);
        displayImage(binImage);
    }

    private Mat autorotate()
    {
        Mat binImage = new Mat();
        Mat grayImage = new Mat();
        if (useColor) {
            Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);
        }
        else {
            grayImage = sampledImage;
        }
        Imgproc.GaussianBlur(grayImage,grayImage,new Size(5,5),0,0);
        Imgproc.adaptiveThreshold(grayImage, binImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 12);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(50, 1));
        Imgproc.morphologyEx(binImage, binImage, Imgproc.MORPH_OPEN, kernel);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        double angles = 0;
        double count = 0;
        for (int i = 0; i < contours.size(); i++) {
            // Calculate the area of each contour
            double area = Imgproc.contourArea(contours.get(i));
            // Ignore contours that are too small or too large
            if (area < 1e2 || 1e5 < area)
                continue;
            count++;
            List<Point> pts = contours.get(i).toList();
            // Find the orientation of each shape
            int sz = pts.size();
            Mat dataPts = new Mat(sz, 2, CvType.CV_64F);

            double[] dataPtsData = new double[(int) (dataPts.total() * dataPts.channels())];
            for (int j = 0; j < dataPts.rows(); j++) {
                dataPtsData[j * dataPts.cols()] = pts.get(j).x;
                dataPtsData[j * dataPts.cols() + 1] = pts.get(j).y;
            }
            dataPts.put(0, 0, dataPtsData);
            // Perform PCA analysis
            Mat mean = new Mat();
            Mat eigenvectors = new Mat();
            Mat eigenvalues = new Mat();
            Core.PCACompute2(dataPts, mean, eigenvectors, eigenvalues);
            double[] eigenvectorsData = new double[(int) (eigenvectors.total() * eigenvectors.channels())];
            double[] eigenvaluesData = new double[(int) (eigenvalues.total() * eigenvalues.channels())];
            eigenvectors.get(0, 0, eigenvectorsData);
            eigenvalues.get(0, 0, eigenvaluesData);
            // Draw the principal components
            double[] meanData = new double[(int) (mean.total() * mean.channels())];
            mean.get(0, 0, meanData);
            // Store the eigenvalues and eigenvectors
            eigenvectors.get(0, 0, eigenvectorsData);
            eigenvalues.get(0, 0, eigenvaluesData);

            angles += Math.atan2(eigenvectorsData[1], eigenvectorsData[0]); // orientation in radians
        }
        angles = (angles/count) * 180 / Math.PI;
        Mat rot_mat = Imgproc.getRotationMatrix2D(new Point(grayImage.cols()/2, grayImage.rows()/2), angles, 1);
        Mat dst = new Mat();
        Imgproc.warpAffine(sampledImage, dst, rot_mat, sampledImage.size());
        return dst;
    }
    private void autoTransform(){
        displayImage(autorotate());//
    }

    private void adaptiveBinary(){
        Mat binImage = new Mat();
        Mat grayImage = new Mat();
        if (useColor) {
        Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);
        }
        else {grayImage = sampledImage;
        }
        Imgproc.GaussianBlur(grayImage,grayImage,new Size(5,5),0,0);

        Imgproc.adaptiveThreshold(grayImage, binImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 12);
        useColor = false;
        displayImage(binImage);
    }

    private void otsuBinary(){
        Mat binImage = new Mat();

        Mat grayImage = new Mat();
        if (useColor) {
            Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);
        }
        else {grayImage = sampledImage;
        }
        useColor = false;
        Imgproc.GaussianBlur(grayImage,grayImage,new Size(5,5),0,0);
        Imgproc.threshold(grayImage,binImage,0,255,Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);

        displayImage(binImage);
    }

    private void contrast(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);
        Mat out=new Mat();
        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);

            for(int channel=1;channel<=2;++channel) {
                Core.MinMaxLocResult minMaxLocRes = Core.minMaxLoc(hsv_list.get(channel));
                double minVal = minMaxLocRes.minVal;//+20;
                double maxVal = minMaxLocRes.maxVal;//-50;
                Mat corrected = new Mat();
                hsv_list.get(channel).convertTo(corrected, CV_8U, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
                hsv_list.set(channel, corrected);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Core.MinMaxLocResult minMaxLocRes = Core.minMaxLoc(grayImage);
            double minVal = minMaxLocRes.minVal;//+20;
            double maxVal = minMaxLocRes.maxVal;//-50;
            grayImage.convertTo(out, CV_8U, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
        }
        displayImage(out);
    }
    private void gammaCorrection(){
        double gammaValue = 1.3;
        Mat lookUpTable = new Mat(1, 256, CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);

        Mat out=new Mat();
        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);

            for(int channel=1;channel<=2;++channel) {
                Mat corrected = new Mat();
                Core.LUT(hsv_list.get(channel), lookUpTable, corrected);
                hsv_list.set(channel, corrected);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Mat grayImage = new Mat();
            Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);

            Core.LUT(grayImage, lookUpTable, out);
        }
        displayImage(out);
    }
    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }
    private void bilateral(){
        Mat outImage=new Mat();
        Mat rgb=new Mat();
        Imgproc.cvtColor(sampledImage, rgb, Imgproc.COLOR_RGBA2RGB);
        Imgproc.bilateralFilter(rgb,outImage,9,75,75);
        displayImage(outImage);
    }

    private void equalizeHisto(){
        Mat out=new Mat();
        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);
            for(int channel=1;channel<=2;++channel) {
                Mat equalizedValue = new Mat();
                Imgproc.equalizeHist(hsv_list.get(channel), equalizedValue);
                hsv_list.set(channel, equalizedValue);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Imgproc.cvtColor(sampledImage, out, Imgproc.COLOR_RGB2GRAY);
            Imgproc.equalizeHist(out, out);
        }
        displayImage(out);
    }

    private void undo(){
        if (imageNum > 1)
        {
        currentImage.remove(imageNum - 1);
        colors.remove(imageNum - 1);
        imageNum--;
        sampledImage = currentImage.get(imageNum - 1);
        useColor = colors.get(imageNum - 1);
        Bitmap bitmap = Bitmap.createBitmap(sampledImage.cols(),
                sampledImage.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(sampledImage, bitmap);
        displayImage(bitmap);}
    }

    private void perspectiveTransform(){
        if(corners.size()<4){
            Toast.makeText(getApplicationContext(),
                    "It is necessary to choose 4 corners",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        org.opencv.core.Point centroid=new org.opencv.core.Point(0,0);
        for(org.opencv.core.Point point:corners)
        {
            centroid.x+=point.x;
            centroid.y+=point.y;
        }
        centroid.x/=corners.size();
        centroid.y/=corners.size();

        sortCorners(corners,centroid);
        Mat correctedImage=new Mat(sampledImage.rows(),sampledImage.cols(),sampledImage.type());
        Mat srcPoints= Converters.vector_Point2f_to_Mat(corners);

        Mat destPoints=Converters.vector_Point2f_to_Mat(Arrays.asList(new org.opencv.core.Point[]{
                new org.opencv.core.Point(0, 0),
                new org.opencv.core.Point(correctedImage.cols(), 0),
                new org.opencv.core.Point(correctedImage.cols(),correctedImage.rows()),
                new org.opencv.core.Point(0,correctedImage.rows())}));

        Mat transformation=Imgproc.getPerspectiveTransform(srcPoints, destPoints);
        Imgproc.warpPerspective(sampledImage, correctedImage, transformation, correctedImage.size());

        corners.clear();
        displayImage(correctedImage);
    }
    void sortCorners(ArrayList<Point> corners, org.opencv.core.Point center)
    {
        ArrayList<org.opencv.core.Point> top=new ArrayList<org.opencv.core.Point>();
        ArrayList<org.opencv.core.Point> bottom=new ArrayList<org.opencv.core.Point>();

        for (int i = 0; i < corners.size(); i++)
        {
            if (corners.get(i).y < center.y)
                top.add(corners.get(i));
            else
                bottom.add(corners.get(i));
        }

        double topLeft=top.get(0).x;
        int topLeftIndex=0;
        for(int i=1;i<top.size();i++)
        {
            if(top.get(i).x<topLeft)
            {
                topLeft=top.get(i).x;
                topLeftIndex=i;
            }
        }

        double topRight=0;
        int topRightIndex=0;
        for(int i=0;i<top.size();i++)
        {
            if(top.get(i).x>topRight)
            {
                topRight=top.get(i).x;
                topRightIndex=i;
            }
        }

        double bottomLeft=bottom.get(0).x;
        int bottomLeftIndex=0;
        for(int i=1;i<bottom.size();i++)
        {
            if(bottom.get(i).x<bottomLeft)
            {
                bottomLeft=bottom.get(i).x;
                bottomLeftIndex=i;
            }
        }

        double bottomRight=bottom.get(0).x;
        int bottomRightIndex=0;
        for(int i=1;i<bottom.size();i++)
        {
            if(bottom.get(i).x>bottomRight)
            {
                bottomRight=bottom.get(i).x;
                bottomRightIndex=i;
            }
        }

        org.opencv.core.Point topLeftPoint = top.get(topLeftIndex);
        org.opencv.core.Point topRightPoint = top.get(topRightIndex);
        org.opencv.core.Point bottomLeftPoint = bottom.get(bottomLeftIndex);
        org.opencv.core.Point bottomRightPoint = bottom.get(bottomRightIndex);

        corners.clear();
        corners.add(topLeftPoint);
        corners.add(topRightPoint);
        corners.add(bottomRightPoint);
        corners.add(bottomLeftPoint);
    }
    private void displayImage(Mat image)
    {
        currentImage.add(image);
        colors.add(useColor);
        sampledImage = image;
        imageNum += 1;
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        displayImage(bitmap);
    }
    private void displayImage(Bitmap bitmap)
    {
        imageView.setImageBitmap(bitmap);
    }
}