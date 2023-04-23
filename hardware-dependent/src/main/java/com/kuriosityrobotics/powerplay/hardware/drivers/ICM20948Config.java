package com.kuriosityrobotics.powerplay.hardware.drivers;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;

//@I2cDeviceType
//@DeviceProperties(name = "ICM20948 IMU", xmlTag = "icm20948", description = "InvenSense ICM20948 9-axis IMU")
@Properties(
	value = @Platform(include = {
		"util/ICM_20948_C.h",
		"util/ICM_20948_ENUMERATIONS.h",
		"util/ICM_20948_REGISTERS.h",
		"util/ICM_20948_DMP.h",
		"util/AK09916_ENUMERATIONS.h",
		"util/AK09916_REGISTERS.h",
		"ICM_20948.h",
	}),
	target = "com.kuriosityrobotics.powerplay.hardware.drivers.ICM20948_gen"
)

public class ICM20948Config implements InfoMapper {
	static {
		// Let Android take care of loading JNI libraries for us
		System.setProperty("org.bytedeco.javacpp.loadLibraries", "false");
	}



	@Override
	public void map(InfoMap infoMap) {
		infoMap.put(new Info("ICM_20948_PWR_MGMT_1_CLKSEL_e").enumerate());
		infoMap.put(new Info("ICM_20948_LP_CONFIG_CYCLE_e").enumerate());
		infoMap.put(new Info("DMP_ODR_Registers").enumerate());
		infoMap.put(new Info("inv_icm20948_sensor").enumerate());
		infoMap.put(new Info("ICM_20948_INT_enable_t").javaNames("ICM_20948_INT_enable").pointerTypes("ICM_20948_INT_enable"));
		infoMap.put(new Info("ICM_20948::readDMPdataFromFIFO").purify());

		/*
		    ICM_20948::ICM_20948(
		            void (*jsleep)(uint32_t ms),
            	ICM_20948_Status_e (*write)(uint8_t regaddr, uint8_t *pdata, uint32_t len, void *user),
            	ICM_20948_Status_e (*read)(uint8_t regaddr, uint8_t *pdata, uint32_t len, void *user)
   			 );
		 */

		infoMap.put(new Info("ICM_20948::ICM_20948").javaText("    public ICM_20948(\n" +
			"                InterruptibleCallback jsleep,\n" +
			"                Write_byte_BytePointer_int_Pointer write,\n" +
			"                Read_byte_BytePointer_int_Pointer read\n," +
			"Pointer user" +
			"        ) { super((Pointer)null); allocate(jsleep, write, read, user); }" +
			" private native void allocate(\n" +
			"                InterruptibleCallback jsleep,\n" +
			"                Write_byte_BytePointer_int_Pointer write,\n" +
			"                Read_byte_BytePointer_int_Pointer read, Pointer user\n" +
			"        );"));
	}

	public static abstract class InterruptibleCallback extends FunctionPointer {
		public static InterruptibleCallback SLEEP = new InterruptibleCallback() {
			@Override
			public void call(@Cast("uint32_t") int i) throws InterruptedException {
				Thread.sleep(i);
			}
		};

		static {
			Loader.load();
		}

		protected InterruptibleCallback() {
			allocate();
		}

		private native void allocate();

		public abstract void call(@Cast("uint32_t") int i) throws InterruptedException;
	}


}


