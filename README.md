# [![Banner](./resource/banner.svg)](https://github.com/lucka-me/mapler-android)

[![Release](https://img.shields.io/github/v/release/lucka-me/mapler-android?color=yellow&include_prereleases)](https://github.com/lucka-me/mapler-android/releases/latest "Latest release") [![Author](https://img.shields.io/badge/author-Lucka-2578B5.svg)](https://lucka.moe "Blog") [![MPL 2.0](https://img.shields.io/github/license/lucka-me/mapler-android)](./LICENSE "License")  
[![Platform](https://img.shields.io/badge/android-6.0+-78C257.svg)](https://www.android.com/versions/marshmallow-6-0/ "Android 6.0")

Make map as wallpaper, under development.

[Download](https://github.com/lucka-me/mapler-android/releases "Releases")

## Features
- Generate wallpaper-size image from map with styles created with [Mapbox Studio](https://www.mapbox.com/mapbox-studio/)
- Add new map style from URL
- Customizable live wallpaper:
  - Follow location of the device
  - Camera position
  - Change style randomly

## Build
Before building, please open the `res/values/strings.xml`, uncomment the `mapbox_default_access_token` and replace the string with your own Mapbox Token.  

## Changelog
See [CHANGELOG.md](./CHANGELOG.md).

## License
The source code are [licensed under Mozilla Public License 2.0](./LICENSE).

```
NOTES
=====

The default styles included in the application all credit to their designers.

The Mapbox token included in the release apk is owned by Lucka and ONLY for
usage in the application.
```
