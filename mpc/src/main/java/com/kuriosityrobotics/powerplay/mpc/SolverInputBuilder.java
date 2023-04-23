package com.kuriosityrobotics.powerplay.mpc;

import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import static com.kuriosityrobotics.powerplay.mpc.SolverOutput.NUM_STAGES;

public class SolverInputBuilder {
	private final SystemState[] initialGuesses = new SystemState[NUM_STAGES];

	private Pose startPose;
	private Twist startVelocity;

	private OptimisationParameters[] optimisationParameters = new OptimisationParameters[NUM_STAGES];

	public SolverInputBuilder() {
		Arrays.fill(initialGuesses, SystemState.from(1, 1, 1, 1, 0, 0, 0, 0, 0, 0));
		this.startPose = Pose.zero();
		this.startVelocity = Twist.zero();
	}

	public SolverInputBuilder(SolverInputBuilder input) {
		System.arraycopy(input.initialGuesses, 0, initialGuesses, 0, initialGuesses.length);
		this.startPose = input.startPose;
		this.startVelocity = input.startVelocity;
		System.arraycopy(input.optimisationParameters, 0, optimisationParameters, 0, optimisationParameters.length);
	}

	public SolverInputBuilder withInitialGuess(SystemState state) {
		Arrays.fill(initialGuesses, state);
		return this;
	}

	public SolverInputBuilder startingAt(LocalisationDatum datum) {
		this.startPose = datum.pose().metres();
		this.startVelocity = datum.twist().metres().rotate(datum.pose().orientation());
		return this;
	}

	public SolverInputBuilder fillParameters(OptimisationParameters params) {
		Arrays.fill(optimisationParameters, params);
		return this;
	}

	public SolverInputBuilder setParametersFor(int stage, OptimisationParameters params) {
		optimisationParameters[stage] = params;
		return this;
	}

	public SolverInputBuilder withParameters(OptimisationParameters... params) {
		this.optimisationParameters = Arrays.copyOf(params, params.length);
		return this;
	}

	public SolverInputBuilder loadParameters(InputStream is) throws IOException {
		CSVParser csv = null;
		try {
			csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().withRecordSeparator('\n').parse(new InputStreamReader(is));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		var records = csv.getRecords().stream().map(
			record -> new OptimisationParameters(
				DriveParameters.ofDefaulDriveParameters(-1), // if voltage is still -1 by the time the code is deployed, it means you forgot to set it
				new TargetParameters(
					Double.parseDouble(record.get("x_desired")),
					Double.parseDouble(record.get("y_desired")),
					Double.parseDouble(record.get("theta_desired")),
					Double.parseDouble(record.get("x_vel_desired")),
					Double.parseDouble(record.get("y_vel_desired")),
					Double.parseDouble(record.get("theta_vel_desired"))),
				new WeightParameters(Double.parseDouble(record.get("fl_weight")),
					Double.parseDouble(record.get("fr_weight")),
					Double.parseDouble(record.get("bl_weight")),
					Double.parseDouble(record.get("br_weight")),
					Double.parseDouble(record.get("x_weight")),
					Double.parseDouble(record.get("y_weight")),
					Double.parseDouble(record.get("theta_weight")),
					Double.parseDouble(record.get("x_vel_weight")),
					Double.parseDouble(record.get("y_vel_weight")),
					Double.parseDouble(record.get("theta_vel_weight")))
			)
		).toArray(OptimisationParameters[]::new);

		return withParameters(records);
	}

	public SolverInput getRange(int start, int end, double batteryVoltage) {
		var newStageParams = new OptimisationParameters[end - start];
		for (int i = start; i < end; i++) {
			if (end >= optimisationParameters.length) {
				newStageParams[i - start] = OptimisationParameters.ofBatteryVoltage(batteryVoltage, optimisationParameters[optimisationParameters.length-1]);
			} else {
				newStageParams[i - start] = OptimisationParameters.ofBatteryVoltage(batteryVoltage, optimisationParameters[i]);
			}
		}
		return new SolverInput(initialGuesses, startPose, startVelocity, newStageParams);
	}
}
