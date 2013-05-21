package server;

import java.io.IOException;

import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDDeviceNotFoundException;
import com.codeminders.hidapi.HIDManager;

public class HIDManagerBaseStation extends HIDManager {
	//vendor_id=6273, product_id=336
	private static final int ICLICKER_VENDOR_ID = 6273;
	private static final int OLD_BASE_STATION_PRODUCT_ID = 336;
	
	private ClickerServer server;
	
	public HIDManagerBaseStation(ClickerServer server) throws IOException {
       super();
       this.server = server;
    }
	
	@Override
	public void deviceAdded(HIDDeviceInfo device) {
		if (device.getVendor_id() == ICLICKER_VENDOR_ID) {
			if (device.getProduct_id() == OLD_BASE_STATION_PRODUCT_ID) {
				server.baseStationAdded();
			}
		}
	}

	@Override
	public void deviceRemoved(HIDDeviceInfo device) {
		if (device.getVendor_id() == ICLICKER_VENDOR_ID) {
			if (device.getProduct_id() == OLD_BASE_STATION_PRODUCT_ID) {
				server.baseStationRemoved();
			}
		}
	}
	
	public HIDDevice openOldBaseStation() throws HIDDeviceNotFoundException, IOException {
		return openById(ICLICKER_VENDOR_ID, OLD_BASE_STATION_PRODUCT_ID, null);
	}

}
