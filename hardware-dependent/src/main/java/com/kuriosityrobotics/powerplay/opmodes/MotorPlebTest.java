package com.kuriosityrobotics.powerplay.opmodes;

import android.annotation.SuppressLint;
import android.os.Environment;

import com.kuriosityrobotics.powerplay.util.Duration;
import com.kuriosityrobotics.powerplay.util.Instant;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

import java.io.IOException;
import java.io.PrintWriter;

@TeleOp
@Disabled
public class MotorPlebTest extends LinearOpMode {
	@SuppressLint("DefaultLocale")
	@Override
	public void runOpMode() {
		// after you run this, transfer the output to ur computer with this command:   adb pull /storage/emulated/0/output.csv data/<sensible name>.csv.  the file should be committed to git
		hardwareMap.get(LynxModule.class, "Control Hub").setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
		try (var out = new PrintWriter(Environment.getExternalStorageDirectory().getPath() + "/output.csv")) {
			out.println("time,m_power,batt_voltage,motor_current,motor_position,motor_velocity,motor_acceleration");
			Instant startTime = Instant.now();

			ramp(out, startTime, Duration.ofMillis(500), 0, 1, .05);
			ramp(out, startTime, Duration.ofMillis(500), 1, 0, -.05);

			ramp(out, startTime, Duration.ofMillis(1_000), 0, 1, .05);
			ramp(out, startTime, Duration.ofMillis(1_000), 1, 0, -.05);

			ramp(out, startTime, Duration.ofMillis(5_000), 0, 1, .05);
			ramp(out, startTime, Duration.ofMillis(5_000), 1, 0, -.05);

			ramp(out, startTime, Duration.ofMillis(10_000), 0, 1, .05);
			ramp(out, startTime, Duration.ofMillis(10_000), 1, 0, -.05);

			ramp(out, startTime, Duration.ofMillis(20_000), 0, 1, .05);
			ramp(out, startTime, Duration.ofMillis(20_000), 1, 0, -.05);

			step(out, startTime, Duration.ofMillis(5_000), 1);
			step(out, startTime, Duration.ofMillis(5_000), 0);
			out.flush();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@SuppressLint("DefaultLocale")
	void ramp(PrintWriter out, Instant startTime, Duration duration, double startVoltage, double endVoltage, double step) throws InterruptedException {
		var ch = hardwareMap.get(LynxModule.class, "Control Hub");
		var motor = (DcMotorEx) hardwareMap.dcMotor.get("motor");
		double sleepDuration = duration.toSeconds() / ((endVoltage - startVoltage) / step);

		for (double power = startVoltage; power < endVoltage; power += step) {
			motor.setPower(power);
			var time = Instant.now();

			ch.getBulkData();
			var m_power = motor.getPower();
			var batt_voltage = hardwareMap.voltageSensor.iterator().next().getVoltage();
			var motor_current = motor.getCurrent(CurrentUnit.AMPS);
			var motor_position = motor.getCurrentPosition();
			var motor_velocity = motor.getVelocity(AngleUnit.RADIANS);

			out.println(String.format("%f, %f, %f, %f, %d, %f", time.since(startTime).toSeconds(), m_power, batt_voltage, motor_current, motor_position, motor_velocity));
			Thread.sleep((long) (sleepDuration * 1000));
		}
	}

	@SuppressLint("DefaultLocale")
	void step(PrintWriter out, Instant startTime, Duration duration, double voltage) throws InterruptedException {
		var ch = hardwareMap.get(LynxModule.class, "Control Hub");
		var motor = (DcMotorEx) hardwareMap.dcMotor.get("motor");
		motor.setPower(voltage);
		var time = Instant.now();

		while (Instant.now().isBefore(time.add(duration))) {
			ch.getBulkData();
			var m_power = motor.getPower();
			var batt_voltage = hardwareMap.voltageSensor.iterator().next().getVoltage();
			var motor_current = motor.getCurrent(CurrentUnit.AMPS);
			var motor_position = motor.getCurrentPosition();
			var motor_velocity = motor.getVelocity(AngleUnit.RADIANS);

			out.println(String.format("%f, %f, %f, %f, %d, %f", time.since(startTime).toSeconds(), m_power, batt_voltage, motor_current, motor_position, motor_velocity));
			Thread.sleep(25);
		}
	}

	@SuppressLint("DefaultLocale")
	void set(PrintWriter out, Instant startTime, double voltage) throws InterruptedException {
		var ch = hardwareMap.get(LynxModule.class, "Control Hub");
		var motor = (DcMotorEx) hardwareMap.dcMotor.get("motor");
		motor.setPower(voltage);
		var time = Instant.now();

		ch.getBulkData();
		var m_power = motor.getPower();
		var batt_voltage = hardwareMap.voltageSensor.iterator().next().getVoltage();
		var motor_current = motor.getCurrent(CurrentUnit.AMPS);
		var motor_position = motor.getCurrentPosition();
		var motor_velocity = motor.getVelocity(AngleUnit.RADIANS);
		out.println(String.format("%f, %f, %f, %f, %d, %f", time.since(startTime).toSeconds(), m_power, batt_voltage, motor_current, motor_position, motor_velocity));
	}
}
