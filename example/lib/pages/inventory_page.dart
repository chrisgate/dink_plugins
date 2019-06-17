import 'package:dink_plugins/dink_plugins.dart';
import '../commons/navigation_drawer.dart';
import 'package:flutter/material.dart';

import '../theme.dart';

class InventoryPage extends StatefulWidget {
  InventoryPage({Key key}) : super(key: key);

  _InventoryPageState createState() => _InventoryPageState();
}

class _InventoryPageState extends State<InventoryPage> {
  // Get the instance of the bluetooth
  DinkPlugins bluetooth = DinkPlugins.instance;

  // Define some variables, which will be required later
  List<TransponderData>transponders = [];

  @override
  initState() {
    super.initState();

    bluetoothConnectionCommands();
  }

  Future<void> bluetoothConnectionCommands() async {
    bluetooth.onCommandsChanged().listen((transponder) {
      setState(() {
       transponders.add(transponder);
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
       appBar: AppBar(
        elevation: 0.0,
        backgroundColor: drawerBackgroundColor,
        title: Text("Inventory"),
      ),
      drawer: NavigationDrawer(),
          body: Column(
        children: <Widget>[
          Expanded(
              child: ListView(
            padding: EdgeInsets.all(10.0),
            children: transponders.reversed.map((data) {
              return Dismissible(
                key: Key(data.epc),
                child: ListTile(
                  title: Text(data.epc),
                  leading: Text(data.tagsSeen.toString()),
                ),
              );
            }).toList(),
          )),
        ],
      ),
    );
  }
}
