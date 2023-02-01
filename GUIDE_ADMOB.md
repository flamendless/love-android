# ADMOB

Based off from [love2d-admob-android](https://bitbucket.org/bio1712/love2d-admob-android/src/master/)

Read original post/thread [here](https://love2d.org/forums/viewtopic.php?f=5&t=84226)

## INSTRUCTIONS

Add to `gradle.properties` the following (modify according to your data):
```
flamendless.admob=true

admob.app_id=ca-app-pub-xxxx
admob.publisher_id=pub-xxxx
admob.privacy_url=https://www.google.com/about/company/user-consent-policy/
admob.test_device_id=XXXX
admob.collect_consent=true
```

Modify the `applicationId`, `versionName`, and `versionCode` in `app/build.gradle`


## Usage with LOVE

For an example, look up [Anagramer](https://github.com/flamendless/anagramer)

In your game, you can require the module using `local admob = require("admob")`

Finally you can override methods and callbacks, note that the `update` is required.
See [love_admob.lua](https://github.com/flamendless/Anagramer/blob/master/modules/love_admob.lua).

### Option 1
You need to provide your own `love.run` method then add this:
```lua
if love_admob then love_admob.update(dt) end --this
if love.timer then love.timer.sleep(0.001) end --just above this
```

### Option 2
If you do not want to provide your own `love.run`, you can easily just call
`admob.update(dt)` in your `love.update`:
```lua
function love.update(dt)
	admob.update(dt)
	--more of your codes
end
```
