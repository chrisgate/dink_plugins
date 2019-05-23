import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'pages/device_list_page.dart';
import 'pages/inventory_page.dart';
import 'pages/welcome_screen.dart';
import 'shared_state/user.dart';

class App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: <SingleChildCloneableWidget>[
        ChangeNotifierProvider(builder: (_) => User()),
      ],
      child: MaterialApp(
        title: 'Plugin DEMO',
        debugShowCheckedModeBanner: false,
        routes: {
          '/': (context) => WelcomeScreen(),
          '/inventory': (context) => InventoryPage(),
          '/devicelist': (context) => DeviceListPage(),
        },
        // home: LudicAppHome(),
      ),
    );
  }
}
