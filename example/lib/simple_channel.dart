import 'package:flutter/services.dart';

class SimpleChannel {
  MethodChannel channel=MethodChannel("dev.yzq.flutter_xupdate/method");
  dynamic  update ()async {
  return await   channel.invokeMethod("update");
  }
}