package com.kuriosityrobotics.powerplay.navigation;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public final class FieldNavConstants {
    public static final double GRID_SIZE = 23.75;
	public static final Vector2D ROBOT_DIMENSIONS = new Vector2D(6.5, 5.5);

    public static final JunctionTypes[][] field = new JunctionTypes[][]{
            {JunctionTypes.GROUND, JunctionTypes.LOW, JunctionTypes.GROUND, JunctionTypes.LOW, JunctionTypes.GROUND},
            {JunctionTypes.LOW, JunctionTypes.MEDIUM, JunctionTypes.HIGH, JunctionTypes.MEDIUM, JunctionTypes.LOW},
            {JunctionTypes.GROUND, JunctionTypes.HIGH, JunctionTypes.GROUND, JunctionTypes.HIGH, JunctionTypes.GROUND},
            {JunctionTypes.LOW, JunctionTypes.MEDIUM, JunctionTypes.HIGH, JunctionTypes.MEDIUM, JunctionTypes.LOW},
            {JunctionTypes.GROUND, JunctionTypes.LOW, JunctionTypes.GROUND, JunctionTypes.LOW, JunctionTypes.GROUND}
    };
}
