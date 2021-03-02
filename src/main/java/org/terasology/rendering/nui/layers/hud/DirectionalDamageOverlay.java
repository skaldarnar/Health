// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.rendering.nui.layers.hud;

import com.google.common.collect.Lists;
import org.terasology.engine.Time;
import org.terasology.math.Direction;
import org.terasology.nui.Canvas;
import org.terasology.nui.widgets.UIImage;
import org.terasology.registry.In;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectionalDamageOverlay extends CoreHudWidget {

    @In
    private Time time;

    private UIImage bottom;
    private UIImage top;
    private UIImage left;
    private UIImage right;

    private List<UIImage> indicators;
    private final Map<UIImage, TimingInformation> activeIndicators = new HashMap<>();

    @Override
    public void initialise() {
        bottom = find("damageBottom", UIImage.class);
        top = find("damageTop", UIImage.class);
        left = find("damageLeft", UIImage.class);
        right = find("damageRight", UIImage.class);

        indicators = Lists.newArrayList(top, right, left, bottom);
    }

    private UIImage imageForDirection(Direction direction) {
        switch (direction) {
            case UP:
            case FORWARD:
                return top;
            case RIGHT:
                return right;
            case LEFT:
                return left;
            case DOWN:
            case BACKWARD:
                return bottom;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        float currentTime = time.getGameTime();

        // update state: update active indicators
        activeIndicators.entrySet().removeIf(activeIndicator -> activeIndicator.getValue().end <= currentTime);

        // render state: render active indicators
        for (UIImage indicator : indicators) {
            boolean isActive = activeIndicators.containsKey(indicator);
            indicator.setVisible(isActive);
            if (isActive) {
                TimingInformation timing = activeIndicators.get(indicator);
                indicator.setTint(indicator.getTint().setAlpha(timing.lerp(currentTime)));
            }
        }
        super.onDraw(canvas);
    }

    public void show(Direction damageDirection, float durationInSeconds) {
        float currentTime = time.getGameTime();
        activeIndicators.put(imageForDirection(damageDirection), new TimingInformation(currentTime,
                currentTime + durationInSeconds));
    }
}
