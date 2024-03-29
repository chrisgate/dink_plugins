import 'package:flutter/material.dart';

class MenuModel {
  IconData icon;
  String title;
  String route;

  MenuModel(this.title, this.icon, this.route);
}

List<MenuModel> menu = [
  new MenuModel(
      'Home', Icons.home, 'home'),
  new MenuModel('Device', Icons.bluetooth_searching,
      'devicelist'),
  // new MenuModel('Trending template', Icons.important_devices, '/Trending'),
  // new MenuModel('ProfileOne template', Icons.group, '/ProfileOne'),
  // new MenuModel('WhasApp template', Icons.whatshot, '/WhatsApp'),
  // new MenuModel('Greenery template', Icons.threesixty, '/Greenery'),
  // new MenuModel('Progress Button', Icons.blur_circular, '/ProgressButton'),
  // new MenuModel('Daycare template', Icons.credit_card, '/Daycare'),
  // new MenuModel('Real Estate template', Icons.home, '/RealEstate'),
  // new MenuModel(
  //     'Smart Plant template', Icons.assignment_turned_in, '/SmartPlant'),
  // new MenuModel('Hospital Dashboard template', Icons.markunread_mailbox,
  //     '/HospitalDashboard'),
  // new MenuModel(
  //     'News App Concept template', Icons.open_in_new, '/NewsAppConcept'),
  // new MenuModel('Furniture template', Icons.pages, '/Furniture'),
];