import 'package:flutter/foundation.dart';

class AuthService extends ChangeNotifier {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  UserModel? _currentUser;
  bool _isLoading = false;
  String? _errorMessage;

  UserModel? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  bool get isAuthenticated => _currentUser != null;
  String? get errorMessage => _errorMessage;

  Future<bool> signIn(String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    //fOr now I have kept it like this. we will connect with real backend...
    await Future.delayed(const Duration(milliseconds: 1500));

    if (email.isNotEmpty && password.length >= 6) {
      _currentUser = UserModel(
        id: 'usr_${DateTime.now().millisecondsSinceEpoch}',
        email: email,
        name: email.split('@').first,
        role: 'Edge Analyst',
        avatarInitials: email.substring(0, 2).toUpperCase(),
        joinedAt: DateTime.now(),
      );
      _isLoading = false;
      notifyListeners();
      return true;
    } else {
      _errorMessage = password.length < 6
          ? 'Password must be at least 6 characters'
          : 'Invalid credentials. Please try again.';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> signUp(String name, String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    await Future.delayed(const Duration(milliseconds: 1800));

    if (name.isNotEmpty && email.contains('@') && password.length >= 6) {
      _currentUser = UserModel(
        id: 'usr_${DateTime.now().millisecondsSinceEpoch}',
        email: email,
        name: name,
        role: 'Edge Analyst',
        avatarInitials: name
            .substring(0, name.contains(' ') ? 2 : 1)
            .toUpperCase(),
        joinedAt: DateTime.now(),
      );
      _isLoading = false;
      notifyListeners();
      return true;
    } else {
      _errorMessage = 'Please fill all fields correctly.';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  void signOut() {
    _currentUser = null;
    _errorMessage = null;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}

class UserModel {
  final String id;
  final String email;
  final String name;
  final String role;
  final String avatarInitials;
  final DateTime joinedAt;

  const UserModel({
    required this.id,
    required this.email,
    required this.name,
    required this.role,
    required this.avatarInitials,
    required this.joinedAt,
  });
}
