package SciChartUserClasses;

//******************************************************************************
// SCICHART® Copyright SciChart Ltd. 2011-2016. All rights reserved.
//
// Web: http://www.scichart.com
// Support: support@scichart.com
// Sales:   sales@scichart.com
//
// SciChartBuilder.java is part of the SCICHART® Examples. Permission is hereby granted
// to modify, create derivative works, distribute and publish any part of this source
// code whether for commercial, private or personal use.
//
// The SCICHART® examples are distributed in the hope that they will be useful, but
// without any warranty. It is provided "AS IS" without warranty of any kind, either
// expressed or implied.
//******************************************************************************

import android.content.Context;
import android.util.DisplayMetrics;

import com.scichart.charting.visuals.pointmarkers.IPointMarker;



public final class SciChartBuilder {
    private final DisplayMetrics displayMetrics;
    private final Context context;

    private static SciChartBuilder INSTANCE;

    public static void init(Context context) {
        INSTANCE = new SciChartBuilder(context);
    }

    public static void dispose() {
        INSTANCE = null;
    }

    public static SciChartBuilder instance() {
        return INSTANCE;
    }

    private SciChartBuilder(Context context){
        this.context = context;
        this.displayMetrics = context.getResources().getDisplayMetrics();
    }


    public <T extends IPointMarker> PointMarkerBuilder<T> newPointMarker(T pointMarker) { return new PointMarkerBuilder<>(pointMarker, displayMetrics); }


    public RenderableSeriesBuilder.FastLineRenderableSeriesBuilder newLineSeries() { return new RenderableSeriesBuilder.FastLineRenderableSeriesBuilder(displayMetrics); }


    public AxisBuilder.NumericAxisBuilder newNumericAxis() { return new AxisBuilder.NumericAxisBuilder(context); }


    public <TX extends Comparable<TX>, TY extends Comparable<TY>> DataSeriesBuilder.XyDataSeriesBuilder<TX, TY> newXyDataSeries(Class<TX> xType, Class<TY> yType) { return new DataSeriesBuilder.XyDataSeriesBuilder<>(xType, yType); }


    public ModifierGroupBuilder newModifierGroup() { return new ModifierGroupBuilder(context); }

    public ModifierGroupBuilder newModifierGroupWithDefaultModifiers() {
        return newModifierGroup()
                .withPinchZoomModifier().build()
                .withZoomPanModifier().withReceiveHandledEvents(true).build()
                .withZoomExtentsModifier().build();
    }

}
