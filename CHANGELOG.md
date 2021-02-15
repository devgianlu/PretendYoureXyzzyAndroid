# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.0.9] - 15-02-2021
### Fixed
- Fixed crash when importing deck
- Fixed crash when creating card
- Fixed minor crashes


## [5.0.8] - 27-01-2021
### Fixed
- Fixed crash in welcome screen
- Fixed crash with custom decks syncing


## [5.0.7] - 18-01-2021
### Added
- Added custom decks instructions

### Fixed
- Fixed crash with Polish language
- Minor bug fixes


## [5.0.6] - 11-01-2021
### Fixed
- Fixed crash at startup

### Added 
- Allow importing deck with duplicate name after changing it


## [5.0.5] - 10-01-2021
### Fixed
- Fixed Overloaded subscription failing
- Minor bug fixes


## [5.0.4] - 09-01-2021
### Fixed
- Fixed another DB crash
- Fixed game layout overlap on small devices
- Fixed crash in metrics
- Minor bug fixes


## [5.0.3] - 03-01-2021
### Fixed
- Fixed crash when creating game
- Minor crashes fixed


## [5.0.2] - 31-12-2020
### Fixed
- Fixed crash when recovering Cardcast deck
- Fixed crash when upgrading from old version

### Added
- Added players list shortcut


## [5.0.1] - 26-12-2020
### Fixed
- Fixed crash at startup
- Fixed weird issue with main screen
- Fixed friend and player status layout
- Minor bug fixes


## [5.0.0] - 22-12-2020
### Added
- See who collaborated to your decks

### Changed
- Improved connection reliability
- Updated icon

### Fixed
- Fixed some layouts
- Many bug fixes too


## [5.0.0-beta01] - 13-12-2020
### Added
- Added dark theme
- New logo
- Games list scroll behavior
- Import exported decks directly

### Fixed
- Fixed UI
- Fixed custom card text not readable
- Do not fail register because of Overloaded
- Minor bug fixes


## [5.0.0-alpha03] - 03-12-2020
### Added
- Show subscription and maintenance warnings
- View deck from game info
- Show metrics from game
- Added chat badges
- Transitions

### Fixed
- Fixed layout on small devices
- Fixed card text escaping
- Fixed starred cards sync
- A lot more


## [5.0.0-alpha02] - 28-11-2020
### Added
- Upload profile image
- See others' profile image

### Changed
- Lock username when signed in with Overloaded

### Fixed
- Fixed game layout
- Fixed crashes
- Other minor fixes


## [5.0.0-alpha01] - 23-11-2020
### Changed
- Complete redesign
- A lot more...


## [4.2.3] - 08-10-2020
### Fixed
- Fixed CrCast decks not loading


## [4.2.2] - 16-09-2020
### Fixed
- Fixed CrCast logging out
- Fixed subscription renewing not working properly


## [4.2.1] - 26-08-2020
### Fixed
- Fixed minor NPE


## [4.2.0] - 21-08-2020
### Added
- Added collaborative decks (Overloaded)
- Added cards count for custom decks

### Fixed
- Fixed some CrCast issues
- Fixed Overloaded sync issues
- Other minor fixes


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