import 'dart:async';
import 'package:flutter/material.dart';

class TrackIncidentScreen extends StatefulWidget {
  final String? initialCode;
  const TrackIncidentScreen({super.key, this.initialCode});

  @override
  State<TrackIncidentScreen> createState() => _TrackIncidentScreenState();
}

class _TrackIncidentScreenState extends State<TrackIncidentScreen> {
  late final TextEditingController _codeController;
  bool _searched = false;
  bool _isLoading = false;
  Timer? _liveRefreshTimer;

  // Mocked/Fetched live state
  String _currentStage = "DISPATCHED";
  int _progress = 60;
  String _stageDesc = "Emergency Work Order Dispatched to Executive Engineer";
  String _fieldNotes = "450mm ductile sleeve welded at Pusa Road. Field pressure check underway.";
  String _officer = "Shri Alok Sharma (EE - Water Central)";
  String _phone = "+91 98110 23412";
  String _clusterCode = "IND-CLUST-1686";
  String _eta = "Within 4 Hours (Emergency Line Team)";

  @override
  void initState() {
    super.initState();
    _codeController = TextEditingController(text: widget.initialCode ?? 'IND-H2O-1686');
    if (widget.initialCode != null) {
      _fetchLiveStatus();
    }
  }

  @override
  void dispose() {
    _liveRefreshTimer?.cancel();
    _codeController.dispose();
    super.dispose();
  }

  void _fetchLiveStatus() {
    setState(() {
      _isLoading = true;
      _searched = true;
    });

    // Simulate network fetch from backend API
    Future.delayed(const Duration(milliseconds: 600), () {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });

        // Set up periodic live stream poll (every 5 seconds)
        _liveRefreshTimer?.cancel();
        _liveRefreshTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
          if (mounted) {
            setState(() {
              // Simulates live progress movement if active
              if (_progress < 100) {
                // Keep current live stage
              }
            });
          }
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Live Grievance Tracker'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Enter 10-Digit Tracking Code', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
            const SizedBox(height: 6),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _codeController,
                    decoration: InputDecoration(
                      hintText: 'e.g., IND-H2O-1686',
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(6)),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  onPressed: _fetchLiveStatus,
                  icon: const Icon(Icons.search),
                  label: const Text('Track Live'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF0284C7),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),

            if (_isLoading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32.0),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (_searched) ...[
              // Live Status Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.grey.shade300),
                  boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 6)],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Code + Live Badge
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(_codeController.text,
                            style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF0284C7), fontSize: 16)),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: const Color(0xFFECFDF5),
                            border: Border.all(color: const Color(0xFFA7F3D0)),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.circle, color: Color(0xFF10B981), size: 8),
                              SizedBox(width: 5),
                              Text('LIVE STREAM',
                                  style: TextStyle(color: Color(0xFF065F46), fontSize: 10, fontWeight: FontWeight.bold)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    // Progress Bar
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Resolution Progress', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                        Text('$_progress% Completed', style: const TextStyle(fontSize: 12, color: Color(0xFF0284C7), fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 6),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: LinearProgressIndicator(
                        value: _progress / 100.0,
                        minHeight: 8,
                        backgroundColor: Colors.grey.shade200,
                        valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF0284C7)),
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(color: const Color(0xFFF8FAFC), borderRadius: BorderRadius.circular(4)),
                      child: Text('Current Stage: $_stageDesc',
                          style: const TextStyle(fontSize: 11, color: Color(0xFF334155), fontWeight: FontWeight.w500)),
                    ),
                    const SizedBox(height: 14),

                    // Nodal Officer Details
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF1F5F9),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.account_balance, size: 14, color: Color(0xFF0284C7)),
                              SizedBox(width: 6),
                              Text('Delhi Jal Board - Ward 85 (Karol Bagh)',
                                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text('Nodal Officer: $_officer', style: const TextStyle(fontSize: 12)),
                          const SizedBox(height: 4),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('Helpline: 1916 | Phone: $_phone',
                                  style: const TextStyle(fontSize: 11, color: Color(0xFF0284C7), fontWeight: FontWeight.bold)),
                              Text('ETA: $_eta', style: const TextStyle(fontSize: 11, color: Colors.green, fontWeight: FontWeight.bold)),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Associated Cluster
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: const Color(0xFFEFF6FF),
                        border: Border.all(color: const Color(0xFFBFDBFE)),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.hub, color: Color(0xFF2563EB), size: 16),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Correlated into Hotspot: $_clusterCode (5 Complaints Grouped within 150m)',
                              style: const TextStyle(fontSize: 11, color: Color(0xFF1E3A8A), fontWeight: FontWeight.w600),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),

                    // 5-Stage Stepper
                    const Text('Progress Timeline', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 10),
                    _buildStepperStep(
                      title: '1. Grievance Intake & EXIF Verified',
                      desc: 'Coordinates: 28.6445 N, 77.1950 E logged via smartphone',
                      isCompleted: true,
                    ),
                    _buildStepperStep(
                      title: '2. Ward Boundary Containment',
                      desc: 'Assigned to Ward 85 Karol Bagh (Pusa Road Division)',
                      isCompleted: true,
                    ),
                    _buildStepperStep(
                      title: '3. Spatial DBSCAN Correlation',
                      desc: 'Merged into Cluster IND-CLUST-1686 (450mm Feeder Rupture)',
                      isCompleted: true,
                    ),
                    _buildStepperStep(
                      title: '4. Field Engineering Crew Dispatched',
                      desc: 'Repair unit on site with ductile iron welding gear',
                      isCompleted: true,
                      isCurrent: true,
                    ),
                    _buildStepperStep(
                      title: '5. Pipeline Restored & Verified',
                      desc: 'Pressure normalization & citizen verification',
                      isCompleted: false,
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildStepperStep({
    required String title,
    required String desc,
    required bool isCompleted,
    bool isCurrent = false,
  }) {
    Color iconColor = isCompleted ? Colors.green : (isCurrent ? const Color(0xFF0284C7) : Colors.grey.shade400);

    return Padding(
      padding: const EdgeInsets.only(bottom: 12.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            isCompleted ? Icons.check_circle : (isCurrent ? Icons.radio_button_checked : Icons.radio_button_unchecked),
            color: iconColor,
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: isCompleted || isCurrent ? Colors.black87 : Colors.grey)),
                const SizedBox(height: 2),
                Text(desc, style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
