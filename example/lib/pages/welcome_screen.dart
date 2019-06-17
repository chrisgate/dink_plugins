import '../commons/navigation_drawer.dart';
import 'package:flutter/material.dart';

import '../theme.dart';

class WelcomeScreen extends StatefulWidget {
  WelcomeScreen({Key key}) : super(key: key);

  _WelcomeScreenState createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  @override
  Widget build(BuildContext context) {
     return Scaffold(
      appBar: AppBar(
        elevation: 0.0,
        backgroundColor: drawerBackgroundColor,
        title: Text("Welcome Screen"),
      ),
      drawer: NavigationDrawer(),
      body: Container(
        child: Text('Welcome Screen'),
      ),
    );
  }
}