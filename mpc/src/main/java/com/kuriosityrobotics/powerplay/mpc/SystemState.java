package com.kuriosityrobotics.powerplay.mpc;

import static java.lang.Math.pow;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;

import java.util.Arrays;

public class SystemState {
	private final double fl;
	private final double fr;
	private final double bl;
	private final double br;

	private final double x;
	private final double y;
	private final double theta;

	private final double xVel;
	private final double yVel;
	private final double thetaVel;

	private SystemState(
		double fl, double fr, double bl, double br,
		double x, double y, double theta,
		double xVel, double yVel, double thetaVel
	) {
		this.fl = fl;
		this.fr = fr;
		this.bl = bl;
		this.br = br;
		this.x = x;
		this.y = y;
		this.theta = theta;
		this.xVel = xVel;
		this.yVel = yVel;
		this.thetaVel = thetaVel;
	}

	public static SystemState from(
		double fl, double fr, double bl, double br,
		double x, double y, double theta,
		double xVel, double yVel, double thetaVel
	) {
		return new SystemState(
			fl, fr, bl, br,
			x, y, theta,
			xVel, yVel, thetaVel
		);
	}

	public static SystemState from(double[] array) {
        return new SystemState(
            array[0], array[1], array[2], array[3],
            array[4], array[5], array[6],
            array[7], array[8], array[9]
        );
    }

	public static SystemState ofLocalisation(LocalisationDatum localisation) {
		return SystemState.from(
			0, 0, 0, 0,
			localisation.pose().x(), localisation.pose().y(), localisation.pose().orientation(),
			localisation.twist().x(), localisation.twist().y(), localisation.twist().angular()
		);
	}

	public static SystemState ofPowersAndLocalisation(MotorPowers powers, LocalisationDatum localisation) {
		return SystemState.from(
			powers.powerFrontLeft(), powers.powerFrontRight(), powers.powerBackLeft(), powers.powerBackRight(),
			localisation.pose().x(), localisation.pose().y(), localisation.pose().orientation(),
			localisation.twist().x(), localisation.twist().y(), localisation.twist().angular()
		);
	}

	public static SystemState[] ofLocalisationGuesses(LocalisationDatum localisation, int numStages) {
		var guesses = new SystemState[numStages];
		Arrays.fill(guesses, ofLocalisation(localisation));
		return guesses;
	}


	public double[] toDoubleArray() {
		return new double[] {
			fl, fr, bl, br,
			x, y, theta,
			xVel, yVel, thetaVel
		};
	}

	public double getFl() { return fl; }
	public double getFr() { return fr; }
	public double getBl() { return bl; }
	public double getBr() { return br; }

	public double getX() { return x; }
	public double getY() { return y; }
	public double getTheta() { return theta; }

	public double getXVel() { return xVel; }
	public double getYVel() { return yVel; }
	public double getThetaVel() { return thetaVel; }


	@Override
	public String toString() {
		return String.format("MotorPowers(%04.2f, %04.2f, %04.2f, %04.2f), position(%04.2f %04.2f %04.2f), velocity(%04.2f %04.2f %04.2f)",
			fl, fr, bl, br,
			x, y, theta,
			xVel, yVel, thetaVel
		);
	}

	public static String alignedHeader() {
		return String.format("%-4s %-4s %-4s %-4s %-4s %-4s %-4s %-4s %-4s %-4s",
			"fl", "fr", "bl", "br",
			"x", "y", "Θ",
			"u", "v", "Φ"
		);
	}

	public MotorPowers getMotorPowers() {
		return MotorPowers.ofPowers(fl, fr, bl, br);
	}

	public LocalisationDatum getLocalisation() {
		return LocalisationDatum.forCurrentTime(
			Pose.of(x, y, theta),
			Twist.of(xVel, yVel, thetaVel)
		);
	}
}
