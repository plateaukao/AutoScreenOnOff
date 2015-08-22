# Auto Screen OnOff

Built on Travis: <img src='https://travis-ci.org/plateaukao/AutoScreenOnOff.svg?branch=master' />

[![Get it on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=com.danielkao.autoscreenonoff)

## Overview
*an android app to turn on/off screen automatically by detecting values from proximity sensor.*

## Features
1. By detecting p-sensor, automatically turn on/off the screen for you.
2. Allows you to only enable the function during charging.
3. Allows you to disable the feature when the screen is rotated.
4. Separate timeout values for screen on/off delay to prevent from accidentally triggering the feature.
5. A widget is supported to quickly toggle the function.
6. Notification is supported to quickly toggle function, or directly turn screen off.

### How it works
Modify Settings in "Auto Screen Settings" app and enable the function

or 

1. Add widget "AutoScreenOnOff" to your home screen
2. Press once on the icon to trigger Device Management Confirmation Dialog.
3. Agree to activate device management. (This is required to turn off the screen)
4. Now everything should work now. Try cover your hand over the top area of the screen (where the proximity sensor might be located) to see if it works.

## Development
This project is built using Android Studio. If you want to clone the git and modify the codes, please use Android Studio too.

## Screenshots
Preference Screen
<img src="https://github.com/plateaukao/AutoScreenOnOff/raw/master/screenshots/autoscreenonoff_preferences.png" alt="preference" style="width: 400px;"/>
