package com.carlos.voiceline;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by carlos on 2016/1/29.
 * 自定义声音振动曲线view
 */
public class VoiceLineView extends View {
    private Paint paintVoiceLine;
    private float translateX = 0;
    private boolean isSet = false;  //是否有语音流输入
    private boolean isShow = true;  //是否开始绘制
    private float amplitude = 1;    //振幅
    private float volume = 10;      //音量
    private int fineness = 1;       //精细度  值越小，曲线越顺滑，但在一些旧手机上，会出现帧率过低的情况，可以把这个值调大一点，在图片的顺滑度与帧率之间做一个取舍
    private float targetVolume = 1;
    private int lineSpeed = 90;     //波动线的横向移动速度，线的速度的反比，即这个值越小，线横向移动越快，越大线移动越慢
    private long lastTime = 0;

    List<Path> paths = null;

    private int indexColor = -1;    //颜色渲染的数组index
    private int inputCount = 0;     //用于统计语音输入次数
    private int color[] = {
            Color.parseColor("#FFFF4744"), Color.parseColor("#FFFF8C44"), Color.parseColor("#FFFFF344"),
            Color.parseColor("#FF57FF44"), Color.parseColor("#FF44FFDD"), Color.parseColor("#FF9E44FF"),
            Color.parseColor("#FFFF44BA"), Color.parseColor("#FFFF444D")};

    private int colors[][] = {
            color,
            {color[6], color[0], color[1], color[2], color[3], color[4], color[5]},
            {color[5], color[6], color[0], color[1], color[2], color[3], color[4]},
            {color[4], color[5], color[6], color[0], color[1], color[2], color[3]},
            {color[3], color[4], color[5], color[6], color[0], color[1], color[2]},
            {color[2], color[3], color[4], color[5], color[6], color[0], color[1]},
            {color[1], color[2], color[3], color[4], color[5], color[6], color[0]}};

    private static LinearGradient linearGradient;

    public VoiceLineView(Context context) {
        this(context, null);
    }

    public VoiceLineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAtts(context, attrs);
    }

    //初始化  设置自定义属性
    private void initAtts(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.voiceView);
        lineSpeed = typedArray.getInt(R.styleable.voiceView_lineSpeed, 150);
        fineness = typedArray.getInt(R.styleable.voiceView_fineness, 3);


        paths = new ArrayList<>(15);
        for (int i = 0; i < 15; i++) {
            paths.add(new Path());
        }
        typedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawVoiceLine(canvas);
        if(isShow){//如果是true则刷新 产生动画  否则不刷新  即没有动画效果
            invalidate();
        }
    }

    //绘制图形
    private void drawVoiceLine(Canvas canvas) {
        lineChange();

        if (paintVoiceLine == null) {
            paintVoiceLine = new Paint();
            paintVoiceLine.setAntiAlias(true);
            paintVoiceLine.setStyle(Paint.Style.STROKE);
            paintVoiceLine.setStrokeWidth(5f);
        }

        canvas.save();
        int moveX = getWidth();
        int moveY = getHeight() / 2;

        //将所有path起点移到左边中点
        for (int i = 0; i < paths.size(); i++) {
            paths.get(i).reset();
            paths.get(i).moveTo(0, moveY);
        }


        //依次从左至右绘制图形
        for (float i = 0; i < moveX; i += fineness) {
            amplitude =  4 * volume * i / moveX -  4 * volume * i * i / moveX / moveX;
            for (int n = 1; n <= paths.size(); n++) {
                float sin = amplitude * (float) Math.sin((i - Math.pow(1.22, n)) * Math.PI / 180 - translateX);
                paths.get(n - 1).lineTo(i, (2 * n * sin / paths.size() - 15 * sin / paths.size() + moveY));
            }
        }

        //通过设置透明度产生层次效果  180  越大线越不透明
        for (int n = 0; n < paths.size(); n++) {
            if (n == paths.size() - 1) {
                paintVoiceLine.setAlpha(255);
            } else {
                paintVoiceLine.setAlpha(n * 180 / paths.size());
            }
            if (paintVoiceLine.getAlpha() > 0) {
                canvas.drawPath(paths.get(n), paintVoiceLine);
            }
        }
        canvas.restore();
    }

    //对线条颜色进行渲染  inputCount可以控制渲染的速度
    private void setLinearGradientColor() {
        inputCount++;
        if (inputCount % 3 == 0) {
            indexColor++;
            if (indexColor > 6) {
                indexColor = 0;
            }
            linearGradient = new LinearGradient(0, getHeight() / 2, getWidth(), getHeight() / 2, colors[indexColor], null, Shader.TileMode.MIRROR);
            paintVoiceLine.setShader(linearGradient);
        }
    }

    //通过声音改变振幅
    public void setVolume(int vol) {
        isSet = true;
        if(vol < 30){
            targetVolume = 0;
        }else if(vol >= 30 && vol <40){
            targetVolume = (float) (0.25 * getHeight() /2);
        }else if (vol >= 40 && vol <45){
            targetVolume = (float) (0.50 * getHeight() /2);
        }else if(vol >= 45 && vol <60){
            targetVolume = (float) (0.75 * getHeight() /2);
        }else {
            targetVolume = (float) (getHeight() /2);
        }
    }
    public boolean isShow(){
        return isShow;
    }

    //启动动画效果   即开始绘制
    public void startWave() {
        isShow = true;
        invalidate();
    }

    //停止动画效果   即停止绘制
    public void stopWave() {
        isShow = false;
        invalidate();
    }

    //通过每次改变产生位移量  形成动画
    private void lineChange() {
        setLinearGradientColor();
        //根据时间改变偏移量
        if (lastTime == 0) {
            lastTime = System.currentTimeMillis();
            translateX += 1.5;
        } else {
            if (System.currentTimeMillis() - lastTime > lineSpeed) {
                lastTime = System.currentTimeMillis();
                translateX += 1.5;
            } else {
                return;
            }
        }
        if(volume < targetVolume && isSet){     //声贝大于volume  而且是处于声音输入状态
            volume = targetVolume;
        }else {
            isSet = false;

            if (volume <= 10) {
                volume = 0;
            } else {//当结束说话的时候，将振幅缓缓降下来
                if (volume < getHeight() / 10) {
                    volume -= getHeight() / 20;
                } else {
                    volume -= getHeight() / 10;
                }
            }
        }
    }
}