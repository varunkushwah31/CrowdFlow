import 'package:flutter/material.dart';

class ReportIncidentScreen extends StatefulWidget {
  const ReportIncidentScreen({super.key});

  @override
  State<ReportIncidentScreen> createState() => _ReportIncidentScreenState();
}

class _ReportIncidentScreenState extends State<ReportIncidentScreen> {
  final _formKey = GlobalKey<FormState>();
  String _issueType = 'BURST_PIPE';
  final TextEditingController _descController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController(text: '+91 ');
  final TextEditingController _nameController = TextEditingController();
  double _lat = 28.6445;
  double _lon = 77.1950;
  bool _isSubmitting = false;

  final List<Map<String, String>> _issueTypes = [
    {'value': 'BURST_PIPE', 'label': 'Main Pipeline Burst (Jal Board Rupture)'},
    {'value': 'LEAKAGE', 'label': 'Visible Pipeline Seepage / Curb Leak'},
    {'value': 'CONTAMINATION', 'label': 'Contaminated Water (Sewage Ingress)'},
    {'value': 'SEVERE_WATERLOGGING', 'label': 'Severe Road Waterlogging / Underpass'},
    {'value': 'DRAINAGE_OVERFLOW', 'label': 'Open Nallah / Choked Stormwater Sump'},
    {'value': 'OPEN_SEWAGE', 'label': 'Open Sewage & Broken Manhole'},
    {'value': 'LOW_PRESSURE', 'label': 'Low Pressure / Booster Station Trip'},
    {'value': 'WATER_SCARCITY', 'label': 'No Municipal Supply / Tanker Delay'},
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Report Water Emergency'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Photo Evidence Card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade300, style: BorderStyle.solid),
                ),
                child: Column(
                  children: [
                    const Icon(Icons.camera_alt, size: 48, color: Color(0xFF0284C7)),
                    const SizedBox(height: 8),
                    const Text('Capture On-Site Photo / Video', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 4),
                    Text('EXIF GPS coordinates & timestamp extracted automatically',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('EXIF GPS acquired: 28.6445 N, 77.1950 E (Karol Bagh)')),
                        );
                      },
                      icon: const Icon(Icons.photo_camera),
                      label: const Text('Take Live Photo'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF0284C7),
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Issue Category
              const Text('Water Hazard Category', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 6),
              DropdownButtonFormField<String>(
                value: _issueType,
                items: _issueTypes.map((item) {
                  return DropdownMenuItem(value: item['value'], child: Text(item['label']!, style: const TextStyle(fontSize: 13)));
                }).toList(),
                onChanged: (val) => setState(() => _issueType = val!),
                decoration: InputDecoration(
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(6)),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                ),
              ),
              const SizedBox(height: 16),

              // Coordinates Box
              const Text('Indian GPS Grid (WGS 84)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 6),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      initialValue: _lat.toString(),
                      decoration: const InputDecoration(labelText: 'Latitude', border: OutlineInputBorder()),
                      readOnly: true,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextFormField(
                      initialValue: _lon.toString(),
                      decoration: const InputDecoration(labelText: 'Longitude', border: OutlineInputBorder()),
                      readOnly: true,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Description
              const Text('Description & Landmarks', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 6),
              TextFormField(
                controller: _descController,
                maxLines: 3,
                decoration: InputDecoration(
                  hintText: 'e.g., Water gushing out near Metro Pillar 128, potable line rupture...',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(6)),
                ),
              ),
              const SizedBox(height: 16),

              // Citizen Contact
              const Text('Citizen Contact (For SMS / WhatsApp Updates)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 6),
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Citizen Name', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(labelText: 'Mobile Phone (+91)', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 24),

              // Submit Button
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton.icon(
                  onPressed: _isSubmitting ? null : _submitReport,
                  icon: const Icon(Icons.send),
                  label: Text(_isSubmitting ? 'Submitting...' : 'Submit Grievance Report'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF0284C7),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _submitReport() {
    setState(() => _isSubmitting = true);
    Future.delayed(const Duration(seconds: 1), () {
      setState(() => _isSubmitting = false);
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.check_circle, color: Colors.green),
              SizedBox(width: 8),
              Text('Grievance Filed'),
            ],
          ),
          content: const Text(
            'Your water grievance has been logged as IND-H2O-7842 and routed to Delhi Jal Board (Ward 85 EE Alok Sharma). You will receive SMS updates.',
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                Navigator.pop(context);
              },
              child: const Text('OK'),
            ),
          ],
        ),
      );
    });
  }
}
