package paintingapp.buildappwithhakhisadiarumkiayasha.com.touchpainting.view;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;


public class PikassoView extends View {

    public static final float TOUCH_TOLERANCE = 10;

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paintScreen;
    private Paint paintLine;
    private HashMap<Integer, Path> pathMap;
    private HashMap<Integer, Point>previousPointMap;

    public PikassoView(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        init();
    }

    void init(){
        paintScreen = new Paint();
        paintLine = new Paint();

        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(5);
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        pathMap = new HashMap<>();
        previousPointMap = new HashMap<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawBitmap(bitmap, 0,0, paintScreen);

        for(Integer key: pathMap.keySet()){
            canvas.drawPath(pathMap.get(key), paintLine);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP){

            touchStarted(event.getX(actionIndex),
            event.getY(actionIndex),
            event.getPointerId(actionIndex));


        }else if(action == MotionEvent.ACTION_UP  || action == MotionEvent.ACTION_POINTER_UP){
            touchEnded(event.getPointerId(actionIndex));

        }else{
            touchMoved(event);
        }

        invalidate(); //screen redraw korar jonno.
        return true;
    }

    private void touchMoved(MotionEvent event) {

        for(int i = 0; i<event.getPointerCount(); i++){

            int pointerId = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerId);

            if(pathMap.containsKey(pointerId)){
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                Path path = pathMap.get(pointerId);
                Point point = previousPointMap.get(pointerId);

                //calculating user's last movement
                float deltaX  = Math.abs(newX-point.x);
                float deltaY  = Math.abs(newY-point.y);

                //if the distance is enough to call a movement then-
                if(deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE){
                    //move that path to new location
                    path.quadTo(point.x, point.y, (newX + point.x)/2,(newY + point.y)/2);

                    //storing the new coordinates
                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }
        }
    }

    public void setDrawingColor(int color){
        paintLine.setColor(color);
    }

    public int getDrawingColor(){
        return paintLine.getColor();
    }

    public void setLineWidth (int width){
        paintLine.setStrokeWidth(width);
    }

    public int getLineWidth(){
        return (int) paintLine.getStrokeWidth();
    }

    public void clear(){
        pathMap.clear(); //aager path remove kore.
        previousPointMap.clear();
        bitmap.eraseColor(Color.WHITE);
        invalidate(); // refreshing screen.
    }

    private void touchEnded(int pointerId) {
        Path path = pathMap.get(pointerId); //corrosponding patha pawar jonno.
        bitmapCanvas.drawPath(path, paintLine); //bitmalCamvas a draw korar jonno.
        path.reset();
    }

    private void touchStarted(float x, float y, int pointerId) {
        Path path; //touch er path store kore
        Point point; //path er last point store kore

        if(pathMap.containsKey(pointerId)){
            path = pathMap.get(pointerId);
            point = previousPointMap.get(pointerId);

        }else{
            path = new Path();
            pathMap.put(pointerId, path);
            point = new Point();
            previousPointMap.put(pointerId, point);
        }
        //touch er coordinates move korar jonno
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
    }

    public void saveImage(){
        String fileName = "Dagaw" + System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");

        //file save er location er URI pawar jonno
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);

        try {
            OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri);
            //bitmap copy kore output stream a rakhar jonno
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream); // eitai amader saved image

            try {
                outputStream.flush();
                outputStream.close();

                Toast message = Toast.makeText(getContext(), "Image Saved!", Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
                message.show();

            } catch (IOException e) {
                Toast message = Toast.makeText(getContext(), "Image Not Saved!", Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
                message.show();
                //e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            Toast message = Toast.makeText(getContext(), "Image Not Saved!", Toast.LENGTH_LONG);
            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
            message.show();
            //e.printStackTrace();
        }
    }

    public void saveImageToExternalStorage() {

        String filename = "Dagaw" + System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");


        // get a URI for the location to save the file
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);


        OutputStream outputStream = null;
        try {
            outputStream =
                    getContext().getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + "test.jpeg");

        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);


        try {
            outputStream.flush();
            outputStream.close();

            Toast message = Toast.makeText(getContext(), "Image Saved", Toast.LENGTH_LONG);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                    message.getYOffset() / 2);
            message.show();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            MediaStore.Images.Media.insertImage(getContext().getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getImagePath() {
        ContextWrapper cw = new ContextWrapper(getContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        return directory.getAbsolutePath();
    }

// uporer 3 ta method  kaje lage nai

    public void saveToInternalStorage() {
        ContextWrapper cw = new ContextWrapper(getContext());
        String filename = "Dagaw" + System.currentTimeMillis();

        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);

        File mypath = new File(directory,  "profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
                Log.d("Image:", directory.getAbsolutePath());
                Toast message = Toast.makeText(getContext(), "Image Saved +" + directory.getAbsolutePath(), Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
                message.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadImageFromStorage(String path) {

        try {
            File f = new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}

