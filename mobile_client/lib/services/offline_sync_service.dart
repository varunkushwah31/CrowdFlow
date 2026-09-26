import 'dart:async';
import 'dart:io';
import '../models/water_incident_report.dart';
import 'crowdflow_api_client.dart';

/// Offline Synchronization Service.
/// Stores pending citizen reports locally when cellular connectivity is unavailable,
/// then auto-syncs when online to ensure no grievance is lost.
class OfflineSyncService {
  final CrowdFlowApiClient _apiClient;
  final List<WaterIncidentReport> _pendingQueue = [];
  bool _isSyncing = false;

  OfflineSyncService(this._apiClient);

  List<WaterIncidentReport> get pendingReports => List.unmodifiable(_pendingQueue);

  /// Enqueues a report when device has low or zero cellular signal
  void queueOfflineReport(WaterIncidentReport report) {
    _pendingQueue.add(report);
  }

  /// Triggers background flush of pending reports to municipal server
  Future<int> syncPendingReports() async {
    if (_isSyncing || _pendingQueue.isEmpty) return 0;
    _isSyncing = true;
    int syncedCount = 0;

    List<WaterIncidentReport> toRemove = [];

    for (var report in _pendingQueue) {
      try {
        if (report.localImagePath != null && File(report.localImagePath!).existsSync()) {
          await _apiClient.submitReportWithImage(
            issueType: report.issueType,
            latitude: report.latitude,
            longitude: report.longitude,
            description: report.description,
            citizenName: report.citizenName,
            citizenPhone: report.citizenPhone,
            citizenEmail: report.citizenEmail,
            imageFile: File(report.localImagePath!),
          );
        }
        toRemove.add(report);
        syncedCount++;
      } catch (e) {
        // Retain in queue for next connectivity window
        break;
      }
    }

    _pendingQueue.removeWhere((item) => toRemove.contains(item));
    _isSyncing = false;
    return syncedCount;
  }
}
