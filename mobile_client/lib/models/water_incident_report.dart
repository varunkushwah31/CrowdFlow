/// Incident report model for Flutter mobile client matching CrowdFlow backend API contract.
class WaterIncidentReport {
  final String? id;
  final String issueType; // BURST_PIPE, CONTAMINATED_WATER, LOW_PRESSURE, WATERLOGGING, OPEN_SEWAGE, OTHER
  final String? description;
  final double latitude;
  final double longitude;
  final String? address;
  final String? citizenName;
  final String? citizenPhone;
  final String? citizenEmail;
  final String? localImagePath;
  final String? remoteImageUrl;
  final DateTime timestamp;
  final bool isSynced;

  WaterIncidentReport({
    this.id,
    required this.issueType,
    this.description,
    required this.latitude,
    required this.longitude,
    this.address,
    this.citizenName,
    this.citizenPhone,
    this.citizenEmail,
    this.localImagePath,
    this.remoteImageUrl,
    DateTime? timestamp,
    this.isSynced = false,
  }) : timestamp = timestamp ?? DateTime.now();

  Map<String, dynamic> toJson() {
    return {
      'issueType': issueType,
      'description': description,
      'latitude': latitude,
      'longitude': longitude,
      'citizenName': citizenName,
      'citizenPhone': citizenPhone,
      'citizenEmail': citizenEmail,
      'imageUrl': remoteImageUrl,
    };
  }

  factory WaterIncidentReport.fromJson(Map<String, dynamic> json) {
    return WaterIncidentReport(
      id: json['reportCode'] ?? json['id']?.toString(),
      issueType: json['issueType'] ?? 'OTHER',
      description: json['description'],
      latitude: (json['latitude'] as num?)?.toDouble() ?? 0.0,
      longitude: (json['longitude'] as num?)?.toDouble() ?? 0.0,
      address: json['address'],
      citizenName: json['citizenName'],
      citizenPhone: json['citizenPhone'],
      citizenEmail: json['citizenEmail'],
      remoteImageUrl: json['imageUrl'],
      isSynced: true,
    );
  }
}
