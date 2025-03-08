# Volume Manager

**Work in progress**

Control each app's volume independently. [Shizuku](https://shizuku.rikka.app/) is used to access privileged APIs.

![Screenshot](screenshot.png)

## Compare with [SoundMaster from ShizuTools](https://github.com/legendsayantan/ShizuTools/wiki/SoundMaster)

This app uses hidden API to directly change each audio stream's volume.

SoundMaster uses MediaProjection API to record audio from each app and apply post-effects.

|                                | Volume Master  | SoundMaster    |
| ------------------------------ | -------------- | -------------- |
| Control volume of each app     | ✅              | ✅              |
| Set output device for each app | ❌              | ✅              |
| Change left-right balance      | ❌ <sup>1</sup> | ✅              |
| Equalizer (EQ)                 | ❌              | ✅              |
| Control protected apps         | ✅              | ❌ <sup>2</sup> |
| Zero latency added             | ✅              | ❌              |

<sup>1</sup>: There is API to do it, but not implemented in this app
<sup>2</sup>: Unless to manually patch each app

## TODOs

- [ ] Run in background: currently this app needs to be in foreground to apply volume to new apps and/or streams.
- [ ] Integrate with volume overlay (volume button)

## Note

This app uses the same API as MIUI's "Adjust media sound in multiple apps". Because this API can only setting volume, not reading, the volume set by one app will not be reflected in the other one.
