# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.1.3] - 18-08-2020
### Fixed
- Fixed app crashing on startup (again)


## [4.1.2] - 18-08-2020
### Fixed
- Fixed app crashing on startup


## [4.1.1] - 18-08-2020
### Added
- Added ability to create image cards

### Fixed
- Fixed metrics not viewing correctly


## [4.1.0] - 16-08-2020
### Added
- Added support for CrCast (view and use decks)

### Fixed
- Fixed crash when using some cards
- Fixed exporting cards


## [4.0.2] - 11-08-2020
### Added
- Export/share custom decks

### Changed
- Refresh game after 3 seconds it has been stuck

### Fixed
- Fixed importing cards with 0 pick
- Fixed unescaped HTML in cards


## [4.0.1] - 01-08-2020
### Fixed
- Fixed crash when joining game


## [4.0.0] - 28-07-2020
### Added
- Highlight Overloaded users in player list

### Changed
- Load default server on network error
- Better edit game options dialog layout


## [4.0.0-beta05] - 26-07-2020
### Added
- Added explanation dialog when importing deck
- Added deck description note

### Fixed
- Fixed imported decks not syncing properly (Overloaded)
- Improved sync when requests fail (Overloaded)
- Correctly close PYX connection
- Fixed "nickname already in use" error when logging in


## [4.0.0-beta04] - 22-07-2020
### Fixed
- Fixed chat not working (Overloaded)


## [4.0.0-beta03] - 21-07-2020
### Added
- Added confirmation for game logout
- Added confirmation for account deletion

### Changed
- Change last sync time format

### Fixed
- Fixed chat not working (Overloaded)


## [4.0.0-beta02] - 13-07-2020
### Added
- Sort custom decks by most recently used
- Better performance monitoring
- Show custom decks in game list

### Changed
- Changed server authentication method (much lighter requests)

### Fixed
- Fixed game locking up when spectator is alone inside a game
- Fixed polling thread not stopping when closing instance


## [4.0.0-beta01] - 06-07-2020
### Added
- Added end-to-end encrypted chat (Overloaded only)
- Synchronize starred cards and custom decks (Overloaded only)
- See other users profiles and stats (Overloaded only)
- Make friends on servers (Overloaded only)

### Changed
- Improved chats layout
- More improvements and bug fixes


## [3.2.1] - 06-07-2020
### Added
- Added ability to recover Cardcast decks


## [3.2.0] - 04-07-2020
### Added
- Added custom decks functionality (on supporting servers)
- Added legend in server dialogs


## [3.1.4] - 19-05-2020
### Removed
- Cardcast has been shutdown, a replacement will be available soon


## [3.1.3] - 28-04-2020
### Changed
- Using Android logging
- Allow app to be moved to the external storage
- Updated third-part libraries


## [3.1.2] - 05-02-2020
### Fixed
- Fixed rounds played event
- Fixed crash reporting and analytics (Google Play only)


## [3.1.1] - 30-01-2020
### Fixed
- Fixed crash when opening app for the first time


## [3.1.0] - 29-01-2020
### Added
- Google Play Games integration

### Fixed
- Fixed payments being refunded automatically


## [3.0.3] - 26-12-2019
### Changed
- Fixed crash with invalid SSL certificates
- Fixed crash due to invalid dialog


## [3.0.2] - 15-12-2019
### Changed
- Migrated to Firebase (Google Play users only)
- Updated OkHttp and build tools


## [3.0.1] - 28-11-2019
### Changed
- Fixed minor crashes


## [3.0.0] - 25-11-2019
### Added
- Retrieve min/max players, spectators, score and blank cards

### Changed
- Updated Material design
- Fixed wrong behaviour of insecure ID allowed
- Updated onboarding tutorial


## [2.8.15] - 19-07-2019
### Changed
- Fixed crash on startup