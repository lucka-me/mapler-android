[![Banner](./resource/banner.svg)](https://github.com/lucka-me/mapler-android)

# Changelog

```markdown
## [0.2.0-beta01] - 2019-10-05
- 0.1.9(812) -> 0.2.0-beta01(1062)
- [BETA] Major update

### Added
- Various new styles

### Changed
- App name: Wallmapper -> Mapler
- Support Android 10
- No longer request external storage permission in Android 10 to save image
- UI improved, especially for dark mode
- Data structure updated and code re-constructed

### Fixed
- Live wallpaper: Crash when preview in system wallpaper selector
- Live wallpaper: Follow location doesn't work properly

### Removed
- Support for 32-bit devices
- Customizing Mapbox Token
- Adding style from JSON
```

```markdown
## [0.1.9] - 2019-06-02
- 0.1.8(797) -> 0.1.9(812)
- Fixed Preference crash

### Changed
- Updated dependencies

### Fixed
- App will crash when try to open sub-screen of Preference - caused by
  minifying

### Known Issues
- Color of buttons is unsuitable in Dark Mode
```

```markdown
## [0.1.8] - 2019-05-22
- 0.1.7(642) -> 0.1.8(797)
- UI redesigned

### Changed
- Redesigned UI, simpler and more Material

### Fixed
- The follow location won't turn on after permission granted
```

```markdown
## [0.1.7] - 2019-05-07
- 0.1.6(582) -> 0.1.7(642)
- Data structure upgrated

### Added
- New data structure (v4) with id, fileId and so on, which is more flexible

### Changed
- Use Material Components
- Reduce apk size

### Fixed
- Selection of style will lost when open Preferences
- Some logical mistakes in Live Wallpaper Engine - not tested yet

### Known Issues
- Styles added from JSON can't work correctly in neither preview nor live
  wallpaper - no direct solution yet
```

```markdown
## [0.1.6] - 2019-03-24
- 0.1.5(539) -> 0.1.6(582)
- New feature: Avaliable for Random switch

### Added
- A switch in Style Information dialog to set if the style is available
  for random style
- A preference to reset designated camere from the main map

### Changed
- Minor text changed

### Fixed
- Live wallpaper will be refreshed to the selected style even if the
  Random Style is on
```

```markdown
## [0.1.5] - 2019-03-16
- 0.1.4(523) -> 0.1.5(539)
- UI improved

### Added
- A new style information dialog to display informations only
- An edit style dialog to edit style informations

### Changed
- Update the dependencies to fix some invisible bugs
- Minor text changed
```

```markdown
## [0.1.4] - 2019-03-11
- 0.1.3(492) -> 0.1.4(523)
- New feature: Random style for live wallpaper

### Added
- Change style randomly for live wallpaper
- Following location for live wallpaper can be disabled now

### Changed
- App will request location permission when turn on the Follow Location instead
  of open the Preview & Set page
- Minor text changed

### Fixed
- App may crash in some extreme situations like push back very quickly after
  launch
```

```markdown
## [0.1.3] - 2019-03-03
- 0.1.2(476) -> 0.1.3(492)
- Fixed: Crash when launch

### Fixed
- App crashes when launch, which caused by minifying
```

```markdown
## [0.1.2] - 2019-03-02
- 0.1.1(471) -> 0.1.2(476)
- Localized: Chinese (Simplified)

### Added
- Localization for Chinese (Simplified)
```

```markdown
## [0.1.1] - 2019-02-16
- 0.1.0(419) -> 0.1.1(471)
- New function: Style Preview

### Added
- Style Preview: Display a sample of the style in the information dialog

### Changed
- Reduce the apk size
```

```markdown
## [0.1.0] - 2019-02-08
- 0.1.0(419)
- Initial version
```
