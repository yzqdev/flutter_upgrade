import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_xupdate/flutter_xupdate.dart';
import 'package:package_info_plus/package_info_plus.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  ///当前App版本号设为3, 服务器版本号以此为基准判断是否有新版本
  setUp(() {
    PackageInfo.setMockInitialValues(
      appName: 'flutter_xupdate',
      packageName: 'com.xuexiang.flutter_xupdate',
      version: '1.0.2',
      buildNumber: '3',
      buildSignature: 'sig',
    );
  });

  group('defaultUpdateParser', () {
    test('服务端返回码非0时解析结果为null', () async {
      const json = '{"Code": 500, "Msg": "server error"}';
      expect(await FlutterXUpdate.defaultUpdateParser(json), isNull);
    });

    test('解析普通版本更新信息', () async {
      const json = '{"Code":0,"UpdateStatus":1,"VersionCode":4,'
          '"VersionName":"1.0.3","ModifyContent":"修复已知问题",'
          '"DownloadUrl":"https://example.com/app.apk",'
          '"ApkSize":2048,"ApkMd5":"abc123"}';
      final entity = await FlutterXUpdate.defaultUpdateParser(json);

      expect(entity, isNotNull);
      expect(entity!.hasUpdate, isTrue);
      expect(entity.isForce, isFalse);
      expect(entity.versionCode, 4);
      expect(entity.versionName, '1.0.3');
      expect(entity.updateContent, '修复已知问题');
      expect(entity.downloadUrl, 'https://example.com/app.apk');
      expect(entity.apkSize, 2048);
      expect(entity.apkMd5, 'abc123');
    });

    test('UpdateStatus为2时标记为强制更新', () async {
      const json = '{"Code":0,"UpdateStatus":2,"VersionCode":4,'
          '"VersionName":"1.0.3","ModifyContent":"强制升级",'
          '"DownloadUrl":"https://example.com/app.apk"}';
      final entity = await FlutterXUpdate.defaultUpdateParser(json);

      expect(entity!.hasUpdate, isTrue);
      expect(entity.isForce, isTrue);
    });

    test('服务器版本号不高于本地版本号时无需更新', () async {
      const json = '{"Code":0,"UpdateStatus":1,"VersionCode":3,'
          '"VersionName":"1.0.2","ModifyContent":"无更新",'
          '"DownloadUrl":"https://example.com/app.apk"}';
      final entity = await FlutterXUpdate.defaultUpdateParser(json);

      expect(entity!.hasUpdate, isFalse);
    });

    test('UpdateStatus为0时无需更新', () async {
      const json = '{"Code":0,"UpdateStatus":0,"VersionCode":4,'
          '"VersionName":"1.0.3","ModifyContent":"",'
          '"DownloadUrl":"https://example.com/app.apk"}';
      final entity = await FlutterXUpdate.defaultUpdateParser(json);

      expect(entity!.hasUpdate, isFalse);
    });
  });

  group('UpdateEntity序列化', () {
    test('toMap/fromMap往返转换保持字段一致', () {
      final entity = UpdateEntity(
        hasUpdate: true,
        isForce: true,
        isIgnorable: false,
        versionCode: 4,
        versionName: '1.0.3',
        updateContent: '修复已知问题',
        downloadUrl: 'https://example.com/app.apk',
        apkSize: 2048,
        apkMd5: 'abc123',
      );

      final restored = UpdateEntity.fromMap(entity.toMap());

      expect(restored!.hasUpdate, entity.hasUpdate);
      expect(restored.isForce, entity.isForce);
      expect(restored.versionCode, entity.versionCode);
      expect(restored.versionName, entity.versionName);
      expect(restored.updateContent, entity.updateContent);
      expect(restored.downloadUrl, entity.downloadUrl);
      expect(restored.apkSize, entity.apkSize);
      expect(restored.apkMd5, entity.apkMd5);
    });

    test('fromMap入参为null时返回null', () {
      expect(UpdateEntity.fromMap(null), isNull);
      expect(UpdateInfo.fromMap(null), isNull);
    });
  });
}
