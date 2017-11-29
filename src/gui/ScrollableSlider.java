package gui;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JSlider;

public class ScrollableSlider extends JSlider {

    public ScrollableSlider() {
        super();
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int newValue = getValue() - e.getWheelRotation();
                if (getMinimum() <= newValue && newValue <= getMaximum()) {
                    setValue(newValue);
                }
            }
        });
    }

}
