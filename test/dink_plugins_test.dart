import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dink_plugins/dink_plugins.dart';

void main() {
  const MethodChannel channel = MethodChannel('dink_plugins');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
//    expect(await DinkPlugins.platformVersion, '42');
  });
}
