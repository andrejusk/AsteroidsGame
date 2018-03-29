package uk.ac.reading.student.akostarevas.asteroids;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

@SuppressWarnings("WeakerAccess")
public abstract class GameThread extends Thread {
    //Different mode states
    //TODO: what are enums
    public static final int STATE_LOSE = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_RUNNING = 4;
    public static final int STATE_WIN = 5;
    //Used to ensure appropriate threading
    static final Integer monitor = 1;
    //The view
    public GameView gameView;
    //Control variable for the mode of the game (e.g. STATE_WIN)
    protected int mode = 1;
    //We might want to extend this call - therefore protected
    protected int canvasWidth = 1;
    protected int canvasHeight = 1;
    //Last time we updated the game physics
    protected long lastUpdate = 0;
    protected long score = 0;
    Paint background;
    //Control of the actual running inside run()
    private boolean running = false;
    //The surface this thread (and only this thread) writes upon
    private SurfaceHolder surfaceHolder;
    //the message handler to the View/Activity thread
    private Handler handler;
    //Android Context - this stores almost all we need to know
    private Context context;
    //Used for time keeping
    private long now;
    private float elapsed;

    public GameThread(GameView gameView) {
        this.gameView = gameView;

        surfaceHolder = gameView.getHolder();
        handler = gameView.handler;
        context = gameView.getContext();

        background = new Paint();
        background.setColor(Color.BLACK);
    }

    /*
     * Called when app is destroyed, so not really that important here
     * But if (later) the game involves more thread, we might need to stop a thread, and then we would need this
     * Dare I say memory leak...
     */
    public void cleanup() {
        this.context = null;
        this.gameView = null;
        this.handler = null;
        this.surfaceHolder = null;
    }

    //Pre-begin a game
    abstract public void setupBeginning();

    //Starting up the game
    public void setup() {
        synchronized (monitor) {

            setupBeginning();

            lastUpdate = System.currentTimeMillis() + 100;

            setState(STATE_RUNNING);

            setScore(0);
        }
    }

    //The thread start
    @Override
    public void run() {
        Canvas canvasRun;
        while (running) {
            canvasRun = null;
            try {
                canvasRun = surfaceHolder.lockCanvas(null);
                synchronized (monitor) {
                    if (mode == STATE_RUNNING) {
                        updatePhysics();
                    }
                    draw(canvasRun);
                }
            } finally {
                if (canvasRun != null) {
                    if (surfaceHolder != null)
                        surfaceHolder.unlockCanvasAndPost(canvasRun);
                }
            }
        }
    }

    /*
     * Surfaces and drawing
     */
    public void setSurfaceSize(int width, int height) {
        synchronized (monitor) {
            canvasWidth = width;
            canvasHeight = height;
        }
    }


    protected void draw(Canvas canvas) {
        if (canvas == null) return;
        canvas.drawRect(0, 0, canvasWidth, canvasHeight, background);
    }

    private void updatePhysics() {
        now = System.currentTimeMillis();
        elapsed = (now - lastUpdate) / 1000.0f;

        updateGame(elapsed);

        lastUpdate = now;
    }

    abstract protected void updateGame(float secondsElapsed);

	/*
	 * Control functions
	 */

    //Finger touches the screen
    public boolean onTouch(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            this.actionOnTouch(e.getRawX(), e.getRawY());
        } else if (e.getAction() == MotionEvent.ACTION_DOWN) {

            if (mode == STATE_READY || mode == STATE_LOSE || mode == STATE_WIN) {
                setup();
                return true;
            }

            if (mode == STATE_PAUSE) {
                unpause();
                return true;
            }

            synchronized (monitor) {
                this.actionOnTouch(e.getRawX(), e.getRawY());
            }

        }

        return false;
    }

    protected void actionOnTouch(float x, float y) {
        //Override to do something
    }

    /*
     * Game states
     */
    public void pause() {
        synchronized (monitor) {
            if (mode == STATE_RUNNING) setState(STATE_PAUSE);
        }
    }

    public void unpause() {
        // Move the real time clock up to now
        synchronized (monitor) {
            lastUpdate = System.currentTimeMillis();
        }
        setState(STATE_RUNNING);
    }

    //Send messages to View/Activity thread
    public void setState(int mode) {
        synchronized (monitor) {
            setState(mode, null);
        }
    }

    public void setState(int mode, CharSequence message) {
        synchronized (monitor) {
            this.mode = mode;

            if (this.mode == STATE_RUNNING) {
                Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "");
                b.putInt("viz", View.INVISIBLE);
                b.putBoolean("showAd", false);
                msg.setData(b);
                handler.sendMessage(msg);
            } else {
                Message msg = handler.obtainMessage();
                Bundle b = new Bundle();

                Resources res = context.getResources();
                CharSequence str = "";
                if (this.mode == STATE_READY)
                    str = res.getText(R.string.mode_ready);
                else if (this.mode == STATE_PAUSE)
                    str = res.getText(R.string.mode_pause);
                else if (this.mode == STATE_LOSE)
                    str = res.getText(R.string.mode_lose);
                else if (this.mode == STATE_WIN) {
                    str = res.getText(R.string.mode_win);
                }

                if (message != null) {
                    str = message + "\n" + str;
                }

                b.putString("text", str.toString());
                b.putInt("viz", View.VISIBLE);

                msg.setData(b);
                handler.sendMessage(msg);
            }
        }
    }

    /*
     * Getter and setter
     */
    public void setSurfaceHolder(SurfaceHolder h) {
        surfaceHolder = h;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mMode) {
        this.mode = mMode;
    }
	
	
	/* ALL ABOUT SCORES */

    public float getScore() {
        return score;
    }

    //Send a score to the View to view
    //Would it be better to do this inside this thread writing it manually on the screen?
    public void setScore(long score) {
        this.score = score;

        synchronized (monitor) {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean("score", true);
            b.putString("text", getScoreString().toString());
            msg.setData(b);
            handler.sendMessage(msg);
        }
    }

    public void updateScore(long score) {
        this.setScore(this.score + score);
    }

    protected CharSequence getScoreString() {
        return Long.toString(Math.round(this.score));
    }

}