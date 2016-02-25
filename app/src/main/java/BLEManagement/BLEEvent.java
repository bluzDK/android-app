package BLEManagement;

public class BLEEvent {
    final public static int EVENT_DEVICE_DISCOVERED = 1;
    final public static int EVENT_DEVICE_LOST = 2;
    final public static int EVENT_UPDATE = 3;
    final public static int EVENT_RX_DATA = 4;
    final public static int EVENT_DEVICE_STATE_CHANGE = 5;

    public int BLEEventType;
    public int State;
    public BLEDeviceInfo DeviceInfo;
    public Object Contents;
}
