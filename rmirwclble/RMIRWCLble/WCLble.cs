using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using wclBluetooth;
using wclCommon;
using wclCommunication;

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
        string GetBLEStack();
    }

    public class WCLble : IBleInterface
    {
        private wclBluetoothManager Manager = null;
        private wclBluetoothApi BtApi;
        private wclGattClient Client = null;
        private wclBluetoothRadio Radio = null;
        private AutoResetEvent FEvent;
        private wclGattService[] Services;
        private wclGattService UEIservice;
        private wclGattCharacteristic[] Characteristics;
        private wclGattCharacteristic WriteCh;
        private wclGattCharacteristic ReadCh;
        private wclGattDescriptor[] Descriptors;
        private wclGattDescriptor CCCDdescriptor;

        private ArrayList addressList = new ArrayList();
        private ArrayList nameList = new ArrayList();
        private ArrayList rssiList = new ArrayList();
        private ArrayList incoming = new ArrayList();
        private bool scanning = false;
        private bool disconnecting = false;
        private int rssi = 1;
        private int stage = 0;
        private int inCount = 0;
        private int sentState = 0;
        private bool hasCCCD = false;
        private bool synced = true;
        private string subscription = null;
        private string bleStack = null;

        public WCLble()
        {
            wclMessageBroadcaster.SetSyncMethod(wclMessageSynchronizationKind.skNone);
            FEvent = new AutoResetEvent(false);
        }

        public string ConnectBLE(string portName)
        {
            Manager = new wclBluetoothManager();
            Manager.OnDeviceFound += new wclBluetoothDeviceEvent(Manager_OnDeviceFound);
            Manager.OnDiscoveringCompleted += new wclBluetoothResultEvent(Manager_OnDiscoveringCompleted);
            Manager.OnDiscoveringStarted += new wclBluetoothEvent(Manager_OnDiscoveringStarted);
            Client = new wclGattClient();
            Client.OnConnect += Client_OnConnect;
            Client.OnDisconnect += Client_OnDisconnect;
            Manager.Open();
            Radio = GetRadio();
            return Radio != null ? portName : null;
        }

        public void DisconnectBLE()
        {
            Manager.Close();
            FEvent.Close();
            FEvent = null;
        }

        private wclBluetoothRadio GetRadio()
        {
            int n = -1;
            if (Manager.Count == 1)
                n = 0;
            else
            {
                for (int i = 0; i < Manager.Count; i++)
                {
                    if (Manager[i].Api != wclBluetoothApi.baMicrosoft)
                    {
                        n = i;
                        break;
                    }
                }
            }
            if (n >= 0)
            {
                BtApi = Manager[n].Api;
                if (BtApi == wclBluetoothApi.baMicrosoft)
                    bleStack = "Microsoft";
                else if (BtApi == wclBluetoothApi.baBlueSoleil)
                    bleStack = "BlueSoleil";
                return Manager[n];
            }
            return null;
        }

        public void DiscoverUEI(bool start)
        {
            if (start)
            {
                addressList.Clear();
                nameList.Clear();
                rssiList.Clear();
                int Res = Radio.Discover(120, wclBluetoothDiscoverKind.dkBle);
                scanning = Res == wclErrors.WCL_E_SUCCESS ? true : false;
                //Console.WriteLine("Scanning state = " + scanning);
            }
            else
            {
                //Console.WriteLine("Scanning terminated");
                scanning = false;
                Radio.Terminate();
            }
        }

        void Manager_OnDiscoveringStarted(object Sender, wclBluetoothRadio Radio)
        {
            //Console.WriteLine("Discovering started");
        }

        void Manager_OnDiscoveringCompleted(object Sender, wclBluetoothRadio Radio, int Error)
        {
            //Console.WriteLine("Discovering completed");
            if (scanning)
            {
                // Restart scan as RMIR allows scanning for 15 minutes
                int Res = Radio.Discover(120, wclBluetoothDiscoverKind.dkBle);
                scanning = Res == wclErrors.WCL_E_SUCCESS ? true : false;
            }
        }

        void Manager_OnDeviceFound(object Sender, wclBluetoothRadio Radio, long Address)
        {
            int Res = Radio.GetRemoteName(Address, out string NameOut);
            string Name = Res == wclErrors.WCL_E_SUCCESS ? NameOut : "N/A";
            Res = Radio.GetRemoteRssi(Address, out sbyte RssiOut);
            int Rssi = Res == wclErrors.WCL_E_SUCCESS ? RssiOut : 1;
            string addrRaw = Address.ToString("x12");
            string addrstr = addrRaw.Substring(0, 2);
            for (int i = 2; i < 12; i += 2)
                addrstr += ":" + addrRaw.Substring(i, 2);
            
            if (addrstr.StartsWith("48:d0:cf") && !addressList.Contains(addrstr))
            {
                //Console.WriteLine("Found " + addrstr + " name " + Name);
                addressList.Add(addrstr);
                nameList.Add(Name);
                rssiList.Add(Rssi);
            }
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
            return (int)rssiList[ndx];
        }

        public void SetDisconnecting(bool disconnecting)
        {
            this.disconnecting = disconnecting;
        }

        public bool IsDisconnecting()
        {
            /*
            wclClientState cs = Client.State;
            if (cs == wclClientState.csDisconnecting || cs == wclClientState.csDisconnected)
                disconnecting = true;
            */
            return disconnecting;
        }

        public bool IsScanning()
        {
            return scanning;
        }

        public string DisconnectUEI()
        {
            Subscribe(false);
            if (Client.State == wclClientState.csDisconnected)
                return "Already disconnected";
            int Res = Client.Disconnect();
            return Res == wclErrors.WCL_E_SUCCESS ? "Disconnection succeeded" : "Disconnection failed";
        }


        public bool ConnectUEI(string address)
        {
            int ndx = addressList.IndexOf(address);
            string addrStr = "0x";
            for (int i = 0; i < 18; i += 3)
                addrStr += address.Substring(i, 2);
            long addrVal = Convert.ToInt64(addrStr, 16);
            Client.Address = addrVal;
            stage = 1;
            if (Radio != null)
            {
                int Res = Client.Connect(Radio);
                FEvent.WaitOne(2000);
                if (Res != wclErrors.WCL_E_SUCCESS)
                    return false;
                else
                {
                    Res = Radio.GetRemoteRssi(addrVal, out sbyte rssiOut);
                    rssi = Res == wclErrors.WCL_E_SUCCESS ? rssiOut: 1;
                }
                stage = 2;
                return true;
            }
            return false;
        }

        void Client_OnConnect(Object obj, int e)
        {
            FEvent.Set();
        }

        void Client_OnDisconnect(Object obj, int e)
        {
            disconnecting = true;
        }

        public bool DiscoverServices()
        {
            Services = null;
            bool UEIServiceFound = false;

            int Res = Client.ReadServices(wclGattOperationFlag.goReadFromDevice, out Services);
            if (Res != wclErrors.WCL_E_SUCCESS || Services == null)
                return false;

            stage = 3;
            foreach (wclGattService Service in Services)
            {
                String s;
                if (Service.Uuid.IsShortUuid)
                    s = Service.Uuid.ShortUuid.ToString("X4");
                else
                    s = Service.Uuid.LongUuid.ToString();
                if (s.Equals("FFE0"))
                {
                    UEIservice = Service;
                    UEIServiceFound = true;
                }
            }
            return UEIServiceFound;
        }

        public bool GetFeatures()
        {
            stage = 4;
            int Res = Client.ReadCharacteristics(UEIservice, wclGattOperationFlag.goReadFromDevice, out Characteristics);
            if (Res != wclErrors.WCL_E_SUCCESS || Characteristics == null)
                return false;

            stage = 5;
            int flags = 0;
            foreach (wclGattCharacteristic Character in Characteristics)
            {
                String s;
                if (Character.Uuid.IsShortUuid)
                    s = Character.Uuid.ShortUuid.ToString("X4");
                else
                    s = Character.Uuid.LongUuid.ToString();
                if (s.Equals("FFE1"))
                {
                    WriteCh = Character;
                    flags |= 1;
                }
                else if (s.Equals("FFE2"))
                {
                    ReadCh = Character;
                    flags |= 2;
                }
            }
            if (flags != 3)
                return false;

            stage = 6;
            Res = Client.ReadDescriptors(ReadCh, wclGattOperationFlag.goReadFromDevice, out Descriptors);
            if (Res != wclErrors.WCL_E_SUCCESS || Descriptors == null)
                return false;

            stage = 7;
            foreach (wclGattDescriptor Descriptor in Descriptors)
            {
                String s;
                if (Descriptor.Uuid.IsShortUuid)
                    s = Descriptor.Uuid.ShortUuid.ToString("X4");
                else
                    s = Descriptor.Uuid.LongUuid.ToString();
                if (s.Equals("2902"))
                {
                    hasCCCD = true;
                    CCCDdescriptor = Descriptor;
                }
            }
            Subscribe(true);
            Client.OnCharacteristicChanged += Client_OnCharacteristicChanged;
            return true;
        }

        void Client_OnCharacteristicChanged(object Sender, ushort Handle, byte[] Value)
        {
            inCount++;
            byte[] data = (byte[])Value.Clone();
            synced = false;
            incoming.Add(data);
            synced = true;
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


        public bool HasCCCD()
        {
            return hasCCCD;
        }

        public bool NeedsCCCD()
        {
            return BtApi == wclBluetoothApi.baMicrosoft;
        }

        private void Subscribe(bool doSubscribe)
        {
            if (doSubscribe)
            {
                int Res = Client.Subscribe(ReadCh);
                if (Res != wclErrors.WCL_E_SUCCESS)
                {
                    subscription = "Subscription failed";
                    return;
                }
                else if (!hasCCCD)
                {
                    subscription = "Subscription without CCCD succeeded";
                    return;
                }
                Res = Client.WriteClientConfiguration(ReadCh, true, wclGattOperationFlag.goReadFromDevice);
                if (Res != wclErrors.WCL_E_SUCCESS)
                {
                    subscription = "CCCD write failed";
                    return;
                }

                Res = Client.ReadDescriptorValue(CCCDdescriptor, wclGattOperationFlag.goReadFromDevice, out wclGattDescriptorValue cccdVal);
                if (Res != wclErrors.WCL_E_SUCCESS)
                {
                    subscription = "CCCD read failed";
                    return;
                }
                subscription = cccdVal.ClientCharacteristicConfiguration.IsSubscribeToNotification
                    ? "CCCD subscribed to notification" : "CCCD not subscribed";
            }
            else
            {
                int Res = Client.Unsubscribe(ReadCh);
                if (Res != wclErrors.WCL_E_SUCCESS)
                {
                    subscription = "Unsubscription failed";
                    return;
                }
                if (hasCCCD)
                {
                    Res = Client.WriteClientConfiguration(ReadCh, false, wclGattOperationFlag.goReadFromDevice);
                    if (Res != wclErrors.WCL_E_SUCCESS)
                    {
                        subscription = "CCCD write failed";
                        return;
                    }
                }
                subscription = "Unsubscription succeeded";
            }
        }

        public int GetInCount()
        {
            return inCount;
        }

        public int GetSentState()
        {
            return sentState;
        }

        public int GetStage()
        {
            return stage;
        }

        public string GetSubscription()
        {
            return subscription;
        }

        public bool IsConnected()
        {
            if (Client == null)
                return false;
            return Client.State == wclClientState.csConnected;
        }

        public int ReadSignalStrength()
        {
            return rssi;
        }

        public void SetSentState(int state)
        {
            sentState = state;
        }

        public string GetBLEStack()
        {
            return bleStack;
        }

        public void UpdateConnection(int interval_min, int interval_max, int latency, int timeout)
        {
            // Bluetooth Framework appears to have no means to update connection parameters
        }

        public void WritePacket(byte[] pkt)
        {
            int Res = Client.WriteCharacteristicValue(WriteCh, pkt, wclGattProtectionLevel.plNone);
            sentState = Res == wclErrors.WCL_E_SUCCESS ? 1 : 0;
        }
    }
}
