// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.cattaka.android.cameraxmlkit.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class ObjectGraphic extends GraphicOverlay.Graphic {

    private static final String TAG = "TextGraphic";
    private static final int TEXT_COLOR = Color.RED;
    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;
    private final Paint textPaint;
    private final FirebaseVisionObject element;

    public ObjectGraphic(GraphicOverlay overlay, FirebaseVisionObject element) {
        super(overlay);

        this.element = element;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Log.d(TAG, "on draw text graphic");
        if (element == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(element.getBoundingBox());
        canvas.drawRect(rect, rectPaint);

        String name = CATEGORY_NAMES.get(element.getClassificationCategory());
        int n = name.length();
        float fontSize = Math.min(rect.width() / n, rect.height()) * 1.8f;

        // Renders the text at the bottom of the box.
        textPaint.setTextSize(fontSize);
        canvas.drawText(name, rect.left, rect.bottom, textPaint);
    }

    private static final Map<Integer, String> CATEGORY_NAMES;

    static {
        Map<Integer, String> t = new HashMap<>();
        t.put(FirebaseVisionObject.CATEGORY_UNKNOWN, "Unknown");
        t.put(FirebaseVisionObject.CATEGORY_HOME_GOOD, "Home Goods");
        t.put(FirebaseVisionObject.CATEGORY_FASHION_GOOD, "Fashion Goods");
        t.put(FirebaseVisionObject.CATEGORY_FOOD, "Food");
        t.put(FirebaseVisionObject.CATEGORY_PLACE, "Place");
        t.put(FirebaseVisionObject.CATEGORY_PLANT, "Plant");
        CATEGORY_NAMES = Collections.unmodifiableMap(t);
    }
}
