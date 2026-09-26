import 'dart:io';
import 'package:dio/dio.dart';
import '../models/water_incident_report.dart';

/// HTTP Client for interacting with CrowdFlow Spring Boot Monolith backend.
class CrowdFlowApiClient {
  final Dio _dio;
  final String baseUrl;

  CrowdFlowApiClient({this.baseUrl = 'http://10.0.2.2:8085'})
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(seconds: 15),
          receiveTimeout: const Duration(seconds: 30),
          headers: {'Accept': 'application/json'},
        ));

  /// Submits an incident report with live camera image and EXIF metadata
  Future<WaterIncidentReport> submitReportWithImage({
    required String issueType,
    required double latitude,
    required double longitude,
    String? description,
    String? citizenName,
    String? citizenPhone,
    String? citizenEmail,
    required File imageFile,
  }) async {
    String fileName = imageFile.path.split('/').last;

    FormData formData = FormData.fromMap({
      'issueType': issueType,
      'latitude': latitude,
      'longitude': longitude,
      if (description != null) 'description': description,
      if (citizenName != null) 'citizenName': citizenName,
      if (citizenPhone != null) 'citizenPhone': citizenPhone,
      if (citizenEmail != null) 'citizenEmail': citizenEmail,
      'file': await MultipartFile.fromFile(imageFile.path, filename: fileName),
    });

    Response response = await _dio.post(
      '/api/reports',
      data: formData,
      options: Options(contentType: 'multipart/form-data'),
    );

    if (response.statusCode == 200 || response.statusCode == 201) {
      return WaterIncidentReport.fromJson(response.data);
    } else {
      throw Exception('Server rejected report: ${response.statusCode}');
    }
  }

  /// Fetches nearby incidents as GeoJSON FeatureCollection
  Future<Map<String, dynamic>> fetchGeoJsonReports() async {
    Response response = await _dio.get('/api/reports/geojson');
    return response.data;
  }

  /// Dispatches OTP to citizen mobile number (+91)
  Future<bool> sendOtp(String phoneNumber) async {
    Response response = await _dio.post(
      '/api/auth/send-otp',
      data: {'phoneNumber': phoneNumber},
    );
    return response.statusCode == 200;
  }

  /// Verifies OTP and returns JWT bearer token
  Future<String?> verifyOtp(String phoneNumber, String otp) async {
    Response response = await _dio.post(
      '/api/auth/verify-otp',
      data: {
        'phoneNumber': phoneNumber,
        'otp': otp,
        'role': 'CITIZEN',
      },
    );
    if (response.statusCode == 200 && response.data != null) {
      String token = response.data['token'];
      _dio.options.headers['Authorization'] = 'Bearer $token';
      return token;
    }
    return null;
  }
}
