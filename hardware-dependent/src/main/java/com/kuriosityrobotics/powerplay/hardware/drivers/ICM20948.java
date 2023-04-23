package com.kuriosityrobotics.powerplay.hardware.drivers;

import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.DMP_ODR_Registers.DMP_ODR_Reg_Accel;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.DMP_ODR_Registers.DMP_ODR_Reg_Gyro_Calibr;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.DMP_ODR_Registers.DMP_ODR_Reg_Quat9;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.DMP_header_bitmap_Gyro;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.DMP_header_bitmap_Quat9;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.ICM_20948_Stat_FIFOMoreDataAvail;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.ICM_20948_Stat_Ok;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.inv_icm20948_sensor.INV_ICM20948_SENSOR_GYROSCOPE;
import static com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen.inv_icm20948_sensor.INV_ICM20948_SENSOR_ORIENTATION;
import static java.lang.Math.sqrt;

import android.util.Log;

import androidx.annotation.NonNull;

import com.kuriosityrobotics.powerplay.util.Instant;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDeviceWithParameters;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.QuaternionBasedImuHelper;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

@I2cDeviceType
@DeviceProperties(name = "ICM20948", xmlTag = "ICM20948", description = "ICM-20948 9-axis IMU", builtIn = true)
public class ICM20948 extends I2cDeviceSynchDeviceWithParameters<I2cDeviceSynchSimple, IMU.Parameters> implements IMU {
	static {
		System.loadLibrary("hardware-dependent");
	}

	private static final String TAG = "ICM20948";

	private final ICM20948_gen.ICM_20948 imu;

	private static final ICM20948_gen.ICM_20948.Write_byte_BytePointer_int_Pointer writer = new ICM20948_gen.ICM_20948.Write_byte_BytePointer_int_Pointer() {
		@Override
		public int call(byte regaddr, BytePointer pdata, int len, Pointer user) {
			var device = user.getPointer(s.class).get();
			byte[] data = new byte[len];
			pdata.get(data);
			device.deviceClient.write(regaddr, data);
			return ICM_20948_Stat_Ok;
		}
	};

	private static final ICM20948_gen.ICM_20948.Read_byte_BytePointer_int_Pointer reader = new ICM20948_gen.ICM_20948.Read_byte_BytePointer_int_Pointer() {
		@Override
		public int call(byte regaddr, BytePointer pdata, int len, Pointer user) {
			var device = user.getPointer(s.class).get();
			byte[] data = device.deviceClient.read(regaddr, len);
			pdata.put(data);
			return ICM_20948_Stat_Ok;
		}
	};

	private QuaternionBasedImuHelper helper;

	public ICM20948(I2cDeviceSynchSimple deviceClient, boolean deviceClientIsOwned) {
		super(deviceClient, deviceClientIsOwned, new Parameters(new RevHubOrientationOnRobot(
			RevHubOrientationOnRobot.LogoFacingDirection.UP,
			RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));

		Log.d(TAG, "ICM20948: constructor");
		imu = new ICM20948_gen.ICM_20948(ICM20948Config.InterruptibleCallback.SLEEP, writer, reader, new s());
		super.resetDeviceConfigurationForOpMode();
	}


	@Override
	protected boolean internalInitialize(@NonNull Parameters parameters) {
		helper = new QuaternionBasedImuHelper(parameters.imuOrientationOnRobot);

		boolean success = true;

		success = imu.initializeDMP() == ICM_20948_Stat_Ok;
		success &= (imu.enableDMPSensor(INV_ICM20948_SENSOR_ORIENTATION) == ICM_20948_Stat_Ok);
		success &= (imu.enableDMPSensor(INV_ICM20948_SENSOR_GYROSCOPE) == ICM_20948_Stat_Ok);

		success &= (imu.setDMPODRrate(DMP_ODR_Reg_Quat9, 8) == ICM_20948_Stat_Ok); 
		success &= (imu.setDMPODRrate(DMP_ODR_Reg_Accel, 8) == ICM_20948_Stat_Ok);
		//success &= (imu.setDMPODRrate(DMP_ODR_Reg_Gyro, 0) == ICM_20948_Stat_Ok); 
		success &= (imu.setDMPODRrate(DMP_ODR_Reg_Gyro_Calibr, 8) == ICM_20948_Stat_Ok); 
		//success &= (imu.setDMPODRrate(DMP_ODR_Reg_Cpass, 0) == ICM_20948_Stat_Ok);
		//success &= (imu.setDMPODRrate(DMP_ODR_Reg_Cpass_Calibr, 0) == ICM_20948_Stat_Ok); 

		// Enable the FIFO
		success &= (imu.enableFIFO() == ICM_20948_Stat_Ok);

		// Enable the DMP
		success &= (imu.enableDMP() == ICM_20948_Stat_Ok);

		// Reset DMP
		success &= (imu.resetDMP() == ICM_20948_Stat_Ok);

		// Reset FIFO
		success &= (imu.resetFIFO() == ICM_20948_Stat_Ok);
		return success;
	}

	private Quaternion getRawQuaternion() throws QuaternionBasedImuHelper.FailedToRetrieveQuaternionException {
		ICM20948_gen.icm_20948_DMP_data_t data = new ICM20948_gen.icm_20948_DMP_data_t();
		imu.readDMPdataFromFIFO(data);

		if ((imu.status() == ICM_20948_Stat_Ok) || (imu.status() == ICM_20948_Stat_FIFOMoreDataAvail)) // Was valid data available?
		{
			if ((data.header() & DMP_header_bitmap_Quat9) > 0) // We have asked for orientation data so we should receive Quat9
			{
				double q1 = ((double) data.Quat9_Data_Q1()) / 1073741824.0; // Convert to double. Divide by 2^30
				double q2 = ((double) data.Quat9_Data_Q2()) / 1073741824.0; // Convert to double. Divide by 2^30
				double q3 = ((double) data.Quat9_Data_Q3()) / 1073741824.0; // Convert to double. Divide by 2^30
				double q0 = sqrt(1.0 - ((q1 * q1) + (q2 * q2) + (q3 * q3)));

				return new Quaternion((float) q0, (float) q1, (float) q2, (float) q3, Instant.now().nanos);
			}
		}

		throw new QuaternionBasedImuHelper.FailedToRetrieveQuaternionException();
	}

	private AngularVelocity getRawAngularVelocity() {
		ICM20948_gen.icm_20948_DMP_data_t data = new ICM20948_gen.icm_20948_DMP_data_t();
		imu.readDMPdataFromFIFO(data);

		if ((imu.status() == ICM_20948_Stat_Ok) || (imu.status() == ICM_20948_Stat_FIFOMoreDataAvail)) // Was valid data available?
		{
			if ((data.header() & DMP_header_bitmap_Gyro) > 0)
			{
				var scale = 2000.0f / 32768.0f;
				var x = data.Gyro_Calibr_Data_X() * scale;
				var y = data.Gyro_Calibr_Data_Y() * scale;
				var z = data.Gyro_Calibr_Data_Z() * scale;
				return new AngularVelocity(AngleUnit.DEGREES, x, y, z, Instant.now().nanos);
			}
		}

		return new AngularVelocity(AngleUnit.DEGREES, 0, 0, 0, Instant.now().nanos);
	}

	@Override
	public void resetYaw() {
		helper.resetYaw(TAG, this::getRawQuaternion, 500);
	}

	@Override
	public YawPitchRollAngles getRobotYawPitchRollAngles() {
		return helper.getRobotYawPitchRollAngles(TAG, this::getRawQuaternion);
	}

	@Override
	public Orientation getRobotOrientation(AxesReference reference, AxesOrder order, AngleUnit angleUnit) {
		return helper.getRobotOrientation(TAG, this::getRawQuaternion, reference, order, angleUnit);
	}

	@Override
	public Quaternion getRobotOrientationAsQuaternion() {
		return helper.getRobotOrientationAsQuaternion(TAG, this::getRawQuaternion, true);
	}

	@Override
	public AngularVelocity getRobotAngularVelocity(AngleUnit angleUnit) {
		return helper.getRobotAngularVelocity(getRawAngularVelocity(), angleUnit);
	}

	class s extends Pointer {
		ICM20948 get() {
			return ICM20948.this;
		}
	}

	@Override
	public Manufacturer getManufacturer() {
		return Manufacturer.Other;
	}

	@Override
	public String getDeviceName() {
		return "ICM20948";
	}
}
