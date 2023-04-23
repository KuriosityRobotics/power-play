package com.kuriosityrobotics.powerplay.io;

public enum PickupTarget {
	ONE_STACK(1),
	TWO_STACK(2),
	THREE_STACK(3),
	FOUR_STACK(4),
	FIVE_STACK(5);

	private static final double BASE_PICKUP_HEIGHT = .05;
	private static final double PICKUP_HEIGHT_PER_CONE = 0.03;
	private static final double BASE_HOVER_HEIGHT = 0.01;
	private static final double CONE_HEIGHT = 0.13;

	public final int coneStackHeight;

	PickupTarget(int coneStackHeight) {
		if (coneStackHeight < 1)
			throw new IllegalArgumentException("Cone stack height must be at least 1");

		this.coneStackHeight = coneStackHeight;
	}


	double getPickupHeight() {
		return BASE_PICKUP_HEIGHT + PICKUP_HEIGHT_PER_CONE * (coneStackHeight - 1);
	}

	double getHoverHeight() {
		if (coneStackHeight == 1)
			return BASE_HOVER_HEIGHT;
		else
			return getPickupHeight() + CONE_HEIGHT;
	}

	public static PickupTarget fromStackHeight(int i) {
		switch (i) {
			case 1:
				return ONE_STACK;
			case 2:
				return TWO_STACK;
			case 3:
				return THREE_STACK;
			case 4:
				return FOUR_STACK;
			case 5:
				return FIVE_STACK;
			default:
				throw new IllegalArgumentException("Invalid cone stack height");
		}
	}
}
