import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../theme/theme_notifier.dart';
import 'auth_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _entryController;
  late Animation<double> _entryFade;
  late Animation<Offset> _entrySlide;

  final _authService = AuthService();
  bool _notificationsEnabled = true;
  bool _autoSaveResults = false;

  @override
  void initState() {
    super.initState();
    _entryController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    )..forward();
    _entryFade = CurvedAnimation(
      parent: _entryController,
      curve: Curves.easeOut,
    );
    _entrySlide =
        Tween<Offset>(begin: const Offset(0, 0.06), end: Offset.zero).animate(
          CurvedAnimation(
              parent: _entryController, curve: Curves.easeOutCubic),
        );
  }

  @override
  void dispose() {
    _entryController.dispose();
    super.dispose();
  }

  void _signOut(BuildContext context, bool isDark) {
    HapticFeedback.mediumImpact();
    final cardBg      = isDark ? AppTheme.darkBgCard : AppTheme.bgCard;
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final textSec     = isDark ? AppTheme.darkTextSecondary : AppTheme.textSecondary;

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: cardBg,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(
          'Sign Out',
          style: TextStyle(color: textPrimary, fontWeight: FontWeight.w800),
        ),
        content: Text(
          'Are you sure you want to sign out?',
          style: TextStyle(color: textSec),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Cancel', style: TextStyle(color: textSec)),
          ),
          TextButton(
            onPressed: () {
              _authService.signOut();
              Navigator.of(context).pushAndRemoveUntil(
                PageRouteBuilder(
                  pageBuilder: (_, __, ___) => const AuthScreen(),
                  transitionDuration: const Duration(milliseconds: 400),
                  transitionsBuilder: (_, animation, __, child) =>
                      FadeTransition(opacity: animation, child: child),
                ),
                (route) => false,
              );
            },
            child: const Text(
              'Sign Out',
              style: TextStyle(
                color: Color(0xFFDC2626),
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark      = context.watch<ThemeNotifier>().isDark;
    final notifier    = context.read<ThemeNotifier>();
    final bg          = isDark ? AppTheme.darkBgDeep    : AppTheme.bgDeep;
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;

    final user = _authService.currentUser;
    final displayName =
        user?.userMetadata?['display_name'] as String? ??
        user?.email?.split('@').first ??
        'EdgeSite User';
    final initials = displayName.isNotEmpty
        ? displayName
              .substring(0, displayName.contains(' ') ? 2 : 1)
              .toUpperCase()
        : 'US';

    return Scaffold(
      backgroundColor: bg,
      body: FadeTransition(
        opacity: _entryFade,
        child: SlideTransition(
          position: _entrySlide,
          child: CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              SliverToBoxAdapter(
                child: SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                    child: Text(
                      'Profile',
                      style: TextStyle(
                        color: textPrimary,
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        letterSpacing: -0.5,
                      ),
                    ),
                  ),
                ),
              ),

              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
                  child: _UserCard(
                    displayName: displayName,
                    email: user?.email ?? '',
                    initials: initials,
                    isDark: isDark,
                  ),
                ),
              ),

              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 24, 20, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _SectionHeader(label: 'Appearance', isDark: isDark),
                      const SizedBox(height: 10),
                      _SettingsCard(
                        isDark: isDark,
                        children: [
                          _ToggleTile(
                            icon: Icons.dark_mode_rounded,
                            label: 'Dark Mode',
                            value: isDark,
                            isDark: isDark,
                            onChanged: (_) => notifier.toggle(),
                          ),
                        ],
                      ),

                      const SizedBox(height: 20),
                      _SectionHeader(label: 'Preferences', isDark: isDark),
                      const SizedBox(height: 10),
                      _SettingsCard(
                        isDark: isDark,
                        children: [
                          _ToggleTile(
                            icon: Icons.notifications_outlined,
                            label: 'Push Notifications',
                            value: _notificationsEnabled,
                            isDark: isDark,
                            onChanged: (v) =>
                                setState(() => _notificationsEnabled = v),
                          ),
                          _Divider(isDark: isDark),
                          _ToggleTile(
                            icon: Icons.save_alt_rounded,
                            label: 'Auto-save Results',
                            value: _autoSaveResults,
                            isDark: isDark,
                            onChanged: (v) =>
                                setState(() => _autoSaveResults = v),
                          ),
                        ],
                      ),

                      const SizedBox(height: 20),
                      _SectionHeader(label: 'Model Info', isDark: isDark),
                      const SizedBox(height: 10),
                      _SettingsCard(
                        isDark: isDark,
                        children: [
                          _ActionTile(
                            icon: Icons.cloud_download_outlined,
                            label: 'Update Model',
                            subtitle: 'YOLOv8n · v1.0.2 · 6.3 MB',
                            color: isDark
                                ? AppTheme.darkPrimary
                                : AppTheme.primary,
                            isDark: isDark,
                            onTap: () {},
                          ),
                          _Divider(isDark: isDark),
                          _ActionTile(
                            icon: Icons.tune_rounded,
                            label: 'Detection Threshold',
                            subtitle: 'Confidence: 0.50 · IoU: 0.45',
                            isDark: isDark,
                            onTap: () {},
                          ),
                        ],
                      ),

                      const SizedBox(height: 20),
                      _SectionHeader(label: 'Account', isDark: isDark),
                      const SizedBox(height: 10),
                      _SettingsCard(
                        isDark: isDark,
                        children: [
                          _ActionTile(
                            icon: Icons.logout_rounded,
                            label: 'Sign Out',
                            color: const Color(0xFFDC2626),
                            isDark: isDark,
                            onTap: () => _signOut(context, isDark),
                          ),
                        ],
                      ),

                      const SizedBox(height: 100),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _UserCard extends StatelessWidget {
  final String displayName;
  final String email;
  final String initials;
  final bool isDark;

  const _UserCard({
    required this.displayName,
    required this.email,
    required this.initials,
    required this.isDark,
  });

  @override
  Widget build(BuildContext context) {
    final cardDeco = isDark ? AppTheme.neuCardDark() : AppTheme.neuCard();
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final textMuted   = isDark ? AppTheme.darkTextSecondary : AppTheme.textMuted;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: cardDeco,
      child: Row(
        children: [
          Container(
            width: 66,
            height: 66,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              gradient: AppTheme.primaryGradient,
              boxShadow: [
                BoxShadow(
                  color: AppTheme.primary.withOpacity(0.3),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Center(
              child: Text(
                initials,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w900,
                  fontSize: 22,
                ),
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  displayName,
                  style: TextStyle(
                    color: textPrimary,
                    fontWeight: FontWeight.w800,
                    fontSize: 18,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  email,
                  style: TextStyle(color: textMuted, fontSize: 13),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 10, vertical: 3),
                  decoration: BoxDecoration(
                    color: AppTheme.accent.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                        color: AppTheme.accent.withOpacity(0.2)),
                  ),
                  child: Text(
                    'Active',
                    style: TextStyle(
                      color: isDark ? AppTheme.darkAccent : AppTheme.accent,
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String label;
  final bool isDark;
  const _SectionHeader({required this.label, required this.isDark});

  @override
  Widget build(BuildContext context) {
    return Text(
      label.toUpperCase(),
      style: TextStyle(
        color: isDark ? AppTheme.darkTextSecondary : AppTheme.textSecondary,
        fontSize: 12,
        fontWeight: FontWeight.w700,
        letterSpacing: 0.8,
      ),
    );
  }
}

class _SettingsCard extends StatelessWidget {
  final List<Widget> children;
  final bool isDark;
  const _SettingsCard({required this.children, required this.isDark});

  @override
  Widget build(BuildContext context) {
    final deco = isDark ? AppTheme.neuCardDark() : AppTheme.neuCard();
    return Container(
      decoration: deco,
      child: Column(children: children),
    );
  }
}

class _Divider extends StatelessWidget {
  final bool isDark;
  const _Divider({required this.isDark});

  @override
  Widget build(BuildContext context) {
    return Divider(
      height: 1,
      color: isDark ? AppTheme.darkBgElevated : AppTheme.bgElevated,
      indent: 54,
    );
  }
}

class _ToggleTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool value;
  final bool isDark;
  final ValueChanged<bool> onChanged;

  const _ToggleTile({
    required this.icon,
    required this.label,
    required this.value,
    required this.isDark,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final surface     = isDark ? AppTheme.darkBgElevated : AppTheme.bgSurface;
    final iconColor   = isDark ? AppTheme.darkTextSecondary : AppTheme.textSecondary;
    final primary     = isDark ? AppTheme.darkPrimary : AppTheme.primary;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: surface,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: iconColor, size: 18),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              label,
              style: TextStyle(
                color: textPrimary,
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Switch.adaptive(
            value: value,
            onChanged: onChanged,
            activeColor: primary,
          ),
        ],
      ),
    );
  }
}
class _ActionTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final String? subtitle;
  final Color? color;
  final bool isDark;
  final VoidCallback onTap;

  const _ActionTile({
    required this.icon,
    required this.label,
    required this.isDark,
    required this.onTap,
    this.subtitle,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    final textPrimary = isDark ? AppTheme.darkTextPrimary : AppTheme.textPrimary;
    final textMuted   = isDark ? AppTheme.darkTextSecondary : AppTheme.textMuted;
    final iconColor   = color ?? (isDark ? AppTheme.darkTextSecondary : AppTheme.textSecondary);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: iconColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, color: iconColor, size: 18),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: TextStyle(
                        color: color ?? textPrimary,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 2),
                      Text(
                        subtitle!,
                        style: TextStyle(color: textMuted, fontSize: 12),
                      ),
                    ],
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: textMuted,
                size: 18,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
