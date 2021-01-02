package com.alphadraco.bleconfigurator;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.InputStream;

/**
 * TODO: document your custom view class.
 */
public class ColorEntry extends View {
    private String titleText = "undefined";
    private int titleColor = Color.BLACK;
    private int selectColor = Color.RED;
    private float displayDimension = 32;
    private int frameColor = Color.BLACK;
    private float frameWidth = 5;
    public enum DisplayStyle { rectangle, roundrectangle, circle};
    private DisplayStyle displayStyle = DisplayStyle.rectangle;
    private float expandHeight = 32*10;

    private Bitmap colormap = null;

    private int foldedHeight = 0;
    private int expandedHeight = 0;
    private boolean expanded = false;

    private Paint paint;

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }


    public ColorEntry(Context context) {
        super(context);
        init(null, 0);
    }

    public ColorEntry(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ColorEntry(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ColorEntry, defStyle, 0);

        titleText = a.getString(
                R.styleable.ColorEntry_titleText);
        selectColor = a.getColor(
                R.styleable.ColorEntry_selectColor,
                selectColor);
        displayDimension = a.getDimension(
                R.styleable.ColorEntry_displayDimension,
                displayDimension);
        titleColor = a.getColor(
                R.styleable.ColorEntry_titleColor,
                titleColor);
        frameWidth = a.getDimension(
                R.styleable.ColorEntry_frameWidth,
                frameWidth);
        frameColor = a.getColor(
                R.styleable.ColorEntry_frameColor,
                frameColor);
        displayStyle = DisplayStyle.values()[a.getInt(
                R.styleable.ColorEntry_displayStyle,
                displayStyle.ordinal())];

        a.recycle();

        paint = new Paint();
        paint.setTextAlign(Paint.Align.LEFT);
        colormap = null;

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

    }

    private void expand() {
        if (expanded) return;
        expanded = true;
        ViewGroup.LayoutParams lo = getLayoutParams();
        if (foldedHeight == 0) {
            foldedHeight = lo.height;
            expandedHeight =  dpToPx(200);
        }
        expanded = true;
        lo.height = expandedHeight;
        setLayoutParams(lo);
        requestLayout();
    }

    private void fold() {
        if (!expanded) return;
        expanded = false;
        ViewGroup.LayoutParams lo = getLayoutParams();
        lo.height = foldedHeight;
        setLayoutParams(lo);
        requestLayout();
    }

    float touched_x, touched_y;
    boolean touched = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // touchCounter++;
        touched_x = event.getX();
        touched_y = event.getY();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (expanded) {
                    int padLeft = getPaddingLeft();
                    int padRight = getPaddingRight();
                    int padTop = getPaddingTop();
                    int padBottom = getPaddingBottom();
                    int width = getWidth();
                    int height = getHeight();
                    if ((touched_x >= padLeft) && (touched_x <= width-padRight) &&
                            (touched_y >= padTop + 4*displayDimension) && (touched_y <= height-padBottom)) {
                        float x = (touched_x - padLeft) / (width-padLeft-padRight);
                        float y = (touched_y - padTop - 4*displayDimension) / (height - padTop - padBottom - 4*displayDimension);
                        if (x < 0) x = 0;if (x > 1) x = 1;
                        if (y < 0) y = 0;if (y > 1) y = 1;
                        x = x * (colormap.getWidth()-1);
                        y = y * (colormap.getHeight()-1);
                        int col = colormap.getPixel((int)Math.round(Math.floor(x)), (int)Math.round(Math.floor(y)));
                        selectColor = col & 0x00FFFFFF;
                        if (onColorChangeListener != null)
                            onColorChangeListener.onColorChange(this, selectColor);
                    }
                    fold();
                } else {
                    if (touched_x > getWidth()-getPaddingRight()-displayDimension*2) {
                        expand();
                    }
                }
                touched = true;
                break;
            case MotionEvent.ACTION_MOVE:
                touched = true;
                break;
            case MotionEvent.ACTION_UP:
                touched = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                touched = false;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                touched = false;
                break;
            default:
        }
        return true; // processed
    }

    private void invalidateTextPaintAndMeasurements() {
        paint.setTextSize(displayDimension);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth();
        int contentHeight = getHeight();

        paint.setColor(titleColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(titleText, paddingLeft, paddingTop + 3*displayDimension/2, paint);

        switch (displayStyle) {
            case rectangle:
                paint.setColor(selectColor | 0xFF000000);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(contentWidth-paddingRight- displayDimension, paddingTop + displayDimension/2,  contentWidth-paddingRight, paddingTop + 3 * displayDimension /2, paint);
                paint.setColor(frameColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(frameWidth);
                canvas.drawRect(contentWidth-paddingRight- displayDimension, paddingTop + displayDimension/2,  contentWidth-paddingRight, paddingTop + 3 * displayDimension /2, paint);
                break;
            case roundrectangle:
                paint.setColor(selectColor | 0xFF000000);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(contentWidth-paddingRight- displayDimension, paddingTop + displayDimension/2,  contentWidth-paddingRight, paddingTop + 3 * displayDimension /2,
                        displayDimension/4,displayDimension/4, paint);
                paint.setColor(frameColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(frameWidth);
                canvas.drawRoundRect(contentWidth-paddingRight- displayDimension, paddingTop + displayDimension/2,  contentWidth-paddingRight, paddingTop + 3 * displayDimension /2,
                        displayDimension/4,displayDimension/4, paint);
                break;
            case circle:
                paint.setColor(selectColor | 0xFF000000);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(contentWidth-paddingRight- displayDimension, paddingTop + displayDimension/2, displayDimension /2, paint);
                paint.setColor(frameColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(frameWidth);
                canvas.drawCircle(contentWidth-paddingRight- displayDimension, paddingTop + displayDimension/2, displayDimension /2, paint);
                break;
        }

        if (expanded) {
            if (colormap == null) {
                Bitmap sourcemap = BitmapFactory.decodeResource(getResources(),R.drawable.colormap);
                colormap = Bitmap.createScaledBitmap(sourcemap,
                        contentWidth-paddingLeft-paddingRight,
                        contentHeight-paddingTop-paddingBottom-Math.round(4*displayDimension),false);
            }
            canvas.drawBitmap(colormap,paddingLeft,paddingTop+4*displayDimension, paint);
        }

    }

    public String getTitleText() {
        return titleText;
    }

    public void setTitleText(String _titleText) {
        titleText = _titleText;
        invalidateTextPaintAndMeasurements();
    }

    public int getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(String _titleColor) {
        titleText = _titleColor;
        invalidateTextPaintAndMeasurements();
    }


    public int getExampleColor() {
        return selectColor;
    }

    public void setSelectColor(int _selectColor) {
        selectColor = _selectColor;
        invalidateTextPaintAndMeasurements();
    }

    public float getDisplayDimension() {
        return displayDimension;
    }

    public void setDisplayDimension(float _displayDimension) {
        displayDimension = _displayDimension;
        invalidateTextPaintAndMeasurements();
    }

    public interface OnColorChangeListener {
        void onColorChange(ColorEntry ce, int newColor);
    }

    private OnColorChangeListener onColorChangeListener = null;

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        onColorChangeListener = listener;
    }

}
