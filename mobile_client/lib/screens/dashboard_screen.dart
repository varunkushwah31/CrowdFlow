import 'package:flutter/material.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            Icon(Icons.water_drop, color: Colors.white),
            SizedBox(width: 8),
            Text('CrowdFlow India', style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            tooltip: 'Track Grievance',
            onPressed: () => Navigator.pushNamed(context, '/track'),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Jal Board Hotline Banner
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: const Color(0xFFE0F2FE),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFFBAE6FD)),
              ),
              child: const Row(
                children: [
                  Icon(Icons.phone_in_talk, color: Color(0xFF0284C7)),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Delhi Jal Board 24x7 Helpline: 1916 | MCD Toll-Free: 1533',
                      style: TextStyle(color: Color(0xFF0369A1), fontWeight: FontWeight.bold, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Civic Stats Grid
            const Text('National Infrastructure Pulse', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            Row(
              children: [
                _buildStatCard('Grievances', '16', Colors.blue),
                const SizedBox(width: 8),
                _buildStatCard('DBSCAN Hotspots', '4', Colors.orange),
                const SizedBox(width: 8),
                _buildStatCard('Escalated', '2', Colors.red),
              ],
            ),
            const SizedBox(height: 20),

            // Active Priority Clusters
            const Text('Active Municipal Clusters (Delhi NCT)', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            _buildClusterCard(
              code: 'IND-CLUST-1686',
              location: 'Ward 85 - Karol Bagh (Pusa Road)',
              cause: 'Main Feeder Pipe Rupture (450mm Arterial)',
              severity: 'CRITICAL',
              reportsCount: 5,
              color: Colors.red,
            ),
            const SizedBox(height: 10),
            _buildClusterCard(
              code: 'IND-CLUST-3042',
              location: 'Ward 142 - Lajpat Nagar IV',
              cause: 'Distribution Branch Fracture & Road Curb Leaks',
              severity: 'HIGH',
              reportsCount: 4,
              color: Colors.orange,
            ),
            const SizedBox(height: 10),
            _buildClusterCard(
              code: 'IND-CLUST-5510',
              location: 'Ward 210 - Mayur Vihar Phase 1',
              cause: 'Sewage Ingress & Drinking Water Cross-Contamination',
              severity: 'CRITICAL',
              reportsCount: 4,
              color: Colors.red,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => Navigator.pushNamed(context, '/report'),
        icon: const Icon(Icons.camera_alt),
        label: const Text('Report Hazard'),
        backgroundColor: const Color(0xFF0284C7),
        foregroundColor: Colors.white,
      ),
    );
  }

  Widget _buildStatCard(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey.shade200),
          boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 4)],
        ),
        child: Column(
          children: [
            Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
            const SizedBox(height: 4),
            Text(label, textAlign: TextAlign.center, style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
          ],
        ),
      ),
    );
  }

  Widget _buildClusterCard({
    required String code,
    required String location,
    required String cause,
    required String severity,
    required int reportsCount,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(code, style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF0284C7))),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(color: color.withOpacity(0.12), borderRadius: BorderRadius.circular(4)),
                child: Text(severity, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(location, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(cause, style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          const SizedBox(height: 6),
          Text('$reportsCount citizen grievances correlated', style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
        ],
      ),
    );
  }
}
