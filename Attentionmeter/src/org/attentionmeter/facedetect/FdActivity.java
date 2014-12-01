package org.attentionmeter.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    GREEN     = new Scalar(0, 255, 0, 255);
    private static final Scalar    RED     = new Scalar(255, 0, 0, 255);
    private static final Scalar    BLUE     = new Scalar(0, 0, 255, 255);
    private static final Scalar    ORANGE     = new Scalar(255, 103, 0,255);
    private static final Scalar    MAGENTA     = new Scalar(255, 0, 255, 255);
    private static final Scalar    YELLOW     = new Scalar(255, 255, 0, 255);
    private static final double    image_scale = 1;
    private static final int       scale = 1;
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;
    public static final int        BACK_CAMERA       = 0;
    public static final int        FRONT_CAMERA     = 1;

    private MenuItem               CameraOrientation;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;
    
    private int                    mDetectorTypeCamera       = FRONT_CAMERA;
    private String[]               mDetectorNameCamera;
    
    

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;
    
    public TextView ecgValue;
    public TextView gsrValue;
    public TextView eegValue;
    public TextView emgValue;
    public TextView ipValue;
    
    LinkedList<Face> trackedFaces;
    
    Server mServer;
    
    boolean mBounded;
    
    private Intent intent;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");
                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
        
        mDetectorNameCamera = new String[2];
        mDetectorNameCamera[BACK_CAMERA] = "Back Camera";
        mDetectorNameCamera[FRONT_CAMERA] = "Front Camera";
        
        trackedFaces = new LinkedList<Face>();

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        this.ecgValue = (TextView) findViewById(R.id.ecgValue);
        this.gsrValue = (TextView) findViewById(R.id.gsrValue);
        this.eegValue = (TextView) findViewById(R.id.eegValue);
        this.emgValue = (TextView) findViewById(R.id.emgValue);
        this.ipValue = (TextView) findViewById(R.id.ipServer);
        
        processStartService(Server.TAG);        
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        CameraOrientation   = menu.add(mDetectorNameCamera[mDetectorTypeCamera]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == CameraOrientation){
        	int tmpDetectorType = (mDetectorTypeCamera + 1) % 2;
            item.setTitle(mDetectorNameCamera[tmpDetectorType]);
            setCamera(mDetectorTypeCamera);
            onPause();
            onCameraViewStopped();
            onResume();
            onCameraViewStarted(0,0);
        }
        	
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        
        //unregisterReceiver(broadcastReceiver);
        //stopService(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        
        startService(intent);
        registerReceiver(broadcastReceiver, new IntentFilter(Server.BROADCAST_ACTION));
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();
        MatOfRect facesFliped = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }
        
        Core.flip(faces, facesFliped, 1);
        

        Rect[] facesArray = facesFliped.toArray();
        for (int i = 0; i < facesArray.length; i++){
        	
        	//Ages all trackedFaces
        	for (Face f : trackedFaces){
        		f.updateLife();
        	}
        	
    		//Remove expired faces
        	LinkedList<Face> trackedFacesTemp = new LinkedList<Face>();
        	for (Face f : trackedFaces){
        		if (! f.isTooOld()){
        			trackedFacesTemp.add(f);
                    
        		}
        	}
        	
        	trackedFaces.clear();
        	
        	if (trackedFacesTemp.size() > 0){
        		trackedFaces = trackedFacesTemp;
        	}

            boolean matchedFace = false;
            Face mf = null;
            
            Point pt1 = facesArray[i].tl();
            Point pt2 = facesArray[i].br();
            
            //check if there are trackedFaces
            if (trackedFaces.size() > 0){
                //each face being tracked
            	for (Face f : trackedFaces){
                    //the face is found (small movement)
                    if ((Math.abs(f.xpt - pt1.x) < Face.FACE_MAX_MOVEMENT) && (Math.abs(f.ypt - pt1.y) < Face.FACE_MAX_MOVEMENT)){
                        matchedFace = true;
                        f.updateFace((int)(facesArray[i].width * image_scale), (int)(facesArray[i].height * image_scale), (int)pt1.x, (int)pt1.y);
                        mf = f;
                        break;
                    }
                }  
                //if face not found, add a new face
                if (matchedFace == false){
                	Face f = new Face(0,(int)(facesArray[i].width * image_scale), (int)(facesArray[i].height * image_scale), (int)pt1.x, (int)pt1.y,0);
                    trackedFaces.add(f);
                    mf = f;
                }
            }
            //No tracked faces: adding one                            
            else{
            	Face f = new Face(0,(int)(facesArray[i].width * image_scale), (int)(facesArray[i].height * image_scale), (int)pt1.x, (int)pt1.y,0);
                trackedFaces.add(f);
                mf = f;
            }
            //where to draw face and properties
            if (mf.age > 5){
                //draw attention line
                Point lnpt1 = new Point (mf.xpt * scale, (mf.ypt * scale - 5) - 5);
                Point lnpt2;
                if (mf.age > mf.width){
                    lnpt2 = new Point (mf.xpt * scale + mf.width, mf.ypt * scale - 5);
                }
                else{
                    lnpt2 = new Point (mf.xpt * scale + mf.age, mf.ypt * scale - 5);
                }

                //drawing bold attention line
                Core.rectangle(mRgba, lnpt1, lnpt2, RED, 10, 8, 0);
                
                //drawing face
                Core.rectangle(mRgba, pt1, pt2, getColor(mf), 3, 8, 0 );
                
                //drawing eyes
                Core.rectangle(mRgba, mf.eyeLeft1, mf.eyeLeft2, MAGENTA, 3,8,0);
                Core.rectangle(mRgba, mf.eyeRight1, mf.eyeRight2, MAGENTA, 3,8,0);                
                
                //drawing mouth
                Core.rectangle(mRgba, mf.mouthTopLeft, mf.mouthBotRight, ORANGE, 3, 8, 0);
                
            }
        	
        }
        
        //Creating squares for information
        Point PointTopLeft;
        Point PointBotRight;
        
        PointTopLeft = new Point(100, 100);
        PointBotRight = new Point(300,200);
        Core.rectangle(mRgba, PointTopLeft, PointBotRight, ORANGE, 3, 8, 0);        
        
        PointTopLeft = new Point(PointTopLeft.x, PointBotRight.y + 100);
        PointBotRight = new Point(PointBotRight.x, PointBotRight.y + 200);
        Core.rectangle(mRgba, PointTopLeft, PointBotRight, ORANGE, 3, 8, 0);
        
        PointTopLeft = new Point(PointTopLeft.x, PointBotRight.y + 100);
        PointBotRight = new Point(PointBotRight.x, PointBotRight.y + 200);
        Core.rectangle(mRgba, PointTopLeft, PointBotRight, ORANGE, 3, 8, 0);
        
        PointTopLeft = new Point(PointTopLeft.x, PointBotRight.y + 100);
        PointBotRight = new Point(PointBotRight.x, PointBotRight.y + 200);
        Core.rectangle(mRgba, PointTopLeft, PointBotRight, ORANGE, 3, 8, 0);

        return mRgba;
    }
    
    private void setCamera(int camera) {
    	if (camera == BACK_CAMERA) {
            Log.i(TAG, "Back camera set");
            mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
            mDetectorTypeCamera = FRONT_CAMERA;
        } else {
            Log.i(TAG, "Front camera set");
            mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
            mDetectorTypeCamera = BACK_CAMERA;
        }
    }
    
    Scalar getColor(Face mf){
        if (mf.isNodding()) return GREEN;
        else if (mf.isShaking()) return RED;
        else if (mf.isStill()) return BLUE;
        else return YELLOW;
    }
    
    private void processStartService(final String tag) {
        intent = new Intent(getApplicationContext(), Server.class);
        intent.addCategory(tag);
        startService(intent);
    }
    
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	final String ecg = intent.getStringExtra("ECG");
        	final String gsr = intent.getStringExtra("GSR");
        	final String eeg = intent.getStringExtra("EEG");
        	final String emg = intent.getStringExtra("EMG");
        	final String ip = intent.getStringExtra("IP");
        	runOnUiThread(new Runnable() {
        		@Override
        		public void run() {
        			
        			ecgValue.setText(ecg);
        			gsrValue.setText(gsr);
        			eegValue.setText(eeg);
        			emgValue.setText(emg);
        			ipValue.setText(ip);        			

        		}
        	});      
        }
    };
    
}
