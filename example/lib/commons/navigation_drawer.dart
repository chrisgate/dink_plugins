import 'package:dink_plugins_example/models/navigation_model.dart';
import 'package:flutter/material.dart';
import 'package:dink_plugins_example/theme.dart';


import 'drawer_list_tile.dart';

class NavigationDrawer extends StatefulWidget {
  NavigationDrawer({Key key}) : super(key: key);

  _NavigationDrawerState createState() => _NavigationDrawerState();
}

class _NavigationDrawerState extends State<NavigationDrawer>
    with SingleTickerProviderStateMixin {
  double maxWidth = 250;
  double minWidth = 0;
  bool isCollapsed = false;
  int currentSelectedIndex = 0;
  AnimationController _animationController;
  Animation<double> widthAnimation;

  @override
  void initState() {
    super.initState();
    _animationController =
        AnimationController(vsync: this, duration: Duration(microseconds: 300));
    widthAnimation = Tween<double>(begin: maxWidth, end: minWidth)
        .animate(_animationController);
  }
Future<void> _pressNavigationItem(String itemName) async {
    await Navigator.of(context).pushNamed(itemName);
  }
  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _animationController,
      builder: (BuildContext context, Widget widget) =>
          getWidget(context, widget),
    );
  }

  getWidget(context, widget) {
    return Material(
      elevation: 8.0,
      child: Container(
        width: widthAnimation.value,
        color: drawerBackgroundColor,
        child: Column(
          children: <Widget>[
            SizedBox(
              height: 50.0,
            ),
            DrawerListTile(
              title: 'Chris Godwin',
              icon: Icons.person,
              animationController: _animationController,
            ),
            Divider(color: Colors.grey,height: 14.0,),
            Expanded(
              child: ListView.separated(
                separatorBuilder: (context,index){
                  return Divider(height: 12.0,);
                },
                itemBuilder: (context, index) {
                  return DrawerListTile(
                      onTap: () {
                        setState(() {
                          currentSelectedIndex = index;
                          _pressNavigationItem(navigationItems[index].route);
                        });
                      },
                      isSelected: currentSelectedIndex == index,
                      title: navigationItems[index].title,
                      icon: navigationItems[index].icon,
                      animationController: _animationController);
                },
                itemCount: navigationItems.length,
              ),
            ),
            InkWell(
                onTap: () {
                  setState(() {
                    isCollapsed = !isCollapsed;
                    isCollapsed
                        ? _animationController.reverse()
                        : _animationController.forward();
                  });
                },
                child: AnimatedIcon(
                  icon: AnimatedIcons.close_menu,
                  progress: _animationController,
                  color: Colors.white,
                  size: 40.0,
                )),
            SizedBox(
              height: 50.0,
            )
          ],
        ),
      ),
    );
  }
}
