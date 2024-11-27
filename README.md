# Kampus Konnect Mobile App

## Overview
Kampus Konnect is a comprehensive query management system developed for educational institutions. The Android mobile app complements the web platform, providing students and administrators with a seamless query handling experience.

## Features

### Student Features
- **Query Submission System**
  - Submit academic and administrative queries
  - Attach supporting documents
  - Select specific modules based on qualification
  - Track query status (Pending/Processing/Solved)

- **Real-time Tracking**
  - Live status updates
  - Push notifications for query status changes
  - Filter queries by status and type

- **Communication**
  - Direct chat with administrators
  - Rate administrator service (5-star system)
  - Provide feedback on query resolution

- **Profile Management**
  - Student registration and login
  - Module selection based on qualification
  - Personal profile management

### Admin Features
- **Dashboard Analytics**
  - Query statistics visualization
  - Admin leaderboard system
  - Global query completion rates
  - Category-wise query distribution

- **Query Management**
  - Process incoming queries
  - Update query status
  - Provide feedback to students
  - Chat with students

- **Performance Tracking**
  - XP-based level system
  - Service ratings overview
  - Query resolution statistics

## Technical Stack

### Mobile App
- Language: Kotlin
- IDE: Android Studio
- Architecture: MVVM
- Real-time Database: Firebase

### Integration
- Shared Firebase database with web platform
- Real-time data synchronization
- Cross-platform notification system

## Setup Instructions

1. Clone the repository
```bash
git clone https://github.com/yourusername/kampus-konnect-mobile
```

2. Open project in Android Studio

3. Configure Firebase:
   - Add your `google-services.json`
   - Set up Firebase Authentication
   - Configure Realtime Database rules

4. Build and run the application

## Database Structure
```
kampus-konnect-db/
├── users/
│   ├── students/
│   └── admins/
├── queries/
│   ├── pending/
│   ├── processing/
│   └── solved/
├── chats/
└── modules/
```

## Requirements
- Android Studio Arctic Fox or later
- Kotlin 1.5+
- Firebase Account
- Minimum SDK: API 21
- Target SDK: API 33

## Contributing
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Authors
- Nathan Nayager
-Bianca Munsami
-Bai Hong He
-Uzair Sharif
-Cristina Rodrigues


