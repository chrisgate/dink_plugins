// For using PlatformException
import '../commons/navigation_drawer.dart';
import 'package:flutter/material.dart';

import '../theme.dart';
import 'inventory_page.dart';

class LudicAppHome extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return LudicAppHomeState();
  }
}

class LudicAppHomeState extends State<LudicAppHome> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        elevation: 0.0,
        backgroundColor: drawerBackgroundColor,
        title: Text("Ludic"),
      ),
      drawer: NavigationDrawer(),
      body: InventoryPage(),
    );
  }
}
