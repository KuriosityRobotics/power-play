import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.mpc.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

class SolverOutputTest {
	@Test
	void a() throws IOException {
		System.loadLibrary("drivempc");

			var twist  = new OptimisationParameters(
				DriveParameters.ofDefaulDriveParameters(12.), // 12 volt battery
				new TargetParameters(0, 0, 0, 0.2, 0, 0), // x-vel target
				new WeightParameters(0, 0, 0, 0, 0, 0, 0, 1, 1, 1) // only weighs velo
			);
			
			System.out.println(twist);
			var driveParams = new OptimisationParameters[10];
			Arrays.fill(driveParams, twist);


			var guess = SystemState.from(
				1, 1, 1, 1,
				0, 0, 0,
				1, 1, 1
			);
		var guesses = new SystemState[10];
		Arrays.fill(guesses, guess);

		var solver = new SolverInput(guesses, new Pose(0, 0, 0), new Twist(.2, 0, 0), driveParams); // must be in meters

		for (int i = 0; i < 10; i++) {
			var result = solver.solve();
			System.out.println(result);
			SystemState[] states = result.getStates();
			solver = SolverInput.ofStartingState(states[states.length - 1], driveParams);
		}
	}

}