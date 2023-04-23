package com.kuriosityrobotics.powerplay.mpc;

import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;

import java.util.Arrays;

import static com.kuriosityrobotics.powerplay.mpc.SolverOutput.*;
import static java.lang.Math.pow;

public class SolverInput {
	private final SystemState[] initialGuesses;

	private final Pose startPose;
	private final Twist startVelocity;

	private final OptimisationParameters[] optimisationParameters;


	public SolverInput(SystemState[] initialGuesses, Pose startPose, Twist startVelocity, OptimisationParameters[] optimisationParameters) {
		this.initialGuesses = initialGuesses;
		this.startPose = startPose;
		this.startVelocity = startVelocity;
		this.optimisationParameters = optimisationParameters;
	}

	public static SolverInput ofStartingState(SystemState prevState, OptimisationParameters[] optimisationParameters) {
		var initialGuesses = new SystemState[NUM_STAGES];
		Arrays.fill(initialGuesses, prevState);
		var startPose = new Pose(prevState.getX(), prevState.getY(), prevState.getTheta());
		var startVelocity = new Twist(prevState.getXVel(), prevState.getYVel(), prevState.getThetaVel());

		return new SolverInput(initialGuesses, startPose, startVelocity, optimisationParameters);
	}

	public String toString() {
		var sb = new StringBuilder();
		for (var cond : initialGuesses) {
			sb.append("INITIAL");
			for (var number : cond.toDoubleArray()) {
				sb.append(" ").append(number);
			}
			sb.append("\n");
		}
		sb.append("XINIT ").append(startPose.x()).append(" ").append(startPose.y()).append(" ").append(startPose.orientation()).append(" ")
			.append(startVelocity.x()).append(" ").append(startVelocity.y()).append(" ").append(startVelocity.angular()).append("\n");
		for (var stage : optimisationParameters) {
			sb.append("STAGE");
			for (var number : stage.toDoubleArray()) {
				sb.append(" ").append(number);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public double[] toDoubleArray() {
	    var array = new double[NUM_VARS * NUM_STAGES + 6 + NUM_PARAMS * NUM_STAGES];

	    var index = 0;
	    for (var cond : initialGuesses) {
            for (var number : cond.toDoubleArray()) {
                array[index++] = number;
            }
        }

		array[index++] = startPose.x();
		array[index++] = startPose.y();
		array[index++] = startPose.orientation();
		array[index++] = startVelocity.x();
		array[index++] = startVelocity.y();
		array[index++] = startVelocity.angular();

		for (var stage : optimisationParameters) {
			for (var number : stage.toDoubleArray()) {
				array[index++] = number;
			}
		}

		return array;
	}

	public OptimisationParameters[] getOptimisationParameters() {
		return optimisationParameters;
	}

	public static double stageObjective(OptimisationParameters parameters, SystemState state) {
		double motorObjective = parameters.weights.fl_weight * pow(state.getFl(), 2) +
				parameters.weights.fr_weight * pow(state.getFr(), 2) +
				parameters.weights.bl_weight * pow(state.getBl(), 2) +
				parameters.weights.br_weight * pow(state.getBr(), 2);

		double robotPositionObjective = parameters.weights.x_weight * pow(parameters.target.x_desired - state.getX(), 2) +
				parameters.weights.y_weight * pow(parameters.target.y_desired - state.getY(), 2) +
				parameters.weights.theta_weight * pow(parameters.target.theta_desired - state.getTheta(), 2);

		double robotVelocityObjective = parameters.weights.x_vel_weight * pow(parameters.target.x_vel_desired - state.getXVel(), 2) +
				parameters.weights.y_vel_weight * pow(parameters.target.y_vel_desired - state.getYVel(), 2) +
				parameters.weights.theta_vel_weight * pow(parameters.target.theta_vel_desired - state.getThetaVel(), 2);

		return motorObjective + robotPositionObjective + robotVelocityObjective;
	}

	public double totalObjective(SolverOutput output) {
		double totalObjective = 0;
		for (int i = 0; i < NUM_STAGES; i++) {
			totalObjective += stageObjective(optimisationParameters[i], output.getStates()[i]);
		}
		return totalObjective;
	}


	static {
		System.loadLibrary("drivempc");
	}
	public native SolverOutput solve();
}
