import 'package:flutter/material.dart';
import 'screens/dashboard_screen.dart';
import 'screens/report_incident_screen.dart';
import 'screens/track_incident_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CrowdFlowApp());
}

class CrowdFlowApp extends StatelessWidget {
  const CrowdFlowApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'CrowdFlow India - Civic Water Watch',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF0284C7), // Civic Sky Blue
          primary: const Color(0xFF0284C7),
          secondary: const Color(0xFF0EA5E9),
          surface: const Color(0xFFF8FAFC),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0284C7),
          foregroundColor: Colors.white,
          elevation: 2,
        ),
      ),
      initialRoute: '/',
      routes: {
        '/': (context) => const DashboardScreen(),
        '/report': (context) => const ReportIncidentScreen(),
        '/track': (context) => const TrackIncidentScreen(),
      },
    );
  }
}
