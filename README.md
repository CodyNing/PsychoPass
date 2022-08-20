# Project PsychoPass
New branch, refactored using google map view

## Installing
* Get your Google Map api key from https://console.developers.google.com/

* Using emulator with Google Play Service

* Once pulled the branch, add a XML Resouce file under *res/values/google_maps_api.xml* with below code. **NOTE: Do not commit your google_maps_api.xml file**
```
<resources>
    <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">YOUR_API_KEY</string>
</resources>
```

* You may need to sync, clean, rebuild your project to solve any errors

* Run the project