import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../theme/theme_notifier.dart';

class DashboardScreen extends StatefulWidget {
  final VoidCallback onLiveTap;
  final VoidCallback onImageTap;
  final VoidCallback onProfileTap;

  const DashboardScreen({
    super.key,
    required this.onLiveTap,
    required this.onImageTap,
    required this.onProfileTap,
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _staggerController;
  final _authService = AuthService();

  @override
  void initState() {
    super.initState();
    _staggerController = AnimationController(
      duration: const Duration(milliseconds: 900),
      vsync: this,
    )..forward();
  }

  @override
  void dispose() {
    _staggerController.dispose();
    super.dispose();
  }

  Animation<double> _itemAnimation(int index) {
    final start = index * 0.12;
    final end = (start + 0.5).clamp(0.0, 1.0);
    return CurvedAnimation(
      parent: _staggerController,
      curve: Interval(start, end, curve: Curves.easeOutCubic),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = context.watch<ThemeNotifier>().isDark;
    final user = _authService.currentUser;
    final hour = DateTime.now().hour;
    final displayName =
        user?.userMetadata?['display_name'] as String? ??
        user?.email?.split('@').first ??
        'User';
    final initials = displayName.isNotEmpty
        ? displayName
              .substring(0, displayName.contains(' ') ? 2 : 1)
              .toUpperCase()
        : 'US';
    final greeting = hour < 12
        ? 'Good Morning'
        : hour < 18
        ? 'Good Afternoon'
        : 'Good Evening';

    final bg = isDark ? AppTheme.darkBgDeep : AppTheme.bgDeep;
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final textMuted   = isDark ? AppTheme.darkTextSecondary : AppTheme.textMuted;

    return Container(
      color: bg,
      child: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverToBoxAdapter(
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '$greeting,',
                            style: TextStyle(color: textMuted, fontSize: 14),
                          ),
                          Text(
                            displayName,
                            style: TextStyle(
                              color: textPrimary,
                              fontSize: 26,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ],
                      ),
                    ),
                    _ThemeToggleButton(isDark: isDark),
                    const SizedBox(width: 10),
                    GestureDetector(
                      onTap: widget.onProfileTap,
                      child: _AvatarWidget(initials: initials, isDark: isDark),
                    ),
                  ],
                ),
              ),
            ),
          ),

          SliverToBoxAdapter(
            child: FadeTransition(
              opacity: _itemAnimation(0),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
                child: _ModelStatusBanner(isDark: isDark),
              ),
            ),
          ),

          SliverToBoxAdapter(
            child: FadeTransition(
              opacity: _itemAnimation(5),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 24, 20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Quick Actions',
                      style: TextStyle(
                        color: textPrimary,
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: _QuickActionCard(
                            icon: Icons.videocam_rounded,
                            label: 'Live\nDetection',
                            color: isDark ? AppTheme.darkPrimary : AppTheme.primary,
                            onTap: widget.onLiveTap,
                            isDark: isDark,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _QuickActionCard(
                            icon: Icons.image_search_rounded,
                            label: 'Image\nScan',
                            color: isDark ? AppTheme.darkSecondary : AppTheme.secondary,
                            onTap: widget.onImageTap,
                            isDark: isDark,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),

          const SliverToBoxAdapter(child: SizedBox(height: 110)),
        ],
      ),
    );
  }
}

class _ThemeToggleButton extends StatelessWidget {
  final bool isDark;
  const _ThemeToggleButton({required this.isDark});

  @override
  Widget build(BuildContext context) {
    final notifier = context.read<ThemeNotifier>();
    return GestureDetector(
      onTap: notifier.toggle,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        width: 56,
        height: 30,
        padding: const EdgeInsets.all(3),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          color: isDark
              ? AppTheme.darkBgElevated
              : AppTheme.bgElevated,
          boxShadow: isDark
              ? const [
                  BoxShadow(color: Color(0x44000000), blurRadius: 6, offset: Offset(2, 2)),
                  BoxShadow(color: Color(0x18FFFFFF), blurRadius: 6, offset: Offset(-2, -2)),
                ]
              : const [
                  BoxShadow(color: Color(0x18B0C4DE), blurRadius: 6, offset: Offset(2, 2)),
                  BoxShadow(color: Color(0xCCFFFFFF), blurRadius: 6, offset: Offset(-2, -2)),
                ],
        ),
        child: AnimatedAlign(
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeInOut,
          alignment: isDark ? Alignment.centerRight : Alignment.centerLeft,
          child: Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isDark ? AppTheme.darkPrimary : AppTheme.primary,
            ),
            child: Icon(
              isDark ? Icons.dark_mode_rounded : Icons.light_mode_rounded,
              size: 14,
              color: Colors.white,
            ),
          ),
        ),
      ),
    );
  }
}

class _AvatarWidget extends StatelessWidget {
  final String initials;
  final bool isDark;
  const _AvatarWidget({required this.initials, required this.isDark});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(14),
        gradient: AppTheme.primaryGradient,
        boxShadow: [
          BoxShadow(
            color: AppTheme.primary.withOpacity(0.25),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Center(
        child: Text(
          initials,
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.w800,
            fontSize: 15,
          ),
        ),
      ),
    );
  }
}

class _ModelStatusBanner extends StatefulWidget {
  final bool isDark;
  const _ModelStatusBanner({required this.isDark});

  @override
  State<_ModelStatusBanner> createState() => _ModelStatusBannerState();
}

class _ModelStatusBannerState extends State<_ModelStatusBanner>
    with SingleTickerProviderStateMixin {
  late AnimationController _pulse;

  @override
  void initState() {
    super.initState();
    _pulse = AnimationController(
      duration: const Duration(milliseconds: 1400),
      vsync: this,
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cardDecoration = widget.isDark
        ? AppTheme.neuCardDark()
        : AppTheme.neuCard();
    final textColor = widget.isDark
        ? AppTheme.darkTextPrimary
        : AppTheme.textPrimary;
    final accentColor = widget.isDark ? AppTheme.darkAccent : AppTheme.accent;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: cardDecoration,
      child: Row(
        children: [
          AnimatedBuilder(
            animation: _pulse,
            builder: (_, __) => Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: accentColor,
                boxShadow: [
                  BoxShadow(
                    color: accentColor.withOpacity(0.3 + _pulse.value * 0.25),
                    blurRadius: 6 + _pulse.value * 4,
                    spreadRadius: _pulse.value * 2,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'YOLOv8 Model · Ready',
              style: TextStyle(
                color: textColor,
                fontWeight: FontWeight.w700,
                fontSize: 14,
              ),
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: accentColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: accentColor.withOpacity(0.2)),
            ),
            child: Text(
              'ACTIVE',
              style: TextStyle(
                color: accentColor,
                fontSize: 11,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.8,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickActionCard extends StatefulWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  final bool isDark;

  const _QuickActionCard({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
    required this.isDark,
  });

  @override
  State<_QuickActionCard> createState() => _QuickActionCardState();
}

class _QuickActionCardState extends State<_QuickActionCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _pressCtrl;
  late Animation<double> _scaleAnim;

  @override
  void initState() {
    super.initState();
    _pressCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 100),
      lowerBound: 0.95,
      upperBound: 1.0,
      value: 1.0,
    );
    _scaleAnim = _pressCtrl;
  }

  @override
  void dispose() {
    _pressCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bgCard = widget.isDark ? AppTheme.darkBgCard : AppTheme.bgCard;

    return GestureDetector(
      onTapDown: (_) => _pressCtrl.reverse(),
      onTapUp: (_) {
        _pressCtrl.forward();
        widget.onTap();
      },
      onTapCancel: () => _pressCtrl.forward(),
      child: ScaleTransition(
        scale: _scaleAnim,
        child: Container(
          height: 90,
          decoration: BoxDecoration(
            color: bgCard,
            borderRadius: BorderRadius.circular(18),
            boxShadow: widget.isDark
                ? [
                    const BoxShadow(color: Color(0x44000000), blurRadius: 10, offset: Offset(4, 4)),
                    const BoxShadow(color: Color(0x1AFFFFFF), blurRadius: 10, offset: Offset(-4, -4)),
                  ]
                : [
                    const BoxShadow(color: Color(0x18B0C4DE), blurRadius: 10, offset: Offset(4, 4)),
                    const BoxShadow(color: Color(0xCCFFFFFF), blurRadius: 10, offset: Offset(-4, -4)),
                  ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: widget.color.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(widget.icon, color: widget.color, size: 22),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    widget.label,
                    style: TextStyle(
                      color: widget.isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary,
                      fontWeight: FontWeight.w700,
                      fontSize: 14,
                      height: 1.3,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
