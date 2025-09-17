package de.uni_koblenz.ptsd.foxtrot.mazeclient.app;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.input.ScrollEvent;

public class Zoom {

    private static final double SCALE_DELTA = 1.1;

    public static void enableZoom(Node target) {
        target.setOnScroll((ScrollEvent event) -> {
            if (event.isControlDown()) {
                double scaleFactor = event.getDeltaY() > 0 ? SCALE_DELTA : 1 / SCALE_DELTA;

                target.setScaleX(target.getScaleX() * scaleFactor);
                target.setScaleY(target.getScaleY() * scaleFactor);

                event.consume();
            }
        });
    }

    public static void bindSlider(Node target, Slider slider) {
        slider.setMin(50);
        slider.setMax(200);
        slider.setValue(100);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(50);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(10);

        ChangeListener<Number> listener = (obs, oldVal, newVal) -> {
            double factor = newVal.doubleValue() / 100.0;
            target.setScaleX(factor);
            target.setScaleY(factor);
        };
        slider.valueProperty().addListener(listener);
    }

}