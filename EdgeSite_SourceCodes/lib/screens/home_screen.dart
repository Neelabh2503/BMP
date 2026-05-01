import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../theme/app_theme.dart';
import '../theme/theme_notifier.dart';
import 'dashboard_screen.dart';
import 'image_detection_screen.dart';
import 'live_detection_screen.dart';
import 'profile_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with TickerProviderStateMixin {
  int _currentIndex = 0;

  late AnimationController _navController;
  late List<AnimationController> _itemControllers;

  late final List<Widget> _screens;

  final List<_NavItem> _navItems = const [
    _NavItem(icon: Icons.dashboard_rounded, label: 'Dashboard'),
    _NavItem(icon: Icons.videocam_rounded, label: 'Live'),
    _NavItem(icon: Icons.image_search_rounded, label: 'Image'),
    _NavItem(icon: Icons.person_rounded, label: 'Profile'),
  ];

  @override
  void initState() {
    super.initState();
    _navController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _itemControllers = List.generate(
      4,
      (i) => AnimationController(
        duration: const Duration(milliseconds: 200),
        vsync: this,
      ),
    );
    _itemControllers[0].forward();
    _screens = [
      DashboardScreen(
        onLiveTap: () => _onTabTapped(1),
        onImageTap: () => _onTabTapped(2),
        onProfileTap: () => _onTabTapped(3),
      ),
      const LiveDetectionScreen(),
      const ImageDetectionScreen(),
      const ProfileScreen(),
    ];
  }

  @override
  void dispose() {
    _navController.dispose();
    for (final c in _itemControllers) {
      c.dispose();
    }
    super.dispose();
  }

  void _onTabTapped(int index) {
    if (index == _currentIndex) return;
    HapticFeedback.lightImpact();
    _itemControllers[_currentIndex].reverse();
    setState(() => _currentIndex = index);
    _itemControllers[index].forward();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = context.watch<ThemeNotifier>().isDark;
    final bg     = isDark ? AppTheme.darkBgDeep : AppTheme.bgDeep;

    return Scaffold(
      backgroundColor: bg,
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 220),
        transitionBuilder: (child, animation) =>
            FadeTransition(opacity: animation, child: child),
        child: KeyedSubtree(
          key: ValueKey(_currentIndex),
          child: _screens[_currentIndex],
        ),
      ),
      extendBody: true,
      bottomNavigationBar: _buildNavBar(isDark),
    );
  }

  Widget _buildNavBar(bool isDark) {
    final navBg   = isDark ? AppTheme.darkBgCard : AppTheme.bgCard;
    final primary = isDark ? AppTheme.darkPrimary : AppTheme.primary;

    return SafeArea(
      minimum: const EdgeInsets.only(bottom: 12),
      child: Container(
        margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
        height: 72,
        decoration: BoxDecoration(
          color: navBg,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: isDark ? AppTheme.darkBgElevated : AppTheme.bgElevated,
          ),
          boxShadow: isDark
              ? const [
                  BoxShadow(color: Color(0x44000000), blurRadius: 16, offset: Offset(0, 6)),
                  BoxShadow(color: Color(0x14FFFFFF), blurRadius: 16, offset: Offset(0, -2)),
                ]
              : const [
                  BoxShadow(color: Color(0x18B0C4DE), blurRadius: 16, offset: Offset(0, 6)),
                  BoxShadow(color: Color(0xBBFFFFFF), blurRadius: 16, offset: Offset(0, -2)),
                ],
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: List.generate(_navItems.length, (i) => _buildNavItem(i, isDark, primary)),
        ),
      ),
    );
  }

  Widget _buildNavItem(int index, bool isDark, Color primary) {
    final isActive = _currentIndex == index;
    final item     = _navItems[index];
    final textMuted = isDark ? AppTheme.darkTextMuted : AppTheme.textMuted;

    return GestureDetector(
      onTap: () => _onTabTapped(index),
      behavior: HitTestBehavior.opaque,
      child: AnimatedBuilder(
        animation: _itemControllers[index],
        builder: (_, __) {
          return SizedBox(
            width: 72,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 220),
                  width: isActive ? 46 : 40,
                  height: isActive ? 38 : 34,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(13),
                    color: isActive ? primary.withOpacity(0.15) : Colors.transparent,
                  ),
                  child: Icon(
                    item.icon,
                    color: isActive ? primary : textMuted,
                    size: isActive ? 22 : 20,
                  ),
                ),
                const SizedBox(height: 3),
                AnimatedDefaultTextStyle(
                  duration: const Duration(milliseconds: 200),
                  style: TextStyle(
                    color: isActive ? primary : textMuted,
                    fontSize: isActive ? 11 : 10,
                    fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
                  ),
                  child: Text(item.label),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _NavItem {
  final IconData icon;
  final String label;
  const _NavItem({required this.icon, required this.label});
}
