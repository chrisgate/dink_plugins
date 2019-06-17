import 'package:flutter/material.dart';

import '../theme.dart';

class DrawerListTile extends StatefulWidget {
  final String title;
  final IconData icon;
  final bool isSelected;
  final Function onTap;

  AnimationController animationController;
  DrawerListTile(
      {Key key,
      @required this.title,
      @required this.icon,
      @required this.animationController,
      this.isSelected = false,
      this.onTap})
      : super(key: key);

  _DrawerListTileState createState() => _DrawerListTileState();
}

class _DrawerListTileState extends State<DrawerListTile> {
  Animation<double> widthAnimation, sizedBoxAnimation;

  @override
  void initState() {
    super.initState();
    widthAnimation = Tween<double>(begin: 250.0, end: 0)
        .animate(widget.animationController);
    sizedBoxAnimation =
        Tween<double>(begin: 10.0, end: 0).animate(widget.animationController);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: widget.onTap,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(16.0)),
          color: widget.isSelected
              ? Colors.transparent.withOpacity(0.3)
              : Colors.transparent,
        ),
        width: widthAnimation.value,
        margin: EdgeInsets.symmetric(horizontal: 8.0, vertical: 8.0),
        child: Row(
          children: <Widget>[
            Icon(
              widget.icon,
              color: widget.isSelected ? selectedColor : Colors.white30,
              size: 25.0,
            ),
            SizedBox(width: sizedBoxAnimation.value),
            (widthAnimation.value >= 220)
                ? Text(
                    widget.title,
                    style: widget.isSelected
                        ? listTitleSelectedTextStyle
                        : listTitleDefaultTextStyle,
                  )
                : Container(),
          ],
        ),
      ),
    );
  }
}
