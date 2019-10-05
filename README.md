<h1 align=center><img src="./resource/banner.svg"></img></h1>

<p align=center>
  <a href="./CHANGELOG.md"><img alt="Version" src="https://img.shields.io/github/release/lucka-me/mapler-android.svg?color=yellow&label=version"></a>
  <a href="https://lucka.moe"><img alt="Author" src="https://img.shields.io/badge/author-Lucka-2578B5.svg"/></a>
  <a href="./LICENSE"><img alt="License" src="https://img.shields.io/badge/license-MPL_2.0-000000.svg"/></a><br>
  <a href="https://www.android.com/versions/marshmallow-6-0/"><img alt="Minmum SDK 23" src="https://img.shields.io/badge/min_SDK-23-78C257.svg"/></a>
  <a href="https://www.android.com/versions/marshmallow-6-0/"><img alt="Android 6.0+" src="https://img.shields.io/badge/Android-6.0+-78C257.svg"/></a>

</p>

<p align=center>
Make map as wallpaper<br/>
Under development<br/>
<a href="https://github.com/lucka-me/mapler-android/releases">Download</a>
</p>

## Functions
- Generate wallpaper-size images from map with different styles
- Add new map style from URL or JSON file
- Customizable live wallpaper of map:
  - Follow location of the device
  - Camera position
  - Change style randomly

## Build
Before building, please open the `res/values/strings.xml`, uncomment the `mapbox_default_access_token` and replace the string with your own Mapbox Token.  
And in `res/values/arrays_default_style.xml`, all styles whose URL begin with `mapbox://styles/lucka-me/` should be removed (they are bound with my token), you can add URLs of your own styles in your Mapbox Studio.

## Changelog
See [CHANGELOG.md](./CHANGELOG.md).

## License
The source code are [licensed under Mozilla Public License 2.0](./LICENSE).

```
NOTES
=====

The default styles included in the application all credit to their designers.

The Mapbox token included in the release apk is owned by Lucka and ONLY for
useage in the application.
```
