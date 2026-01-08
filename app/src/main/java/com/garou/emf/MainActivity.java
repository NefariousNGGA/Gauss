package com.garou.emf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magSensor;
    private TextView emfText, maxText, btnAbout, btnReset;
    private FrameLayout graphPlaceholder;
    private LineGraphView lineGraph; // Custom View

    private double maxMagnitude = 0;
    private double filteredValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        emfText = (TextView) findViewById(R.id.emfValue);
        maxText = (TextView) findViewById(R.id.maxText);
        btnAbout = (TextView) findViewById(R.id.btnAbout);
        btnReset = (TextView) findViewById(R.id.btnReset);
        graphPlaceholder = (FrameLayout) findViewById(R.id.graphPlaceholder);

        // Add the custom Line Graph programmatically
        lineGraph = new LineGraphView(this);
        graphPlaceholder.addView(lineGraph);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        btnReset.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					maxMagnitude = 0;
					maxText.setText("Peak: 0.0 µT");
					lineGraph.clearGraph(); // Clear lines
				}
			});

        btnAbout.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					showAboutDialog();
				}
			});
    }

    private void showAboutDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setGravity(Gravity.CENTER);

        // Soft Square Background
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(30); 
        shape.setColor(Color.parseColor("#14181F"));
        layout.setBackgroundDrawable(shape);

        TextView title = new TextView(this);
        title.setText("Gauss v1.2");
        title.setTextColor(Color.parseColor("#2979FF"));
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);

        TextView desc = new TextView(this);
        desc.setText("\nThis device measures localized magnetic flux density in Micro-Teslas (µT).\n\nNormal ambient levels: 30-50 µT.");
        desc.setTextColor(Color.parseColor("#B0BEC5"));
        desc.setTextSize(14);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 0, 0, 40);

        TextView telegram = new TextView(this);
        telegram.setText("TELEGRAM: @dndvcx");
        telegram.setTextColor(Color.WHITE);
        telegram.setPadding(0, 20, 0, 20);
        telegram.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/dndvcx"))); } catch (Exception e) {}
				}
			});

        TextView facebook = new TextView(this);
        facebook.setText("FACEBOOK: Polnareff Ubermensch");
        facebook.setTextColor(Color.WHITE);
        facebook.setPadding(0, 10, 0, 10);
        facebook.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/PolnareffOnGithub"))); } catch (Exception e) {}
				}
			});

        layout.addView(title);
        layout.addView(desc);
        layout.addView(telegram);
        layout.addView(facebook);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            double raw = Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
            filteredValue = (filteredValue * 0.90) + (raw * 0.10);

            emfText.setText(String.format("%.1f", filteredValue));

            if (filteredValue > maxMagnitude) {
                maxMagnitude = filteredValue;
                maxText.setText("Peak: " + String.format("%.1f", maxMagnitude) + " µT");
            }

            // Push data to the Line Graph
            lineGraph.addPoint((float) filteredValue);
        }
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}
    @Override protected void onResume() { super.onResume(); if(magSensor!=null) sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI); }
    @Override protected void onPause() { super.onPause(); sensorManager.unregisterListener(this); }

    // --- CUSTOM GRAPH ENGINE ---
    // This draws a smooth line 📈
    private class LineGraphView extends View {
        private Paint paint;
        private ArrayList<Float> dataPoints;
        private int maxPoints = 100; // How much history to show

        public LineGraphView(Context context) {
            super(context);
            dataPoints = new ArrayList<Float>();
            paint = new Paint();
            paint.setColor(Color.parseColor("#2979FF")); // Graph Color (Blue)
            paint.setStrokeWidth(4f); // Line Thickness
            paint.setStyle(Paint.Style.STROKE); // Draw lines, not shapes
            paint.setAntiAlias(true); // Smooth edges
        }

        public void addPoint(float value) {
            dataPoints.add(value);
            if (dataPoints.size() > maxPoints) {
                dataPoints.remove(0); // Scroll effect (remove old data)
            }
            invalidate(); // Force redraw
        }

        public void clearGraph() {
            dataPoints.clear();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (dataPoints.size() < 2) return;

            Path path = new Path();
            float width = getWidth();
            float height = getHeight();

            // X-axis spacing
            float xStep = width / (maxPoints - 1);

            // Scale data to fit height (assuming max range around 150uT usually)
            float scaleY = height / 150f; 

            // Start path
            path.moveTo(0, height - (dataPoints.get(0) * scaleY));

            for (int i = 1; i < dataPoints.size(); i++) {
                float x = i * xStep;
                // Invert Y because canvas coordinates start from top
                float y = height - (dataPoints.get(i) * scaleY);
                // Clamp Y so it doesn't go off screen
                if (y < 0) y = 0; 

                path.lineTo(x, y);
            }

            canvas.drawPath(path, paint);
        }
    }
}

