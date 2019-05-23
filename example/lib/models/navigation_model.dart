import 'package:flutter/material.dart';

class NavigationModel{
  IconData icon;
   String title;

  String route;

  NavigationModel({this.title,this.icon,this.route});
}
List<NavigationModel> navigationItems =[
 NavigationModel(title: "Welcome Screen",icon: Icons.insert_chart,route: '/'),
 NavigationModel(title: "Inventory",icon: Icons.collections,route: '/inventory'),
 NavigationModel(title: "Reader Connect",icon: Icons.bluetooth_searching,route: '/devicelist'),
//  NavigationModel(title: "Search",icon: Icons.search),
//  NavigationModel(title: "Settings",icon: Icons.settings),
];