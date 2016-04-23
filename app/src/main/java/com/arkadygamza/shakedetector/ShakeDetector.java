package com.arkadygamza.shakedetector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;

public class ShakeDetector {

    public static final int THRESHOLD = 13;
    public static final int SHAKES_COUNT = 3;
    public static final int SHAKES_PERIOD = 1;

    @NonNull
    public static Observable create(@NonNull Context context) {
        Observable<SensorEvent> eventObservable = getSensorEventObservable(context);

        return eventObservable
            .map(sensorEvent -> sensorEvent.values[0])
            .filter(shake -> shake > THRESHOLD || shake < -THRESHOLD)
            .buffer(2, 1)
            .filter(buf -> buf.get(0) * buf.get(1) < 0)
            .doOnNext(val -> Log.d("!!!!", "filtered by sign: " + val))
            .timestamp()
            .buffer(SHAKES_COUNT, 1)
            .doOnNext(val -> Log.d("!!!!", "grouped: " + val))
            .filter(buffer -> buffer.get(buffer.size() - 1).getTimestampMillis() - buffer.get(0).getTimestampMillis() < SHAKES_PERIOD * 1000)
            .throttleFirst(SHAKES_PERIOD, TimeUnit.SECONDS);
    }

    public static Observable create(@NonNull Context context, @NonNull MarblesPlotter marblesPlotter) {
        Observable<SensorEvent> eventObservable = getSensorEventObservable(context);

        return eventObservable
            .map(sensorEvent -> sensorEvent.values[0])
//            .doOnNext(val -> marblesPlotter.addMarble(0))

            .filter(shake -> shake > THRESHOLD || shake < -THRESHOLD)
            .doOnNext(val -> marblesPlotter.addMarble(1, val > 0))

            .buffer(2, 1)
            .filter(buf -> buf.get(0) * buf.get(1) < 0)
            .doOnNext(val -> marblesPlotter.addMarble(2, true))

            .timestamp()
            .buffer(SHAKES_COUNT, 1)
            .filter(buffer -> buffer.get(buffer.size() - 1).getTimestampMillis() - buffer.get(0).getTimestampMillis() < SHAKES_PERIOD * 1000)
            .doOnNext(val -> marblesPlotter.addMarble(3, true))

            .throttleFirst(SHAKES_PERIOD, TimeUnit.SECONDS)
            .doOnNext(val -> marblesPlotter.addMarble(4, true));
    }

    @NonNull
    private static Observable<SensorEvent> getSensorEventObservable(@NonNull Context context) {
        SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        if (sensorList == null || sensorList.isEmpty()) {
            throw new IllegalStateException("Device has no linear acceleration sensor");
        }

        return ObservableSensorListener.create(sensorList.get(0), mSensorManager);
    }
}
