// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.rendering.nui.layers.hud;

import org.terasology.engine.Time;
import org.terasology.math.Direction;
import org.terasology.nui.Canvas;
import org.terasology.nui.widgets.UIImage;
import org.terasology.registry.In;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DirectionalDamageOverlay extends CoreHudWidget {

    @In
    private Time time;

    private UIImage bottom;
    private UIImage top;
    private UIImage left;
    private UIImage right;

    private Map<Direction, TimingInformation> activeIndicators = new HashMap<>();

    @Override
    public void initialise() {
        bottom = find("damageBottom", UIImage.class);
        top = find("damageTop", UIImage.class);
        left = find("damageLeft", UIImage.class);
        right = find("damageRight", UIImage.class);
    }

    private Optional<UIImage> imageForDirection(Direction direction) {
        switch (direction) {

            case FORWARD:
                return Optional.of(top);
            case RIGHT:
                return Optional.of(right);
            case LEFT:
                return Optional.of(left);
            case BACKWARD:
                return Optional.of(bottom);
            case UP:
            case DOWN:
            default:
                return Optional.empty();

        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        float currentTime = time.getGameTime();

        // update state: update active indicators
        activeIndicators.entrySet().removeIf(activeIndicator -> activeIndicator.getValue().end <= currentTime);

        // render state: render active indicators
        for (Direction direction : Direction.values()) {
            imageForDirection(direction).ifPresent(indicator -> {
                boolean isActive = activeIndicators.containsKey(direction);
                indicator.setVisible(isActive);

                if (isActive) {
                    TimingInformation timing = activeIndicators.get(direction);
                    indicator.setTint(indicator.getTint().setAlpha(timing.lerp(currentTime)));
                }
            });
        }
        super.onDraw(canvas);
    }

    public void show(Direction damageDirection, float durationInSeconds) {
        float currentTime = time.getGameTime();
        activeIndicators.put(damageDirection, new TimingInformation(currentTime, currentTime + durationInSeconds));
    }
}
