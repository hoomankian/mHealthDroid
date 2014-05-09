package communicationManager.datareceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import communicationManager.CommunicationManager;
import communicationManager.dataStructure.Data;
import communicationManager.dataStructure.Metadata;
import communicationManager.dataStructure.ObjectMetadata.FormatType;
import communicationManager.dataStructure.ObjectData;
import communicationManager.dataStructure.ObjectData.SensorType;
import communicationManager.dataStructure.ObjectMetadata;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;


public class DeviceShimmer extends Activity implements Device {

	private Shimmer myShimmerDevice = null;
	private BluetoothAdapter myBluetoothAdapter = null;
	private String bluetoothAddress;
	private Context myContext;
	private String myName;
	private Handler myHandlerManager;
	private FormatType format;
	private int myShimmerVersion;
	public ObjectMetadata metadata;
	private boolean isStreaming;
	public ArrayList<ObjectData> buffer;
	public int cont;
	public long offsetSessionTimeStamp;
	public long offsetShimmerTimeStamp;
	public boolean firstSample;
	
	public int NUM_SAMPLE = 200; // size of the block of sample to work with
	public int MAX_SIZE = 4 * NUM_SAMPLE; //size of the buffer

	// Sensors available in a Shimmer device
	public static final int SENSOR_ACCEL = Shimmer.SENSOR_ACCEL;
	public static final int SENSOR_DACCEL = Shimmer.SENSOR_DACCEL; //only applicable for Shimmer3
	public static final int SENSOR_GYRO = Shimmer.SENSOR_GYRO;
	public static final int SENSOR_MAG = Shimmer.SENSOR_MAG;
	public static final int SENSOR_ECG = Shimmer.SENSOR_ECG;
	public static final int SENSOR_EMG = Shimmer.SENSOR_EMG;
	public static final int SENSOR_GSR = Shimmer.SENSOR_GSR;
	public static final int SENSOR_EXP_BOARD_A7 = Shimmer.SENSOR_EXP_BOARD_A7;
	public static final int SENSOR_EXP_BOARD_A0 = Shimmer.SENSOR_EXP_BOARD_A0;
	public static final int SENSOR_STRAIN = Shimmer.SENSOR_STRAIN;
	public static final int SENSOR_HEART = Shimmer.SENSOR_HEART;
	public static final int SENSOR_EXG1_24BIT = Shimmer.SENSOR_EXG1_24BIT; //only applicable for Shimmer3
	public static final int SENSOR_EXG2_24BIT = Shimmer.SENSOR_EXG2_24BIT; //only applicable for Shimmer3
	public static final int SHIMMER3_SENSOR_ECG = Shimmer.SHIMMER3_SENSOR_ECG;
	public static final int SHIMMER3_SENSOR_EMG	= Shimmer.SHIMMER3_SENSOR_EMG;
	public static final int SENSOR_EXP_BOARD = Shimmer.SENSOR_EXP_BOARD;
	public static final int SENSOR_BATT = Shimmer.SENSOR_BATT;
	public static final int SENSOR_EXT_ADC_A7 = Shimmer.SENSOR_EXT_ADC_A7; //only Applicable for Shimmer3
	public static final int SENSOR_EXT_ADC_A6 = Shimmer.SENSOR_EXT_ADC_A6; //only Applicable for Shimmer3
	public static final int SENSOR_EXT_ADC_A15 = Shimmer.SENSOR_EXT_ADC_A15;
	public static final int SENSOR_INT_ADC_A1  = Shimmer.SENSOR_INT_ADC_A1;
	public static final int SENSOR_INT_ADC_A12 = Shimmer.SENSOR_INT_ADC_A12;
	public static final int SENSOR_INT_ADC_A13 = Shimmer.SENSOR_INT_ADC_A13;
	public static final int SENSOR_INT_ADC_A14 = Shimmer.SENSOR_INT_ADC_A14;
	public static final int SENSOR_ALL_ADC_SHIMMER3 = Shimmer.SENSOR_ALL_ADC_SHIMMER3; 
	public static final int SENSOR_BMP180 = Shimmer.SENSOR_BMP180;
	public static final int SENSOR_EXG1_16BIT = Shimmer.SENSOR_EXG1_16BIT; //only applicable for Shimmer3
	public static final int SENSOR_EXG2_16BIT = Shimmer.SENSOR_EXG2_16BIT; //only applicable for Shimmer3
	
	
	// Units of measure of the Shimmer's sensors. [0] = Calibrated, [1] =
	// Uncalibrated
	public static final String[] UNITS_TIME = { "ms", "u16" };
	public static final String[] UNITS_ACCEL = { "m/(sec^2)", "u12" };
	public static final String[] UNITS_GYRO = { "deg/s", "u12" };
	public static final String[] UNITS_MAG = { "local", "i16" };
	public static final String[] UNITS_GSR = { "kohms", "u16" };
	public static final String[] UNITS_ECG = { "mV", "u12" };
	public static final String[] UNITS_EMG = { "mV", "u12" };
	public static final String[] UNITS_STRAIN = { "mV", "u12" };
	public static final String[] UNITS_HEART = { "bmp", "" };
	public static final String[] UNITS_EXP_A0 = { "mV", "u12" };
	public static final String[] UNITS_EXP_A7 = { "mV", "u12" };
	
	//libraries that can be used to connect the device
	//The purpose of having two libraries is because some Stock firmware do not implement the full Bluetooth Stack.
	//In such cases use 'gerdavax'. If problems persist consider installing an aftermarket firmware, with a mature Bluetooth stack.
	public static final String libraryDefault = "default";
	public static final String libraryGerdavax = "gerdavax";
	
	//This represents the library to be used in order to connect a device. "1" is the default library, "2" is the gervadax library. 
	public int libraryToUse = 1;
	
	static final int REQUEST_ENABLE_BT = 1;

	/**
     * Constructor. Create a new DeviceShimmer object.
     * @param context  The UI Activity Context
     * @param name  To allow the user to set a unique identifier for each Shimmer device
     * @param countiousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
     */
	public DeviceShimmer(Context context, String name, boolean continuosSync) {
		super();
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		myShimmerDevice = new Shimmer(context, mHandler, name, continuosSync);
		myContext = context;
		myName = name;
		format = FormatType.CALIBRATED;
		metadata = new ObjectMetadata();
		isStreaming = false;
		cont = 0;
		buffer = new ArrayList<ObjectData>();
		firstSample = true;

		for (int i = 0; i < MAX_SIZE; i++)
			buffer.add(i, new ObjectData(myName));

	}
	
	/**
	 * Constructor. Prepares a new Bluetooth session. Additional fields allows the device to be set up immediately.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 * @param myname  To allow the user to set a unique identifier for each Shimmer device
	 * @param samplingRate Defines the sampling rate
	 * @param accelRange Defines the Acceleration range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 * @param gsrRange Numeric value defining the desired gsr range. Valid range settings are 0 (10kOhm to 56kOhm),  1 (56kOhm to 220kOhm), 2 (220kOhm to 680kOhm), 3 (680kOhm to 4.7MOhm) and 4 (Auto Range).
	 * @param setEnabledSensors Defines the sensors to be enabled (e.g. 'Shimmer.SENSOR_ACCEL|Shimmer.SENSOR_GYRO' enables the Accelerometer and Gyroscope)
	 * @param countiousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
	 */
	public DeviceShimmer(Context context, Handler handler, String name, double samplingRate, int accelRange, int gsrRange, int setEnabledSensors, boolean continousSync){
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		myShimmerDevice = new Shimmer(context, mHandler, name, samplingRate, accelRange, gsrRange, setEnabledSensors, continousSync);
		myContext = context;
		myName = name;
		format = FormatType.CALIBRATED;
		metadata = new ObjectMetadata();
		isStreaming = false;
		cont = 0;
		buffer = new ArrayList<ObjectData>();
		firstSample = true;
		
		for (int i = 0; i < MAX_SIZE; i++)
			buffer.add(i, new ObjectData(myName));
	}


	@Override
	public boolean connect(String adress) {
		// TODO Auto-generated method stub
		if (myBluetoothAdapter == null) {
			Toast.makeText(myContext,"Device does not support Bluetooth\nExiting...",Toast.LENGTH_LONG).show();
			return false;
		}

		if (!myBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		bluetoothAddress = adress;
		if(libraryToUse==1)
			myShimmerDevice.connect(bluetoothAddress, libraryDefault);
		else
			myShimmerDevice.connect(bluetoothAddress, libraryGerdavax);
		
		Log.d("ConnectionStatus", "Connecting...");
		while (myShimmerDevice.getShimmerState() == Shimmer.STATE_CONNECTING) {}; // wait for the connecting state to finish
		if (myShimmerDevice.getShimmerState() == Shimmer.STATE_CONNECTED) {
			Log.d("ConnectionStatus", "Successful");
		} else {
			Log.d("ConnectionStatus", "Failed. Is the Bluetooth radio on the Android device switched on?");
			return false;
		}

		myShimmerVersion = myShimmerDevice.getShimmerVersion();
		
		return true;
	}

	@Override
	public boolean disconnect() {
		// TODO Auto-generated method stub
		if (myShimmerDevice != null)
			myShimmerDevice.stop();
		if(CommunicationManager.mHandlerApp!=null)
			CommunicationManager.mHandlerApp.obtainMessage(CommunicationManager.STATUS_DISCONNECTED, myName).sendToTarget();
		return true;
	}

	@Override
	public void startStreaming() {

		cont = 0;
		initBuffer();
		firstSample = true;
		myShimmerDevice.startStreaming();
		isStreaming = true;
		setMetadata();
	}

	@Override
	public void stopStreaming() {
		
		isStreaming = false;
		myShimmerDevice.stopStreaming();
		metadata.finish.setToNow();
		if(CommunicationManager.mHandlerApp!=null)
			CommunicationManager.mHandlerApp.obtainMessage(CommunicationManager.STATUS_CONNECTED, myName).sendToTarget();
	}

	// The Handler that gets the messages sent by the Shimmer device
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) { // handlers have a what identifier which is used
								// to identify the type of msg
				case Shimmer.MESSAGE_ACK_RECEIVED:
				break;
				case Shimmer.MESSAGE_DEVICE_NAME:
					Toast.makeText(myContext, "Connected to " + myShimmerDevice.getBluetoothAddress(), 
							Toast.LENGTH_SHORT).show();
				break;
				case Shimmer.MESSAGE_INQUIRY_RESPONSE:
					Toast.makeText(myContext,"Inquiry Response " + myShimmerDevice.getBluetoothAddress(), 
							Toast.LENGTH_SHORT).show();
				break;
				case Shimmer.MESSAGE_SAMPLING_RATE_RECEIVED:
				break;
				case Shimmer.MESSAGE_STATE_CHANGE:
					switch (msg.arg1) {
						case Shimmer.STATE_CONNECTED:
							if(CommunicationManager.mHandlerApp!=null)
								CommunicationManager.mHandlerApp.obtainMessage(CommunicationManager.STATUS_CONNECTED, myName).sendToTarget();
						break;
						case Shimmer.STATE_CONNECTING:
							if(CommunicationManager.mHandlerApp!=null)
								CommunicationManager.mHandlerApp.obtainMessage(CommunicationManager.STATUS_CONNECTING, myName).sendToTarget();
						break;
						case Shimmer.STATE_NONE:
							if (isStreaming) { //if it was streaming and the current state is none means that the connection was lost
								isStreaming = false;
								//the driver sends the name instead an objectData so that the communication manager know that the connection was lost
								myHandlerManager.obtainMessage(CommunicationManager.DEVICE_SHIMMER2, myName).sendToTarget();
								metadata.finish.setToNow();
							}
							
							if(CommunicationManager.mHandlerApp!=null)
								CommunicationManager.mHandlerApp.obtainMessage(CommunicationManager.STATUS_DISCONNECTED, myName).sendToTarget();
							
							break;
					}
					break;
				case Shimmer.MESSAGE_WRITE:
				break;
	
				case Shimmer.MESSAGE_READ:
					if ((msg.obj instanceof ObjectCluster)) { // within each msg an object can be include, objectclusters
															  // are used to represent the data structure of the shimmer device
						ObjectCluster objectCluster = (ObjectCluster) msg.obj;
						if(firstSample){ // this set the offset timestamp to the momment when the first sample is received					 
							offsetSessionTimeStamp = System.currentTimeMillis();
							metadata.start.setToNow();
							firstSample = false;
							offsetShimmerTimeStamp = (long) (ObjectCluster.returnFormatCluster(objectCluster.mPropertyCluster.get("Timestamp"), "CAL").mData);
							if(CommunicationManager.mHandlerApp!=null) //send a message telling the device is already streaming
								CommunicationManager.mHandlerApp.obtainMessage(CommunicationManager.STATUS_STREAMING, myName).sendToTarget();
						}
						
						SensorType sensor = null;
						Set<String> keys = objectCluster.mPropertyCluster.keySet();
						if(myShimmerVersion == 4 || myShimmerVersion == 3){ // Shimmer 3 or Shimmer 3R
							for (String k : keys) { // introduce the objectCluster into our data structure
								//SIN TERMINAR
								if (k.equals("Low Noise Accelerometer X"))
									sensor = SensorType.LOW_NOISE_ACCELEROMETER_X;
								else if (k.equals("Low Noise Accelerometer Y"))
									sensor = SensorType.LOW_NOISE_ACCELEROMETER_Y;
								else if (k.equals("Low Noise Accelerometer Z"))
									sensor = SensorType.LOW_NOISE_ACCELEROMETER_Z;
								else if (k.equals("Wide Range Accelerometer X"))
									sensor = SensorType.WIDE_RANGE_ACCELEROMETER_X;
								else if (k.equals("Wide Range Accelerometer Y"))
									sensor = SensorType.WIDE_RANGE_ACCELEROMETER_Y;
								else if (k.equals("Wide Range Accelerometer Z"))
									sensor = SensorType.WIDE_RANGE_ACCELEROMETER_Z;
								else if (k.equals("Gyroscope X"))
									sensor = SensorType.GYROSCOPE_X;
								else if (k.equals("Gyroscope Y"))
									sensor = SensorType.GYROSCOPE_Y;
								else if (k.equals("Gyroscope Z"))
									sensor = SensorType.GYROSCOPE_Z;
								else if (k.equals("Magnetometer X"))
									sensor = SensorType.MAGNETOMETER_X;
								else if (k.equals("Magnetometer Y"))
									sensor = SensorType.MAGNETOMETER_Y;
								else if (k.equals("Magnetometer Z"))
									sensor = SensorType.MAGNETOMETER_Z;
								else if (k.equals("Axis Angle A"))
									sensor = SensorType.ANGLE_AXIS_A;
								else if (k.equals("Axis Angle X"))
									sensor = SensorType.ANGLE_AXIS_X;
								else if (k.equals("Axis Angle Y"))
									sensor = SensorType.ANGLE_AXIS_Y;
								else if (k.equals("Axis Angle Z"))
									sensor = SensorType.ANGLE_AXIS_Z;
								else if (k.equals("Quartenion 0"))
									sensor = SensorType.QUARTENION0;
								else if (k.equals("Quartenion 1"))
									sensor = SensorType.QUARTENION1;
								else if (k.equals("Quartenion 2"))
									sensor = SensorType.QUARTENION2;
								else if (k.equals("Quartenion 3"))
									sensor = SensorType.QUARTENION3;
								else if (k.equals("GSR"))
									sensor = SensorType.GSR;
								else if (k.equals("VSenseBatt"))
									sensor = SensorType.V_SENSE_BATT;
								else if (k.equals("External ADC A7"))
									sensor = SensorType.EXTERNAL_ADC_A7;
								else if (k.equals("External ADC A6"))
									sensor = SensorType.EXTERNAL_ADC_A6;
								else if (k.equals("External ADC A15"))
									sensor = SensorType.EXTERNAL_ADC_A15;
								else if (k.equals("Internal ADC A1"))
									sensor = SensorType.INTERNAL_ADC_A1;
								else if (k.equals("Internal ADC A12"))
									sensor = SensorType.INTERNAL_ADC_A12;
								else if (k.equals("Internal ADC A13"))
									sensor = SensorType.INTERNAL_ADC_A13;
								else if (k.equals("Internal ADC A14"))
									sensor = SensorType.INTERNAL_ADC_A14;
								else if (k.equals("EXG1 STATUS"))
									sensor = SensorType.EXG1_STATUS;
								else if (k.equals("ECG LL-RA"))
									sensor = SensorType.ECG_LLRA;
								else if (k.equals("ECG LA-RA"))
									sensor = SensorType.ECG_LARA;
								else if (k.equals("EMG CH1"))
									sensor = SensorType.EMG_CH1;
								else if (k.equals("EMG CH2"))
									sensor = SensorType.EMG_CH2;
								else if (k.equals("EXG1 CH1"))
									sensor = SensorType.EXG1_CH1;
								else if (k.equals("EXG1 CH2"))
									sensor = SensorType.EXG1_CH2;
								else if (k.equals("EXG2 STATUS"))
									sensor = SensorType.EXG2_STATUS;
								else if (k.equals("EXG2 CH1"))
									sensor = SensorType.EXG2_CH1;
								else if (k.equals("ECG Vx-RL"))
									sensor = SensorType.ECG_VXRL;
								else if (k.equals("EXG2 CH2"))
									sensor = SensorType.EXG2_CH2;
								else if (k.equals("EXG1 CH1 16Bit"))
									sensor = SensorType.EXG1_CH1_16B;
								else if (k.equals("EXG1 CH2 16Bit"))
									sensor = SensorType.EXG1_CH2_16B;
								else if (k.equals("EXG2 CH1 16Bit"))
									sensor = SensorType.EXG2_CH1_16B;
								else if (k.equals("EXG2 CH2 16Bit"))
									sensor = SensorType.EXG2_CH2_16B;
								else if (k.equals("Pressure"))
									sensor = SensorType.PRESSURE;
								else if (k.equals("Temperature"))
									sensor = SensorType.TEMPERATURE;
								else if (k.equals("Timestamp"))
									sensor = SensorType.TIME_STAMP;
		
								if (sensor != SensorType.TIME_STAMP) {
									Collection<FormatCluster> formatCluster = objectCluster.mPropertyCluster.get(k);
									for (FormatCluster f : formatCluster) {
										if (f.mFormat.equals("CAL") && (format == FormatType.CALIBRATED))
											buffer.get(cont).hashData.get(sensor).data = (float) f.mData;
										if (f.mFormat.equals("RAW") && (format == FormatType.UNCALIBRATED))
											buffer.get(cont).hashData.get(sensor).data = (float) f.mData;
									}
								}
		
							}
						}
						else{ //shimmer 1, 2 or 2R 
							for (String k : keys) { // introduce the objectCluster into our data structure
								if (k.equals("Accelerometer X"))
									sensor = SensorType.ACCELEROMETER_X;
								else if (k.equals("Accelerometer Y"))
									sensor = SensorType.ACCELEROMETER_Y;
								else if (k.equals("Accelerometer Z"))
									sensor = SensorType.ACCELEROMETER_Z;
								else if (k.equals("Gyroscope X"))
									sensor = SensorType.GYROSCOPE_X;
								else if (k.equals("Gyroscope Y"))
									sensor = SensorType.GYROSCOPE_Y;
								else if (k.equals("Gyroscope Z"))
									sensor = SensorType.GYROSCOPE_Z;
								else if (k.equals("Magnetometer X"))
									sensor = SensorType.MAGNETOMETER_X;
								else if (k.equals("Magnetometer Y"))
									sensor = SensorType.MAGNETOMETER_Y;
								else if (k.equals("Magnetometer Z"))
									sensor = SensorType.MAGNETOMETER_Z;
								else if (k.equals("Axis Angle A"))
									sensor = SensorType.ANGLE_AXIS_A;
								else if (k.equals("Axis Angle X"))
									sensor = SensorType.ANGLE_AXIS_X;
								else if (k.equals("Axis Angle Y"))
									sensor = SensorType.ANGLE_AXIS_Y;
								else if (k.equals("Axis Angle Z"))
									sensor = SensorType.ANGLE_AXIS_Z;
								else if (k.equals("Quartenion 0"))
									sensor = SensorType.QUARTENION0;
								else if (k.equals("Quartenion 1"))
									sensor = SensorType.QUARTENION1;
								else if (k.equals("Quartenion 2"))
									sensor = SensorType.QUARTENION2;
								else if (k.equals("Quartenion 3"))
									sensor = SensorType.QUARTENION3;
								else if (k.equals("GSR"))
									sensor = SensorType.GSR;
								else if (k.equals("ECG RA-LL"))
									sensor = SensorType.ECG_RALL;
								else if (k.equals("ECG LA-LL"))
									sensor = SensorType.ECG_LALL;
								else if (k.equals("EMG"))
									sensor = SensorType.EMG;
								else if (k.equals("Strain Gauge High"))
									sensor = SensorType.STRAIN_GAUGE_HIGH;
								else if (k.equals("Strain Gauge Low"))
									sensor = SensorType.STRAIN_GAUGE_LOW;
								else if (k.equals("Heart Rate"))
									sensor = SensorType.HEART_RATE;
								else if (k.equals("ExpBoard A0"))
									sensor = SensorType.EXP_BOARDA0;
								else if (k.equals("VSenseReg"))
									sensor = SensorType.V_SENSE_REG;
								else if (k.equals("ExpBoard A7"))
									sensor = SensorType.EXP_BOARDA7;
								else if (k.equals("VSenseBatt"))
									sensor = SensorType.V_SENSE_BATT;
								else if (k.equals("Timestamp"))
									sensor = SensorType.TIME_STAMP;
		
								
								if (sensor != SensorType.TIME_STAMP) {
									Collection<FormatCluster> formatCluster = objectCluster.mPropertyCluster.get(k);
									for (FormatCluster f : formatCluster) {
										if (f.mFormat.equals("CAL") && (format == FormatType.CALIBRATED))
											buffer.get(cont).hashData.get(sensor).data = (float) f.mData;
										
										if (f.mFormat.equals("RAW") && (format == FormatType.UNCALIBRATED))
											buffer.get(cont).hashData.get(sensor).data = (float) f.mData;
									}
								}
							}
						}
						
	
						long shimmerTime = (long) (ObjectCluster.returnFormatCluster(objectCluster.mPropertyCluster.get("Timestamp"), "CAL").mData);	
						long timeToStore = offsetSessionTimeStamp + (shimmerTime - offsetShimmerTimeStamp);					

						buffer.get(cont).hashData.get(SensorType.TIME_STAMP).data = timeToStore;
						// send our data structure with the information to the manager
						if(myShimmerVersion == 4 || myShimmerVersion == 3)
							myHandlerManager.obtainMessage(CommunicationManager.DEVICE_SHIMMER3, cont, 0, buffer.get(cont)).sendToTarget();
						else
							myHandlerManager.obtainMessage(CommunicationManager.DEVICE_SHIMMER2, cont, 0, buffer.get(cont)).sendToTarget();

						cont = (cont + 1) % MAX_SIZE;
					}
				break;
				case Shimmer.MESSAGE_TOAST:
					Log.d("toast", msg.getData().getString(Shimmer.TOAST));
					Toast.makeText(myContext, msg.getData().getString(Shimmer.TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(myContext, "Bluetooth is now enabled", Toast.LENGTH_SHORT).show();
			} else {
				// User did not enable Bluetooth or an error occured
				Toast.makeText(myContext, "Bluetooth not enabled\nExiting...", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	/**
     * Gets the name given to the device
     * @return the device's name
     */
	public String getName() {
		return myName;
	}


	/**
     * Set the communication manager's handler contained in the ObjectCommunication
     * @param handler The handler contained in the ObjectCommunication
     */
	public void setHandlerManager(Handler handler) {

		this.myHandlerManager = handler;
	}

	/**
     * Sets the format of the data. It can be "calibrated" or "uncalibrated"
     * @param format It's the format of the data
     */
	public void setFormat(FormatType format) {

		if (!isStreaming())
			this.format = format;
	}
	
	public FormatType getFormat(){
		return this.format;
	}

	@Override
	public String getTableName() {
		// TODO Auto-generated method stub
		String[] split = bluetoothAddress.split(":");
		String name = "Table_";
		for (int i = 0; i < split.length; i++)
			name += split[i];

		return name;
	}

	@Override
	public String getMetadataTableName() {
		// TODO Auto-generated method stub
		String[] split = bluetoothAddress.split(":");
		String name = "Metadata_";
		for (int i = 0; i < split.length; i++)
			name += split[i];

		return name;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		if (myShimmerDevice.getShimmerState() == Shimmer.STATE_CONNECTED)
			return true;
		else
			return false;
	}
	
	@Override
	public boolean isStreaming() {

		return this.isStreaming;
	}

	@Override
	public ArrayList<SensorType> getEnabledSensors() {

		ArrayList<SensorType> sensors = new ArrayList<SensorType>();
		int s = myShimmerDevice.getEnabledSensors();

		if (((s & 0xFF) & SENSOR_ACCEL) > 0) {
			if(myShimmerVersion == 3 || myShimmerVersion == 4)
				sensors.add(SensorType.LOW_NOISE_ACCELEROMETER);
			else
				sensors.add(SensorType.ACCELEROMETER);
		}
		if (((s & 0xFF) & SENSOR_DACCEL) > 0) {
			sensors.add(SensorType.WIDE_RANGE_ACCELEROMETER);
		}
		if (((s & 0xFF) & SENSOR_GYRO) > 0) {
			sensors.add(SensorType.GYROSCOPE);
		}
		if (((s & 0xFF) & SENSOR_MAG) > 0) {
			sensors.add(SensorType.MAGNETOMETER);
		}
		if (((s & 0xFF) & SENSOR_GSR) > 0) {
			sensors.add(SensorType.GSR);
		}
		if (((s & 0xFF) & SENSOR_ECG) > 0) {
			sensors.add(SensorType.ECG);
		}
		if (((s & 0xFF) & SENSOR_EMG) > 0) {
			sensors.add(SensorType.EMG);
		}
		if (((s & 0xFF) & SENSOR_STRAIN) > 0) {
			sensors.add(SensorType.STRAIN);
		}
		if (((s & 0xFF) & SENSOR_HEART) > 0) {
			sensors.add(SensorType.HEART_RATE);
		}
		if (((s & 0xFF) & SENSOR_EXP_BOARD_A0) > 0) {
			sensors.add(SensorType.EXP_BOARDA0);
		}
		if (((s & 0xFF) & SENSOR_EXP_BOARD_A7) > 0) {
			sensors.add(SensorType.EXP_BOARDA7);
		}
		if (((s & 0xFF) & SENSOR_BATT) > 0) {
			sensors.add(SensorType.V_SENSE_BATT);
		}
		if (((s & 0xFF) & SENSOR_EXT_ADC_A6) > 0) {
			sensors.add(SensorType.EXTERNAL_ADC_A6);
		}
		if (((s & 0xFF) & SENSOR_EXT_ADC_A7) > 0) {
			sensors.add(SensorType.EXTERNAL_ADC_A7);
		}
		if (((s & 0xFF) & SENSOR_EXT_ADC_A15) > 0) {
			sensors.add(SensorType.EXTERNAL_ADC_A15);
		}
		if (((s & 0xFF) & SENSOR_INT_ADC_A1) > 0) {
			sensors.add(SensorType.INTERNAL_ADC_A1);
		}
		if (((s & 0xFF) & SENSOR_INT_ADC_A12) > 0) {
			sensors.add(SensorType.INTERNAL_ADC_A12);
		}
		if (((s & 0xFF) & SENSOR_INT_ADC_A13) > 0) {
			sensors.add(SensorType.INTERNAL_ADC_A13);
		}
		if (((s & 0xFF) & SENSOR_INT_ADC_A14) > 0) {
			sensors.add(SensorType.INTERNAL_ADC_A14);
		}
		if (((s & 0xFF) & SENSOR_ACCEL) > 0 && ((s & 0xFF) & SENSOR_MAG) > 0 && ((s & 0xFF) & SENSOR_GYRO) > 0 && myShimmerDevice.is3DOrientatioEnabled()) {
			sensors.add(SensorType.ORIENTATION);
		}
		if (((s & 0xFF) & SENSOR_EXG1_24BIT) > 0) {
			sensors.add(SensorType.EXG1_24B);
		}
		if (((s & 0xFF) & SENSOR_EXG2_24BIT) > 0) {
			sensors.add(SensorType.EXG2_24B);
		}
		if (((s & 0xFF) & SENSOR_EXG1_16BIT) > 0) {
			sensors.add(SensorType.EXG1_16B);
		}
		if (((s & 0xFF) & SENSOR_EXG2_16BIT) > 0) {
			sensors.add(SensorType.EXG2_16B);
		}
		if (((s & 0xFF) & SENSOR_BMP180) > 0) {
			sensors.add(SensorType.BMP180);
		}

		return sensors;
	}

	@Override
	public double getRate() {

		return myShimmerDevice.getSamplingRate();
	}

	@Override
	public void setMetadata() {

		//SIN TERMINAR
		int sensors = myShimmerDevice.getEnabledSensors();
		metadata.format = format;

		if (format == FormatType.CALIBRATED)
			metadata.hashMetadata.put(SensorType.TIME_STAMP, new Metadata(UNITS_TIME[0]));
		else
			metadata.hashMetadata.put(SensorType.TIME_STAMP, new Metadata(UNITS_TIME[1]));

		if (((sensors & 0xFF) & SENSOR_ACCEL) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.ACCELEROMETER,	new Metadata(UNITS_ACCEL[0]));
			else
				metadata.hashMetadata.put(SensorType.ACCELEROMETER,	new Metadata(UNITS_ACCEL[1]));
		}
		if (((sensors & 0xFF) & SENSOR_GYRO) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.GYROSCOPE, new Metadata(UNITS_GYRO[0]));
			else
				metadata.hashMetadata.put(SensorType.GYROSCOPE, new Metadata(UNITS_GYRO[1]));
		}
		if (((sensors & 0xFF) & SENSOR_MAG) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.MAGNETOMETER,new Metadata(UNITS_MAG[0]));
			else
				metadata.hashMetadata.put(SensorType.MAGNETOMETER,new Metadata(UNITS_MAG[1]));
		}
		if (((sensors & 0xFF) & SENSOR_GSR) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.GSR, new Metadata(UNITS_GSR[0]));
			else
				metadata.hashMetadata.put(SensorType.GSR, new Metadata(UNITS_GSR[1]));
		}
		if (((sensors & 0xFF) & SENSOR_ECG) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.ECG, new Metadata(UNITS_ECG[0]));
			else
				metadata.hashMetadata.put(SensorType.ECG, new Metadata(UNITS_ECG[1]));
		}
		if (((sensors & 0xFF) & SENSOR_EMG) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.EMG, new Metadata(UNITS_EMG[0]));
			else
				metadata.hashMetadata.put(SensorType.EMG, new Metadata(UNITS_EMG[1]));
		}
		if (((sensors & 0xFF) & SENSOR_STRAIN) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.STRAIN, new Metadata(UNITS_STRAIN[0]));
			else
				metadata.hashMetadata.put(SensorType.STRAIN, new Metadata(UNITS_STRAIN[1]));
		}
		if (((sensors & 0xFF) & SENSOR_HEART) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.HEART_RATE, new Metadata(UNITS_HEART[0]));

		}
		if (((sensors & 0xFF) & SENSOR_EXP_BOARD_A0) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.EXP_BOARDA0, new Metadata(UNITS_EXP_A0[0]));
			else
				metadata.hashMetadata.put(SensorType.EXP_BOARDA0, new Metadata(UNITS_EXP_A0[1]));
		}
		if (((sensors & 0xFF) & SENSOR_EXP_BOARD_A7) > 0) {
			if (format == FormatType.CALIBRATED)
				metadata.hashMetadata.put(SensorType.EXP_BOARDA7, new Metadata(UNITS_EXP_A7[0]));
			else
				metadata.hashMetadata.put(SensorType.EXP_BOARDA7, new Metadata(UNITS_EXP_A7[1]));
		}
		
		metadata.rate = myShimmerDevice.getSamplingRate();

	}

	/**
	 *  This function sets the sample rate of the external device
	 * @param rate Is the new rate.
	 */
	@Override
	public void setRate(double rate) {
		myShimmerDevice.writeSamplingRate(rate);
	}

	/**
	 * Transmits a command to the Shimmer device to enable the sensors. To
	 * enable multiple sensors an or operator should be used (e.g.
	 * writeEnabledSensors
	 * (Shimmer.SENSOR_ACCEL|Shimmer.SENSOR_GYRO|Shimmer.SENSOR_MAG)). Command
	 * should not be used consecutively.
	 * 
	 * @param enabledSensors
	 *            e.g DeviceShimmer.SENSOR_ACCEL|DeviceShimmer.SENSOR_GYRO|
	 *            DeviceShimmer.SENSOR_MAG
	 * @param nameDevice
	 *            is the device's name
	 */
	@Override
	public void writeEnabledSensors(ArrayList<SensorType> enabledSensors) {
		// TODO Auto-generated method stub

		if (!isStreaming) {
			int sensors = 0;
			for (int i = 0; i < enabledSensors.size(); i++) {
				switch (enabledSensors.get(i)) {
				case ACCELEROMETER:
					sensors = sensors | SENSOR_ACCEL;
					break;
				case GYROSCOPE:
					sensors = sensors | SENSOR_GYRO;
					break;
				case MAGNETOMETER:
					sensors = sensors | SENSOR_MAG;
					break;
				case ECG:
					sensors = sensors | SENSOR_ECG;
					break;
				case EMG:
					sensors = sensors | SENSOR_EMG;
					break;
				case GSR:
					sensors = sensors | SENSOR_GSR;
					break;
				case EXP_BOARDA0:
					sensors = sensors | SENSOR_EXP_BOARD_A0;
					break;
				case EXP_BOARDA7:
					sensors = sensors | SENSOR_EXP_BOARD_A7;
					break;
				case STRAIN:
					sensors = sensors | SENSOR_STRAIN;
					break;
				case HEART_RATE:
					sensors = sensors | SENSOR_HEART;
					break;
				}
			}

			myShimmerDevice.writeEnabledSensors(sensors);
		}
	}

	/**
     * Initialize the buffer of the driver
     */
	private void initBufferShimmer2() {

		//SIN TERMINAR
		ArrayList<SensorType> s = getEnabledSensors();
		ArrayList<SensorType> sensors = new ArrayList<SensorType>();
		for (int j = 0; j < s.size(); j++) {
			switch (s.get(j)) {
			case ACCELEROMETER:
				sensors.add(SensorType.ACCELEROMETER_X);
				sensors.add(SensorType.ACCELEROMETER_Y);
				sensors.add(SensorType.ACCELEROMETER_Z);
				break;
			case MAGNETOMETER:
				sensors.add(SensorType.MAGNETOMETER_X);
				sensors.add(SensorType.MAGNETOMETER_Y);
				sensors.add(SensorType.MAGNETOMETER_Z);
				break;
			case GYROSCOPE:
				sensors.add(SensorType.GYROSCOPE_X);
				sensors.add(SensorType.GYROSCOPE_Y);
				sensors.add(SensorType.GYROSCOPE_Z);
				break;
			case ECG:
				sensors.add(SensorType.ECG_LALL);
				sensors.add(SensorType.ECG_RALL);
				break;
			case EMG:
				sensors.add(SensorType.EMG);
				break;
			case GSR:
				sensors.add(SensorType.GSR);
				break;
			case EXP_BOARDA0:
				sensors.add(SensorType.EXP_BOARDA0);
				break;
			case EXP_BOARDA7:
				sensors.add(SensorType.EXP_BOARDA7);
				break;
			case STRAIN:
				sensors.add(SensorType.STRAIN_GAUGE_HIGH);
				sensors.add(SensorType.STRAIN_GAUGE_LOW);
				break;
			case HEART_RATE:
				sensors.add(SensorType.HEART_RATE);
				break;
			}
		}
		sensors.add(SensorType.TIME_STAMP);
		for (int i = 0; i < MAX_SIZE; i++) {
			buffer.get(i).hashData.clear();
			for (int j = 0; j < sensors.size(); j++) {
				buffer.get(i).hashData.put(sensors.get(j), new Data(0));
			}
		}
	}
	
	private void initBufferShimmer3() {

		//SIN TERMINAR
		ArrayList<SensorType> s = getEnabledSensors();
		ArrayList<SensorType> sensors = new ArrayList<SensorType>();
		for (int j = 0; j < s.size(); j++) {
			switch (s.get(j)) {
			case ACCELEROMETER:
				sensors.add(SensorType.ACCELEROMETER_X);
				sensors.add(SensorType.ACCELEROMETER_Y);
				sensors.add(SensorType.ACCELEROMETER_Z);
				break;
			case MAGNETOMETER:
				sensors.add(SensorType.MAGNETOMETER_X);
				sensors.add(SensorType.MAGNETOMETER_Y);
				sensors.add(SensorType.MAGNETOMETER_Z);
				break;
			case GYROSCOPE:
				sensors.add(SensorType.GYROSCOPE_X);
				sensors.add(SensorType.GYROSCOPE_Y);
				sensors.add(SensorType.GYROSCOPE_Z);
				break;
			case ECG:
				sensors.add(SensorType.ECG_LALL);
				sensors.add(SensorType.ECG_RALL);
				break;
			case EMG:
				sensors.add(SensorType.EMG);
				break;
			case GSR:
				sensors.add(SensorType.GSR);
				break;
			case EXP_BOARDA0:
				sensors.add(SensorType.EXP_BOARDA0);
				break;
			case EXP_BOARDA7:
				sensors.add(SensorType.EXP_BOARDA7);
				break;
			case STRAIN:
				sensors.add(SensorType.STRAIN_GAUGE_HIGH);
				sensors.add(SensorType.STRAIN_GAUGE_LOW);
				break;
			case HEART_RATE:
				sensors.add(SensorType.HEART_RATE);
				break;
			}
		}
		sensors.add(SensorType.TIME_STAMP);
		for (int i = 0; i < MAX_SIZE; i++) {
			buffer.get(i).hashData.clear();
			for (int j = 0; j < sensors.size(); j++) {
				buffer.get(i).hashData.put(sensors.get(j), new Data(0));
			}
		}
	}


	@Override
	public void setNumberOfSampleToStorage(int samples) {
		// TODO Auto-generated method stub
		this.NUM_SAMPLE = samples;
		this.MAX_SIZE = 4*NUM_SAMPLE;
	}


	@Override
	public int getNumberOfSamples() {
		// TODO Auto-generated method stub
		return this.NUM_SAMPLE;
	}


	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return this.MAX_SIZE;
	}
	
	/** This function change the default library to be used when a device is connected. For further information refer to the
	 * connect function of the driver offered by Shimmer. (The class Shimmer.java)
	 * @param libraryNumber is the number of the library to be used. 1 for the default library, and 2 for the gerdavax library
	 */
	public void changeLibraryForBluetoohConnection(int libraryNumber){
		if(libraryNumber == 1 || libraryNumber==2)
			this.libraryToUse = libraryNumber;
	}
	
	/**
	 * This function returns the version of the Shimmer device. 
	 * @return it returns "0" for Shimmer 1, "1" for Shimmer 2, "2" for Shimmer 2R, "3" for Shimmer 3 or "4" for Shimmer SR30 
	 */
	public int getShimmerVersion(){
		
		return myShimmerVersion;
	}
	
	public void enable3DOrientation(boolean orientation){
		myShimmerDevice.enable3DOrientation(orientation);
	}
	
	public boolean is3DOrientationEnabled(){
		return myShimmerDevice.is3DOrientatioEnabled();
	}
}
