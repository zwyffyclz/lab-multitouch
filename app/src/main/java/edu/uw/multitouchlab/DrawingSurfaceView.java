package edu.uw.multitouchlab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.HashMap;

/**
 * An example SurfaceView for generating graphics on
 * @author Joel Ross
 * @version Spring 2017
 */
public class DrawingSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "SurfaceView";

    private int viewWidth, viewHeight; //size of the view

    private Bitmap bmp; //image to draw on

    private SurfaceHolder mHolder; //the holder we're going to post updates to
    private DrawingRunnable mRunnable; //the code that we'll want to run on a background thread
    private Thread mThread; //the background thread

    private Paint whitePaint; //drawing variables (pre-defined for speed)
    private Paint goldPaint; //drawing variables (pre-defined for speed)

    private HashMap<Integer, Ball> touches;

    public Ball ball; //public for easy access

    /**
     * We need to override all the constructors, since we don't know which will be called
     */
    public DrawingSurfaceView(Context context) {
        this(context, null);
    }

    public DrawingSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingSurfaceView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);

        viewWidth = 1; viewHeight = 1; //positive defaults; will be replaced when #surfaceChanged() is called

        // register our interest in hearing about changes to our surface
        mHolder = getHolder();
        mHolder.addCallback(this);

        mRunnable = new DrawingRunnable();

        //set up drawing variables ahead of time
        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(Color.WHITE);
        goldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goldPaint.setColor(Color.rgb(145, 123, 76));

        touches = new HashMap<>();

        init();
    }

    // add new ball to touches
    public synchronized void addTouch(Integer id, float x, float y) {
        Ball newBall = new Ball(x, y, 100);
        touches.put(id, newBall);
    }

    // remove ball from touches
    public synchronized void removeTouch(Integer id) {
        touches.remove(id);
    }

    // change ball's configuration
    public synchronized void moveTouch(Integer id, float x, float y) {
        if (!touches.containsKey(id)) {
            addTouch(id, x, y);
        } else {
            Ball curBall = touches.get(id);
            curBall.setX(x);
            curBall.setY(y);
            touches.put(id, curBall);
        }
    }

    /**
     * Initialize graphical drawing state
     */
    public void init(){
        //make ball
        ball = new Ball(viewWidth/2, viewHeight/2, 100);
    }


    /**
     * Helper method for the "game loop"
     */
    public void update(){
        //update the "game state" here (move things around, etc)
    }


    /**
     * Helper method for the "render loop"
     * @param canvas The canvas to draw on
     */
    public synchronized void render(Canvas canvas){
        if(canvas == null) return; //if we didn't get a valid canvas for whatever reason

        canvas.drawColor(Color.rgb(51,10,111)); //purple out the background

        canvas.drawCircle(ball.cx, ball.cy, ball.radius, whitePaint); //we can draw directly onto the canvas

        for (Ball ball : touches.values()) {
            canvas.drawCircle(ball.getX(), ball.getY(), ball.getRadius(), goldPaint);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //create and start the background updating thread
        Log.d(TAG, "Creating new drawing thread");
        mThread = new Thread(mRunnable);
        mRunnable.setRunning(true); //turn on the runner
        mThread.start(); //start up the thread when surface is created

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        synchronized (mHolder) { //synchronized to keep this stuff atomic
            viewWidth = width;
            viewHeight = height;
            bmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888); //new buffer to draw on

            init();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        mRunnable.setRunning(false); //turn off
        boolean retry = true;
        while(retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
                //will try again...
            }
        }
        Log.d(TAG, "Drawing thread shut down");
    }

    /**
     * An inner class representing a runnable that does the drawing. Animation timing could go in here.
     * http://obviam.net/index.php/the-android-game-loop/ has some nice details about using timers to specify animation
     */
    public class DrawingRunnable implements Runnable {

        private boolean isRunning; //whether we're running or not (so we can "stop" the thread)

        public void setRunning(boolean running){
            this.isRunning = running;
        }

        public void run() {
            Canvas canvas;
            while(isRunning)
            {
                canvas = null;
                try {
                    canvas = mHolder.lockCanvas(); //grab the current canvas
                    synchronized (mHolder) {
                        update(); //update the game
                        render(canvas); //redraw the screen
                    }
                }
                finally { //no matter what (even if something goes wrong), make sure to push the drawing so isn't inconsistent
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}