# PACKAGING

## Signing

### For Linux

1. Generate your own keystore:
```
keytool -genkey -v -keystore my.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias my_alias
```

2. Sign with `apksigner`:
```
apksigner sign --ks my.keystore app/build/outputs/apk/embedNoRecord/release/app-embed-noRecord-release-unsigned.apk
```
