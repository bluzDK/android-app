package com.banc.BLEManagement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class BLEDeviceInfoList {
	
	private List<BLEDeviceInfo> bleDevices;
	
	public BLEDeviceInfoList()
	{
		bleDevices = new ArrayList<BLEDeviceInfo>();
	}
	
	public void InsertOrUpdate(BLEDeviceInfo bleDevice)
	{
		Iterator<BLEDeviceInfo> deviceIter = bleDevices.iterator();
		boolean found = false;
		while(deviceIter.hasNext()){
			BLEDeviceInfo existingBleDevice = deviceIter.next();
			if (existingBleDevice.GetMAC().equalsIgnoreCase(bleDevice.GetMAC()))
			{
				found = true;
				existingBleDevice.UpdateRSSI(bleDevice.GetRSSI());
				break;
			}
		}
		if (!found)
		{
			bleDevices.add(bleDevice);
		}
	}
	public BLEDeviceInfo GetBLEDeviceInfo(int index)
	{
		return bleDevices.get(index);
	}
	public BLEDeviceInfo GetBLEDeviceInfoByAddress(String address)
	{
		for (BLEDeviceInfo b : bleDevices) {
			if (b.GetMAC() == address) {
				return b;
			}
		}
		return null;
	}

	public int GetCount()
	{
		return bleDevices.size();
	}
	
	public BLEDeviceInfoList MergeAndTakeUnique(BLEDeviceInfoList newDevices)
	{
		BLEDeviceInfoList mergedOverDevice = new BLEDeviceInfoList();
		
		Iterator<BLEDeviceInfo> deviceIter = newDevices.bleDevices.iterator();
		boolean found = false;
		while(deviceIter.hasNext()) {
			BLEDeviceInfo newBleDevice = deviceIter.next();
			if (!this.ContainsByAddress(newBleDevice))
			{
				mergedOverDevice.bleDevices.add(newBleDevice);
			}
		}
		
		return mergedOverDevice;
	}
	
	private boolean ContainsByAddress(BLEDeviceInfo bleDevice)
	{
		Iterator<BLEDeviceInfo> deviceIter = bleDevices.iterator();
		boolean found = false;
		while(deviceIter.hasNext()) {
			BLEDeviceInfo existingBleDevice = deviceIter.next();
			if (existingBleDevice.GetMAC().equalsIgnoreCase(bleDevice.GetMAC()))
			{
				found = true;
				break;
			}
		}
		return found;
	}

	public void clearNonConnected() {
		Iterator<BLEDeviceInfo> iter = bleDevices.listIterator();
		while (iter.hasNext()) {
			if (iter.next().State != BLEDeviceInfo.STATE_CONNECTED) {
				iter.remove();
			}
		}
	}
}
