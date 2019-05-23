import 'package:dink_plugins_example/commons/navigation_drawer.dart';
import 'package:flutter/material.dart';
import 'dart:async';
// For using PlatformException
import 'package:flutter/services.dart';
import '../theme.dart';

import 'package:dink_plugins/dink_plugins.dart';

class DeviceListPage extends StatefulWidget {
  DeviceListPage({Key key}) : super(key: key);
  //end  registration of routes
  _DeviceListPageState createState() => _DeviceListPageState();
}

class _DeviceListPageState extends State<DeviceListPage> {
  // Get the instance of the bluetooth
  DinkPlugins bluetooth = DinkPlugins.instance;

  // Define some variables, which will be required later
  List<BluetoothDevice> _devicesList = [];
  final _pairDevices = <BluetoothDevice>[];

  List<String> transponders = [];
  BluetoothDevice _device;
  bool _connected = false;
  bool _pressed = false;

  bool _isSelected = false;

  Widget _buildRow(BluetoothDevice device) {
    return ListTile(
      title: Text(device.name),
      trailing: Icon(
        _connected ? Icons.bluetooth_connected : Icons.bluetooth_disabled,
        color: _connected ? Colors.blueGrey : Colors.lightBlue,
      ),
      selected: _isSelected,
      onTap: () {
        setState(() {
          _device = device;
          _connected ? _disconnectDevice() : _connectToDevice();
        });
      },
    );
  }

  Widget _buildDevices() {
    return ListView.builder(
      itemBuilder: (context, i) {
        if (i.isOdd) return Divider();
        final index = i ~/ 2;

        if (index >= _pairDevices.length) {
          _pairDevices.addAll(_devicesList);
        }
        return _buildRow(_pairDevices[index]);
      },
      itemCount: _devicesList.length,
    );
  }

  @override
  initState() {
    super.initState();

    bluetoothConnectionState();
  }

  // We are using async callback for using await
  Future<void> bluetoothConnectionState() async {
    List<BluetoothDevice> devices = [];

    // To get the list of paired devices
    try {
      devices = await bluetooth.getBondedDevices();
    } on PlatformException {
      print("Error");
    }

    // For knowing when bluetooth is connected and when disconnected
    bluetooth.onStateChanged().listen((state) {
      switch (state) {
        case DinkPlugins.CONNECTED:
          setState(() {
            _connected = true;
            //  _pressed = false;
            _isSelected = false;
          });

          break;

        case DinkPlugins.DISCONNECTED:
          setState(() {
            _connected = false;
            // _pressed = false;
            _isSelected = false;
          });
          break;

        default:
          print(state);
          break;
      }
    });

    // It is an error to call [setState] unless [mounted] is true.
    if (!mounted) {
      return;
    }

    // Store the [devices] list in the [_devicesList] for accessing
    // the list outside this class
    setState(() {
      _devicesList = devices;
    });
  }

  Future<void> bluetoothConnectionCommands() async {
    bluetooth.onCommandsChanged().listen((transponder) {
      setState(() {
        transponders.add(transponder.epc);
      });
    });
  }

  // Now, its time to build the UI
  @override
  Widget build(BuildContext context) {
    return Scaffold(
       appBar: AppBar(
        elevation: 0.0,
        backgroundColor: drawerBackgroundColor,
        title: Text("Device List"),
      ),
      drawer: NavigationDrawer(),
          body: Container(
        //   decoration: linearGradient,
        child: Column(
          mainAxisSize: MainAxisSize.max,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  DropdownButton(
                      items: _getDeviceItems(),
                      onChanged: (value) => setState(() => _device = value),
                      value: _device,
                      style: TextStyle(
                          fontWeight: FontWeight.bold, color: Colors.blue)),
                  RaisedButton(
                    onPressed:_pressed ? null: _connected ? _disconnectDevice : _connectToDevice,
                    child: Text(_connected ? 'Disconnect' : 'Connect'),
                  )
                ],
              ),
            ),
            RaisedButton(
              onPressed: _openSettings,
              child: Text('Open Settings'),
            ),
            Expanded(
              child: _buildDevices(),
              //       child:StreamBuilder(
              //   stream:bluetooth.onCommandsChanged(),
              //   builder: (context, AsyncSnapshot<String> snapshot) {
              //     return Padding(
              //       padding: const EdgeInsets.symmetric(vertical: 24.0),
              //       child: Text(snapshot.hasData ? '${snapshot.data}' : ''),
              //     );
              //   },
              // ),
            )
          ],
        ),
      ),
    );
  }

  Widget _makeElement(int index) {
    if (index >= transponders.length) {
      return null;
    }
    // return ListTile(
    //   leading: Text("RFID ${index.toString()}"),
    //   title: Text(transponders[index]),
    // );
    return Container(
      padding: EdgeInsets.all(5.0),
      child: Column(
        children: <Widget>[Text(transponders[index])],
      ),
    );
  }

  // Create the List of devices to be shown in Dropdown Menu
  List<DropdownMenuItem<BluetoothDevice>> _getDeviceItems() {
    List<DropdownMenuItem<BluetoothDevice>> items = [];
    if (_devicesList.isEmpty) {
      items.add(DropdownMenuItem(
        child: Text('NONE'),
      ));
    } else {
      _devicesList.forEach((device) {
        items.add(DropdownMenuItem(
          child: Text(device.name),
          value: device,
        ));
      });
    }
    return items;
  }

  // Method to connect to bluetooth
  // Method to connect to Tsl bluetooth
  void _connectToDevice() {
    if (_device == null) {
      // show('No device selected');
    } else {
      bluetooth.isConnected.then((isConnected) {
        if (!isConnected) {
          bluetooth
              .connectToDevice(_device)
              .timeout(Duration(seconds: 10))
              .catchError((error) {
            setState((){ 
              _pressed  = false;
              _isSelected = false;
              });
          });
          setState((){ 
            _pressed = false;
            _isSelected = true;
            });
        }
      });
    }
  }

  // Method to disconnect bluetooth
  void _disconnectDevice() {
    bluetooth.disconnectDevice();
    setState(() => _pressed = true);
  }

  // Method to open settings,
  // for turning the bletooth device off
  void _openSettings() {
    bluetooth.openSettings.then((isConnected) {
      if (isConnected) {
        return;
      }
    });
  }

  void _resetTransponders() {
    setState(() {
      transponders.clear();
    });
  }
}
