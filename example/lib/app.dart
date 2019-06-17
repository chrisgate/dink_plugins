import 'package:flutter/material.dart';
import 'pages/device_list_page.dart';
import 'pages/inventory_page.dart';
import 'pages/welcome_screen.dart';

class App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        title: 'Plugin DEMO',
        debugShowCheckedModeBanner: false,
        routes: {
          '/': (context) => WelcomeScreen(),
          '/inventory': (context) => InventoryPage(),
          '/devicelist': (context) => DeviceListPage(),
        },
        // home: LudicAppHome(),
    );
  }
}
