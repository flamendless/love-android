ks=""

apk () {
    sudo ./gradlew assembleEmbedNoRecordRelease && sudo apksigner sign --ks "$ks" app/build/outputs/apk/embedNoRecord/release/app-embed-noRecord-release-unsigned.apk
}

aab () {
    sudo ./gradlew bundleEmbedNoRecordRelease && sudo apksigner sign --min-sdk-version 19 --ks "$ks" app/build/outputs/bundle/embedNoRecordRelease/app-embed-noRecord-release.aab
}
