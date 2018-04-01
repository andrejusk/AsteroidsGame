package uk.ac.reading.student.akostarevas.asteroids;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

abstract class StaticObject {

    private final static float SIZE = 10;
    float x;
    float y;
    private Paint debugPaint;

    StaticObject(float x, float y) {
        this.x = x;
        this.y = y;
        debugPaint = new Paint();
        debugPaint.setColor(Color.GREEN);
    }

    void draw(Canvas canvas) {
        canvas.drawCircle(x, y, SIZE, debugPaint);
    }

}
