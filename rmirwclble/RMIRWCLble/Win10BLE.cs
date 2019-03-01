using System;
using System.Collections;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Threading;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Devices.Enumeration;
using Windows.Storage.Streams;

namespace RMIRWin10BLE
{
    public interface IBleInterface
    {
        string ConnectBLE(string portName);
        void DisconnectBLE();
        void DiscoverUEI(bool start);
        bool ConnectUEI(string address);
        string DisconnectUEI();
        bool DiscoverServices();
        bool GetFeatures();
        string GetSubscription();
        int GetStage();

        void SetDisconnecting(bool disconnecting);
        bool IsDisconnecting();
        bool IsScanning();
        bool IsConnected();
        bool HasCCCD();
        bool NeedsCCCD();

        int GetListSize();
        string GetListItem(int ndx);
        string GetItemName(int ndx);
        int GetRssi(int ndx);
        int GetInCount();

        int GetInDataSize();
        byte[] GetInData(int ndx);

        int ReadSignalStrength();
        void WritePacket(byte[] pkt);
        int GetSentState();
        void SetSentState(int state);
        void UpdateConnection(int interval_min, int interval_max, int latency, int timeout);
    }

    public class Win10BLE : IBleInterface
    {
        private BluetoothLEAdvertisementWatcher watcher;
        private BluetoothLEDevice bleDevice = null;

        private ArrayList addressList = new ArrayList();
        private ArrayList nameList = new ArrayList();
        private ArrayList rssiList = new ArrayList();
        private ArrayList incoming = new ArrayList();
        private bool disconnecting = false;
        private bool scanning = false;
        private bool hasCCCD = false;
        private bool done = false;
        private bool synced = true;
        private string subscription = null;
        private GattCharacteristic writeCh = null;
        private GattCharacteristic readCh = null;
        private GattDeviceServicesResult services;
        private GattCharacteristicsResult ueiCharacteristics;
        private GattDescriptorsResult ueiDescriptors;
        private int stage = 0;
        private int sentState = 0;
        private int rssi = 1;
        private int inCount = 0;

        public string ConnectBLE(string portName)
        {
            return portName;
        }

        public bool ConnectUEI(string address)
        {
            int ndx = addressList.IndexOf(address);
            rssi = ndx >= 0 ? (int)(short)rssiList[ndx] : 1;

            string addrStr = "0x";
            for (int i=0; i<18; i+=3)
                addrStr += address.Substring(i, 2);
            ulong addrVal = Convert.ToUInt64(addrStr, 16);
            var selector = BluetoothLEDevice.GetDeviceSelectorFromBluetoothAddress(addrVal, BluetoothAddressType.Public);
            var deviceWatcher = DeviceInformation.CreateWatcher(selector);
            deviceWatcher.Added += DeviceWatcher_Added;
            deviceWatcher.Removed += DeviceWatcher_Removed;
            stage = 1;
            done = false;
            deviceWatcher.Start();
            while (!done) { };
            stage = 2;
            if ( bleDevice != null )
            {
                bleDevice.ConnectionStatusChanged += BleDevice_ConnectionStatusChanged;
            }
            return bleDevice != null;
        }

        private void BleDevice_ConnectionStatusChanged(BluetoothLEDevice sender, object args)
        {
            if (sender.ConnectionStatus == BluetoothConnectionStatus.Disconnected)
                disconnecting = true;
        }

        private void DeviceWatcher_Removed(DeviceWatcher sender, DeviceInformationUpdate args)
        { }

        private void DeviceWatcher_Added(DeviceWatcher sender, DeviceInformation di)
        {
            if (di.Name != null && !di.Name.Equals(""))
            {
                sender.Stop();
                var t = Task.Run(async() => await BluetoothLEDevice.FromIdAsync(di.Id));
                bleDevice = WaitTask(t, 2) == t ? t.Result : null;
                done = true;
            }
        }

        private Task WaitTask<T>(Task<T> t, int n)
        {
            CancellationTokenSource source = new CancellationTokenSource();
            source.CancelAfter(TimeSpan.FromSeconds(n));
            TaskCompletionSource<T> completionSource = new TaskCompletionSource<T>();
            source.Token.Register(() => completionSource.TrySetCanceled());
            return Task.Run(async () => await Task.WhenAny(t, completionSource.Task)).GetAwaiter().GetResult();
        }

        public void DisconnectBLE()
        { }

        public string DisconnectUEI()
        {
            if (hasCCCD)
                Subscribe(GattClientCharacteristicConfigurationDescriptorValue.None); ;

            if (readCh != null)
            {
                readCh = null;
                writeCh.Service.Dispose();
                writeCh = null;
            }
            if (bleDevice.ConnectionStatus == BluetoothConnectionStatus.Disconnected)
                return "Already disconnected";

            bleDevice.Dispose();
            DateTime waitStart = DateTime.Now;
            TimeSpan delay;
            while (bleDevice.ConnectionStatus == BluetoothConnectionStatus.Connected )
            {
                delay = DateTime.Now - waitStart;
                if (delay.TotalSeconds > 10)
                    return "Disconnection failed";
            }
            delay = DateTime.Now - waitStart;
            return "Disconnection succeeded after " + delay.TotalMilliseconds + "ms";
        }

        public bool DiscoverServices()
        {
            var t = Task.Run(async () => await bleDevice.GetGattServicesAsync());
            services = WaitTask(t, 10) == t ? t.Result : null;
            stage = 3;
            return services != null;
        }

        private void DataCharacteristic_ValueChanged(GattCharacteristic sender,
               GattValueChangedEventArgs args)
        {
            inCount++;
            byte[] data = new byte[args.CharacteristicValue.Length];
            DataReader.FromBuffer(args.CharacteristicValue).ReadBytes(data);
            synced = false;
            incoming.Add(data);
            synced = true;
        }

        public void DiscoverUEI(bool start)
        {
            if (start)
            {
                // Create Bluetooth Listener
                watcher = new BluetoothLEAdvertisementWatcher();

                watcher.ScanningMode = BluetoothLEScanningMode.Active;

                // Only activate the watcher when we're recieving values >= -80
                watcher.SignalStrengthFilter.InRangeThresholdInDBm = -80;

                // Stop watching if the value drops below -90 (user walked away)
                watcher.SignalStrengthFilter.OutOfRangeThresholdInDBm = -90;

                // Register callback for when we see an advertisements
                watcher.Received += OnAdvertisementReceived;

                // Wait 5 seconds to make sure the device is really out of range
                watcher.SignalStrengthFilter.OutOfRangeTimeout = TimeSpan.FromMilliseconds(5000);
                watcher.SignalStrengthFilter.SamplingInterval = TimeSpan.FromMilliseconds(2000);

                // Starting watching for advertisements
                addressList.Clear();
                nameList.Clear();
                scanning = true;
                watcher.Start();
            }
            else
            {
                scanning = false;
                watcher.Stop();
            }
        }

        public bool GetFeatures()
        {
            foreach (var service in services.Services)
            {
                string servUuid = service.Uuid.ToString().Substring(4, 4);
                if (!servUuid.Equals("ffe0"))
                    continue;

                var t = Task.Run(async () => await service.GetCharacteristicsAsync());
                ueiCharacteristics = WaitTask(t, 10) == t ? t.Result : null;
                break;
            }
            services = null;
            stage = 4;
            if (ueiCharacteristics == null)
                return false;

            foreach (var characteristic in ueiCharacteristics.Characteristics)
            {
                String chUuid = characteristic.Uuid.ToString().Substring(4, 4);
                var t = Task.Run(async () => await characteristic.GetDescriptorsAsync());
                ueiDescriptors = WaitTask(t, 10) == t ? t.Result : null;
                stage = 5;
                if (ueiDescriptors == null)
                    return false;
                foreach (var descriptor in ueiDescriptors.Descriptors)
                {
                    String s = descriptor.Uuid.ToString().Substring(4, 4);
                    if (chUuid.Equals("ffe2") && s.Equals("2902"))
                        hasCCCD = true;
                }
                if (chUuid.Equals("ffe1"))
                    writeCh = characteristic;
                else if (chUuid.Equals("ffe2"))
                    readCh = characteristic;
            }
            ueiCharacteristics = null;
            stage = 6;
            if (hasCCCD)
            {
                Subscribe(GattClientCharacteristicConfigurationDescriptorValue.Notify);
                readCh.ValueChanged += DataCharacteristic_ValueChanged;
            }
            stage = 7;
            ReadSubscription();
            stage = 8;
            if (subscription == null)
                return false;
            return true;
        }

        public int GetStage()
        {
            return stage;
        }
        
        private void Subscribe(GattClientCharacteristicConfigurationDescriptorValue value)
        {
            var t = Task.Run(async () => await readCh.WriteClientCharacteristicConfigurationDescriptorAsync(value));
            string result = WaitTask(t, 2) == t ? t.Result.ToString() : null;
            subscription = "CCCD subscription result: " + (result ?? "timed out");
        }
    
        public string GetSubscription()
        {
            return subscription;
        }

        private void ReadSubscription()
        {
            if (!hasCCCD)
                subscription = "CCCD absent";
            else
            {
                var t = Task.Run(async () => await readCh.ReadClientCharacteristicConfigurationDescriptorAsync());
                string result = WaitTask(t, 2) == t ? t.Result.ClientCharacteristicConfigurationDescriptor.ToString() : null;
                subscription = "CCCD state: " + (result ?? "timed out");
            }
        }

        public byte[] GetInData(int ndx)
        {
            while (!synced) { };
            byte[] data = (byte[])incoming[ndx];
            incoming.RemoveAt(ndx);
            return data;
        }

        public int GetInDataSize()
        {
            return incoming.Count;
        }

        public string GetItemName(int ndx)
        {
            return (string)nameList[ndx];
        }

        public string GetListItem(int ndx)
        {
            return (string)addressList[ndx];
        }

        public int GetListSize()
        {
            int size = Math.Min(addressList.Count, nameList.Count);
            return Math.Min(size, rssiList.Count);
        }

        public int GetRssi(int ndx)
        {
            return (int)(short)rssiList[ndx];
        }

        public int GetSentState()
        {
            return sentState;
        }

        public bool IsConnected()
        {
            if (bleDevice == null)
                return false;
            return bleDevice.ConnectionStatus == BluetoothConnectionStatus.Connected;
        }

        public bool IsDisconnecting()
        {
            return disconnecting;
        }

        public bool IsScanning()
        {
            return scanning;
        }

        public bool HasCCCD()
        {
            return hasCCCD;
        }

        public bool NeedsCCCD()
        {
            return true;
        }

        public int GetInCount()
        {
            return inCount;
        }

        public int ReadSignalStrength()
        {
            // Windows has no means of reading signal strength other than from an
            // advertisement packet
            return rssi;
        }

        public void SetDisconnecting(bool disconnecting)
        {
            this.disconnecting = disconnecting;
        }

        public void SetSentState(int state)
        {
            sentState = state;
        }

        public void UpdateConnection(int interval_min, int interval_max, int latency, int timeout)
        {
            // Windows has no means of updating connection parameters
        }

        public void WritePacket(byte[] pkt)
        {
            var t = Task.Run(async () => await writeCh.WriteValueAsync(pkt.AsBuffer(), GattWriteOption.WriteWithoutResponse));
            sentState = WaitTask(t, 2) == t && t.Result == GattCommunicationStatus.Success ? 1 : 0;
        }

        private void OnAdvertisementReceived(BluetoothLEAdvertisementWatcher watcher, BluetoothLEAdvertisementReceivedEventArgs eventArgs)
        {
            ulong address = eventArgs.BluetoothAddress;
            string addrRaw = address.ToString("x12");
            string addrstr = addrRaw.Substring(0, 2);
            for (int i = 2; i < 12; i += 2)
                addrstr += ":" + addrRaw.Substring(i, 2);

            if (addrstr.StartsWith("48:d0:cf"))
            {
                if (!addressList.Contains(addrstr))
                {
                    nameList.Add(eventArgs.Advertisement.LocalName);
                    addressList.Add(addrstr);
                    rssiList.Add(eventArgs.RawSignalStrengthInDBm);
                }
            }
        }
    }
}
