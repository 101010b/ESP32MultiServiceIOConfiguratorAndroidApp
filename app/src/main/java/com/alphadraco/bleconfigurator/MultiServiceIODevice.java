package com.alphadraco.bleconfigurator;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class BLERequest {
    public enum Operation { read, write, notifyOn };
    public Operation operation;
    public BluetoothGattCharacteristic characteristic;
    public BLERequest(Operation _operation, BluetoothGattCharacteristic _characteristic) {
        operation = _operation;
        characteristic = _characteristic;
    }
}

class BLERequestQueue extends ArrayList<BLERequest> {
    BluetoothGatt gatt;
    public BLERequestQueue(BluetoothGatt _gatt) {
        gatt = _gatt;
    }

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (gatt == null) return;
        if (characteristic == null) return;
        gatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            if (enabled) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            gatt.writeDescriptor(descriptor);
        }
    }

    public void execNextStep() {
        if (gatt == null) return;
        if (!isEmpty()) {
            switch (get(0).operation) {
                case read:
                    gatt.readCharacteristic(get(0).characteristic);
                    break;
                case write:
                    gatt.writeCharacteristic(get(0).characteristic);
                    break;
                case notifyOn:
                    setCharacteristicNotification(get(0).characteristic, true);
                    break;
            }
        }
    }

    public boolean confirm(BLERequest.Operation operation, BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) return false;
        int idx = -1;
        for (int i=0;(i<size()) && (idx == -1);i++)
            if (get(i).characteristic.getUuid().equals(characteristic.getUuid()) && (get(i).operation == operation))
                idx = i;
        // int idx = requestQueue.indexOf(characteristic);
        if (idx >= 0) {
            remove(idx);
            return true; // Was one from the list
        }
        return false;
    }

    @Override
    public boolean add(BLERequest bleRequest) {
        boolean needsrestart = isEmpty();
        boolean result = super.add(bleRequest);
        if (needsrestart)
            execNextStep();;
        return result;
    }
}

class BLEAdds {

    public static UUID asUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID offsetUID(UUID base, int ofs) {
        byte[] data = asBytes(base);
        ByteBuffer b = ByteBuffer.wrap(data);
        short bdata = b.getShort(2);
        bdata += ofs;
        b.putShort(2, bdata);
        return asUuid(b.array());
    }

    public static String BLEOrgBase = "0000%04X-0000-1000-8000-00805F9B34FB";
    // public static String BLEGeigerBase = "D2BF%04X-8C1B-42A2-9E11-A36170A20D92";
    public static UUID ShortUUID(String base, int sval) { return UUID.fromString((String.format(base,sval))); }
    public static UUID ShortBLEUUID(int sval) { return UUID.fromString((String.format(BLEOrgBase,sval))); }

    public static UUID BLEDevInfoSvc = ShortUUID(BLEOrgBase,0x180A);
    public static UUID BLEMfgName = ShortUUID(BLEOrgBase,0x2A29);
    public static UUID BLEMdlNum = ShortUUID(BLEOrgBase,0x2A24);
    public static UUID BLESerNum = ShortUUID(BLEOrgBase,0x2A25);
    public static UUID BLEFWRev = ShortUUID(BLEOrgBase,0x2A26);
    public static UUID BLEHWRev = ShortUUID(BLEOrgBase,0x2A27);
    public static UUID BLESWRev = ShortUUID(BLEOrgBase,0x2A28);

    public UUID base;
    public UUID param;

    public UUID BLEPwd() { return offsetUID(base,1); }
    public UUID BLECfgWLANSSID() { return offsetUID(base,2); }
    public UUID BLECfgWLANPWD() { return offsetUID(base,3); }
    public UUID BLECfgWLANMQTTIP() { return offsetUID(base,4); }
    public UUID BLECfgWLANMQTTPORT() { return offsetUID(base,5); }
    public UUID BLECfgWLANMQTTUSER() { return offsetUID(base,6); }
    public UUID BLECfgWLANMQTTPWD() { return offsetUID(base,7); }

    public UUID BLEPrmStream() { return offsetUID(param,1); }
    public UUID BLEPrmPrm(int p) { return offsetUID(param,2+p); }

    public BLEAdds(UUID _base) {
        base = _base;
        param = offsetUID(base,0x1000);
    }
}

class BleCallback extends BluetoothGattCallback {
    public MultiServiceIODevice device;
    private BluetoothGattService devinfosv;
    private BluetoothGattService configsv;
    private BluetoothGattService paramsv;

    // private  ArrayList<BLERequest> requestQueue = null;
    private BLERequestQueue requestQueue = null;


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            device.connectState = true;
            gatt.discoverServices();
            device.gatt = gatt;
        } else {
            device.connectState = false;
            device.gatt = null;
            device.geigerSV = null;
            requestQueue.clear();
        }
    }

    public void requestCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null) return;
        if (characteristic == null) return;
        requestQueue.add(new BLERequest(BLERequest.Operation.read, characteristic));
    }

    public void requestCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null) return;
        if (characteristic == null) return;
        requestQueue.add(new BLERequest(BLERequest.Operation.write, characteristic));
    }

    public BLEAdds BLE = null;

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        // super.onServicesDiscovered(gatt, status);
        if (status == 0) {
            List<BluetoothGattService> services = gatt.getServices();
            requestQueue = new BLERequestQueue(gatt);
            devinfosv = gatt.getService(BLE.BLEDevInfoSvc);
            if (devinfosv != null) {
                BluetoothGattCharacteristic c;
                c = devinfosv.getCharacteristic(BLE.BLEMdlNum);
                if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
                c = devinfosv.getCharacteristic(BLE.BLESerNum);
                if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
                c = devinfosv.getCharacteristic(BLE.BLEFWRev);
                if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
                c = devinfosv.getCharacteristic(BLE.BLEHWRev);
                if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
                c = devinfosv.getCharacteristic(BLE.BLESWRev);
                if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
                c = devinfosv.getCharacteristic(BLE.BLEMfgName);
                if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
            }
            // find dedicated service base
            for (int i=0;i<services.size();i++) {
                UUID uid = services.get(i).getUuid();
                long hi = uid.getLeastSignificantBits();
                long lo = uid.getMostSignificantBits();
                if ((hi == 0x800000805F9B34FBL) && ((lo & 0xFFFF0000FFFFFFFFL) == 0x0000000000001000L)) {
                    // Is a standardized one
                } else {
                    // Is a special one
                    long v = (lo >> 32) & 0x000000000000FFFFL;
                    if (v == 0)
                        configsv = services.get(i);
                    else
                        paramsv = services.get(i);
                }
            }
            if ((configsv != null) && (paramsv != null)) {
                // Seems to be a valid device
                BLE = new BLEAdds(configsv.getUuid());
            } else {
                BLE = null;
            }
            if (BLE != null) {
                // Trigger the download of the characteristics descriptions
                BluetoothGattCharacteristic c;
                UUID uuid = BLE.BLEPrmStream();
                c = paramsv.getCharacteristic(uuid);
                if (c != null) {
                    requestQueue.add(new BLERequest(BLERequest.Operation.notifyOn, c));
                    byte[] req = new byte[4];
                    req[0] = 0;req[1] = 0; req[2] = -1; req[3] = -1;
                    c.setValue(req);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write,c));
                    device.setNoConfig();
                }
            }
        }
    }

    public void tryPassword(String password) {
        if (requestQueue == null)
            return;
        if (configsv == null)
            return;
        BluetoothGattCharacteristic c;
        c = configsv.getCharacteristic(BLE.BLEPwd());
        if (c != null) {
            c.setValue(password);
            requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
        }
        if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
        c = configsv.getCharacteristic(BLE.BLECfgWLANSSID());
        if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
        c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTIP());
        if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
        c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTPORT());
        if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
        c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTUSER());
        if (c != null) requestQueue.add(new BLERequest(BLERequest.Operation.read, c));
    }

    public void trySetPassword(String password) {
        if (requestQueue == null)
            return;
        if (configsv == null)
            return;
        BluetoothGattCharacteristic c;
        c = configsv.getCharacteristic(BLE.BLEPwd());
        if (c != null) {
            c.setValue(password);
            requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
        }
    }

    void updateConfig(MultiServiceIODevice.ConfigData what) {
        BluetoothGattCharacteristic c;
        switch (what) {
            case WLANSSID:
                c = configsv.getCharacteristic(BLE.BLECfgWLANSSID());
                if (c != null) {
                    c.setValue(device.wlanSSID);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                }
                break;
            case WLANPWD:
                c = configsv.getCharacteristic(BLE.BLECfgWLANPWD());
                if (c != null) {
                    c.setValue(device.wlanPWD);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                }
                break;
            case MQTTSERVER:
                c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTIP());
                if (c != null) {
                    c.setValue(device.mqttServer);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                }
                break;
            case MQTTPORT:
                c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTPORT());
                if (c != null) {
                    c.setValue(device.mqttPort, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                }
                break;
            case MQTTUSER:
                c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTUSER());
                if (c != null) {
                    c.setValue(device.mqttUser);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                }
                break;
            case MQTTPWD:
                c = configsv.getCharacteristic(BLE.BLECfgWLANMQTTPWD());
                if (c != null) {
                    c.setValue(device.mqttPwd);
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                }
                break;
        }
    }

    boolean[] packetlog = null;
    ByteBuffer packet = null;
    int totalbytes = 0;
    List<ConfigVariable> config = null;

    public void processPacketList() {
        // Decode Packet
        config = new ArrayList<ConfigVariable>();
        int offset = 0;
        int prm = 0;
        while (offset < totalbytes) {
            ConfigVariable cv = new ConfigVariable();
            int nextoffset = cv.setData(packet, offset);
            if (nextoffset < 0) {
                // Flush it
                packetlog = null;
                packet = null;
                totalbytes = 0;
                config = null;
                return;
            }
            offset = nextoffset;
            cv.uuid = BLE.BLEPrmPrm(prm); prm++;
            config.add(cv);
        }
        // Done
        device.setConfigList(config);
        // Flush it
        packetlog = null;
        packet = null;
        totalbytes = 0;
        config = null;
    }

    public void onRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic.getUuid().equals(BLE.BLEMdlNum)) {
            device.model = characteristic.getStringValue(0);
            device.infoUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLESerNum)) {
            device.serial = characteristic.getStringValue(0);
            device.infoUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLEFWRev)) {
            device.fwrev = characteristic.getStringValue(0);
            device.infoUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLEHWRev)) {
            device.hwrev = characteristic.getStringValue(0);
            device.infoUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLESWRev)) {
            device.swrev = characteristic.getStringValue(0);
            device.infoUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLEMfgName)) {
            device.mfg = characteristic.getStringValue(0);
            device.infoUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLEPrmStream())) {
            // Stream data
            byte[] data = characteristic.getValue();
            if (data.length < 20) return;
            ByteBuffer b = ByteBuffer.allocate(20);b.put(data);
            int pack = Short.reverseBytes(b.getShort(0));
            if (pack == 0) {
                // Reserve space
                int packets = Short.reverseBytes(b.getShort(2));
                totalbytes = Integer.reverseBytes(b.getInt(4));
                int calcpacks = totalbytes / 18;
                if (calcpacks*18 < totalbytes)
                    calcpacks++;
                if (calcpacks != (packets-1)) {
                    // Does not match
                    return;
                }
                // All good --> reserve space
                packetlog = new boolean[packets-1];
                packet = ByteBuffer.allocate((packets-1) * 18);
            } else {
                pack--;
                if (pack >= packetlog.length)
                    return;
                packetlog[pack] = true;
                for (int i=0;i<18;i++)
                    packet.put(pack*18+i,data[i+2]);
                // Check whether complete
                boolean missing = false;
                for (int i=0;i<packetlog.length;i++)
                    if (!packetlog[i])
                        missing = true;
                if (!missing) {
                    processPacketList();
                }
            }
        }
       if (characteristic.getUuid().equals(BLE.BLECfgWLANSSID())) {
            String s = characteristic.getStringValue(0);
            device.wlanSSID = s;
            device.configUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLECfgWLANMQTTIP())) {
            device.mqttServer = characteristic.getStringValue(0);
            device.configUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLECfgWLANMQTTPORT())) {
            device.mqttPort = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
            device.configUpdateOccured();
            return;
        }
        if (characteristic.getUuid().equals(BLE.BLECfgWLANMQTTUSER())) {
            device.mqttUser = characteristic.getStringValue(0);
            device.configUpdateOccured();
            return;
        }
    }

    public void updateVariable(ConfigVariable cfg) {
        BluetoothGattCharacteristic c = paramsv.getCharacteristic(cfg.uuid);
        boolean needsstart=false;
        if (c != null) {
            switch (cfg.vartype) {
                case ui32:
                    c.setValue((int)cfg.lvalue, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                    if (requestQueue.isEmpty()) needsstart = true;
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                    if (needsstart) requestQueue.execNextStep();
                    break;
                case i32:
                    c.setValue((int)cfg.lvalue, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                    if (requestQueue.isEmpty()) needsstart = true;
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                    if (needsstart) requestQueue.execNextStep();
                    break;
                case f32:
                    c.setValue(Float.floatToRawIntBits(cfg.fvalue), BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
                    if (requestQueue.isEmpty()) needsstart = true;
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                    if (needsstart) requestQueue.execNextStep();
                    break;
                case str:
                    c.setValue(cfg.svalue);
                    if (requestQueue.isEmpty()) needsstart = true;
                    requestQueue.add(new BLERequest(BLERequest.Operation.write, c));
                    if (needsstart) requestQueue.execNextStep();
                    break;
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        //super.onCharacteristicRead(gatt, characteristic, status);
        onRead(gatt, characteristic, status);
        if (requestQueue.confirm(BLERequest.Operation.read, characteristic))
            requestQueue.execNextStep();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        // super.onCharacteristicWrite(gatt, characteristic, status);
        if (requestQueue.confirm(BLERequest.Operation.write, characteristic))
            requestQueue.execNextStep();
    }


    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // super.onCharacteristicChanged(gatt, characteristic);
        onRead(gatt, characteristic, 0);
        if (requestQueue.confirm(BLERequest.Operation.read, characteristic))
            requestQueue.execNextStep();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        // super.onDescriptorWrite(gatt, descriptor, status);
        if (requestQueue.confirm(BLERequest.Operation.notifyOn, descriptor.getCharacteristic()))
            requestQueue.execNextStep();
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        // super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        // super.onReadRemoteRssi(gatt, rssi, status);
    }
}




public class MultiServiceIODevice {

    public enum ServiceState {
        offline,
        noconfig,
        configreceived,
        fullysetup
    }

    public enum ConfigData {
        WLANSSID,
        WLANPWD,
        MQTTSERVER,
        MQTTPORT,
        MQTTUSER,
        MQTTPWD
    }

    MainActivity activity = null;
    boolean connectState = false;

    public BluetoothDevice device = null;
    BluetoothGatt gatt = null;
    BluetoothGattService geigerSV = null;
    public String name;
    public String address;
    BleCallback blecallback = null;

    // Device Information Service
    public String mfg="";
    public String model="";
    public String serial="";
    public String fwrev="";
    public String hwrev="";
    public String swrev="";

    // Device Configuration
    public String wlanSSID="";
    public String wlanPWD="";
    public String mqttServer="";
    public int mqttPort=1883;
    public String mqttUser="";
    public String mqttPwd="";

    public ServiceState serviceState = ServiceState.offline;

    public List<ConfigVariable> config = null;

    public void setNoConfig() {
        if (serviceState == ServiceState.offline)
            serviceState = ServiceState.noconfig;
    }

    public void setConfigList(List<ConfigVariable> cfglist) {
        config = cfglist;
        serviceState = ServiceState.configreceived;
    }

    public String getConnectString() {
        if (!connectState)
            return "Not Connected";
        String s = "Connected to " + name + "\n";
        s = s + "Manufacturer: " + mfg + "\n";
        s = s + "Model       : " + model + " [" + serial + "]\n";
        s = s + "Revisions   : " + "FW=" + fwrev + ", HW=" + hwrev + ", SW=" + swrev + "\n";
        return s;
    }

    public void configUpdateOccured() {
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.configUpdateOccured();
                }
            });
    }

    public void infoUpdateOccured() {
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.infoUpdateOccured();
                }
            });
    }

    public void updateVariable(ConfigVariable cfg) {
        blecallback.updateVariable(cfg);
    }

    public void tick() {
        if (serviceState == ServiceState.offline) {
            // Nothing to do

        } else if (serviceState == ServiceState.noconfig) {
            // Waiting for the configuration

        } else if (serviceState == ServiceState.configreceived) {
            // Config received --> setup UI
            if (activity != null)
                activity.updateParameterFragment();
            serviceState = ServiceState.fullysetup;
        } else if (serviceState == ServiceState.fullysetup) {
            // Complete operation

        }
    }

    public MultiServiceIODevice(MainActivity _activity, BluetoothDevice _device) {
        activity = _activity;
        device = _device;
        name = device.getName();
        address = device.getAddress();
        blecallback = new BleCallback();
        blecallback.device = this;
        device.connectGatt(activity.getApplicationContext(), true, blecallback);
    }

    public void tryDevicePwd(String pwd) {
        // Enter device Password
        if (serviceState != ServiceState.fullysetup) return;
        blecallback.tryPassword(pwd);
    }

    public void trySetDevicePwd(String newpwd) {
        if (serviceState != ServiceState.fullysetup) return;
        blecallback.trySetPassword(newpwd);
    }

    public void updateConfig(ConfigData what) {
        if (serviceState != ServiceState.fullysetup) return;
        blecallback.updateConfig(what);
    }

}
