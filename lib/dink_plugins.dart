import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

///
///
///
class DinkPlugins {
  static const int STATE_OFF = 10;
  static const int STATE_TURNING_ON = 11;
  static const int STATE_ON = 12;
  static const int STATE_TURNING_OFF = 13;
  static const int STATE_BLE_TURNING_ON = 14;
  static const int STATE_BLE_ON = 15;
  static const int STATE_BLE_TURNING_OFF = 16;
  static const int ERROR = -1;
  static const int CONNECTED = 1;
  static const int DISCONNECTED = 0;

  static const String namespace = 'tslplugins.chrisgate.dev';

  static const MethodChannel _channel =
  const MethodChannel('$namespace/methods');

  static const EventChannel _readChannel =
  const EventChannel('$namespace/read');

  static const EventChannel _stateChannel =
  const EventChannel('$namespace/state');
  static const EventChannel _commandsChannel =
  const EventChannel('$namespace/commands');

  final StreamController<MethodCall> _methodStreamController =
  new StreamController.broadcast();

  Stream<MethodCall> get _methodStream => _methodStreamController.stream;

  DinkPlugins._() {
    _channel.setMethodCallHandler((MethodCall call) {
      _methodStreamController.add(call);
    });
  }
  static DinkPlugins _instance = new DinkPlugins._();

  static DinkPlugins get instance => _instance;

  Stream<int> onStateChanged() =>
      _stateChannel.receiveBroadcastStream().map((buffer) => buffer);

  Stream<TransponderData> onCommandsChanged() =>
      _commandsChannel.receiveBroadcastStream().map((map)=>
          TransponderData.fromMap(new Map<String, dynamic>.from(map))
      );
  // _commandsChannel.receiveBroadcastStream().map((map)=>
  //  TransponderData.fromMap(new Map<String, dynamic>.from(map))
  //  );

  Stream<String> onRead() =>
      _readChannel.receiveBroadcastStream().map((buffer) => buffer.toString());

  Future<void> sumStream(Stream<MethodCall> stream) async {

  }

  Future<bool> get isAvailable async =>
      await _channel.invokeMethod('isAvailable');

  Future<bool> get isOn async => await _channel.invokeMethod('isOn');

  Future<bool> get isConnected async =>
      await _channel.invokeMethod('isConnected');

  Future<bool> get openSettings async =>
      await _channel.invokeMethod('openSettings');

  Future<List<BluetoothDevice>> getBondedDevices() async {
    final List list = await _channel.invokeMethod('getBondedDevices');
    return list.map((map) => BluetoothDevice.fromMap(map)).toList();
  }

  Future<dynamic> connect(BluetoothDevice device) =>
      _channel.invokeMethod('connect', device.toMap());
  Future<dynamic> connectToDevice(BluetoothDevice device) =>
      _channel.invokeMethod('connectToDevice', device.toMap());

  Future<dynamic> disconnect() => _channel.invokeMethod('disconnect');
  Future<dynamic> disconnectDevice() => _channel.invokeMethod('disconnectDevice');

  Future<dynamic> write(String message) =>
      _channel.invokeMethod('write', {'message': message});

  Future<dynamic> writeBytes(Uint8List message) =>
      _channel.invokeMethod('writeBytes', {'message': message});


}

///
///
///
class BluetoothDevice {
  final String name;
  final String address;
  final int type = 0;
  bool connected = false;

  BluetoothDevice(this.name, this.address);

  BluetoothDevice.fromMap(Map map)
      : name = map['name'],
        address = map['address'];

  Map<String, dynamic> toMap() => {
    'name': this.name,
    'address': this.address,
    'type': this.type,
    'connected': this.connected,
  };

  operator ==(Object other) {
    return other is BluetoothDevice && other.address == this.address;
  }

  @override
  int get hashCode => address.hashCode;
}
///
///
///
class TransponderData {
  final String epc;
  final String rssi;
  final String tid;
  final int tagsSeen;

  TransponderData(this.epc, this.rssi,this.tid,this.tagsSeen);

  TransponderData.fromMap(Map map)
      : epc = map['EPC'],
        rssi = map['RSSI'],
        tid  = map['TID'],
        tagsSeen = map['TagsSeen'];

  Map<String, dynamic> toMap() => {
    'EPC': this.epc,
    'RSSI': this.rssi,
    'TID': this.tid,
    'TagsSeen': this.tagsSeen,
  };

  operator ==(Object other) {
    return other is TransponderData && other.epc == this.epc;
  }

  @override
  int get hashCode => epc.hashCode;
}