# ADMOB

Based off from [love2d-admob-android](https://bitbucket.org/bio1712/love2d-admob-android/src/master/)

Read original post/thread [here](https://love2d.org/forums/viewtopic.php?f=5&t=84226)

## INSTRUCTIONS

Add to `gradle.properties` the following (modify according to your data):
```
app.name=Going Home # your game's title'
app.activity_name=org.flamendless.admob.AdActivity # probably don't change this anymore
app.application_id=org.flamendless.test # this should be unique
app.orientation=landscape # or portrait

admob.admob_app_id=ca-app-pub-xxxx
admob.publisher_id=pub-xxxx
admob.privacy_url=https://www.google.com/about/company/user-consent-policy/
admob.test_device_id=XXXX
admob.collect_consent=true

flamendless.admob=true
```


## Usage with LOVE

For an example, look up [Anagramer](https://github.com/flamendless/anagramer)

In your game, you can require the module using `local admob = require("admob")`

Finally you can override methods and callbacks, note that the `update` is required.
See [love_admob.lua](https://github.com/flamendless/Anagramer/blob/master/modules/love_admob.lua).

You can use this simple module:
```lua
local love_admob = require("admob")

love_admob.timer = 0
love_admob.updateTime = 1 --Seconds
love_admob.debugging = false

local test_done = false

--[[
	debugging: bool = false
	timer: int = 0
	updateTime: int = 1

	changeEUConsent: function
	checkForAdsCallbacks: function
	coreGetRewardQuantity: function
	coreGetRewardType: function
	coreInterstitialClosed: function
	coreInterstitialError: function
	coreRewardedAdDidFinish: function
	coreRewardedAdDidStop: function
	coreRewardedAdError: function
	createBanner: function
	getDeviceLanguage: function
	hideBanner: function
	isInterstitialLoaded: function
	isRewardedAdLoaded: function
	requestInterstitial: function
	requestRewardedAd: function
	showBanner: function
	showInterstitial: function
	showRewardedAd: function
	test: function
	update: function
--]]

function love_admob.update(dt)
	if love_admob.timer > 1 then
		if not test_done and love_admob.debugging then
			print(Inspect(love_admob))
			test_done = love_admob.test()
			print("Test", test_done)
		end
		love_admob.checkForAdsCallbacks()
		love_admob.timer = 0
	end
	love_admob.timer = love_admob.timer + dt
end

function love_admob.checkForAdsCallbacks()
	if love_admob.coreInterstitialError() then --Interstitial failed to load
		if love_admob.interstitialFailedToLoad then
			love_admob.interstitialFailedToLoad()
		end
	end

	if love_admob.coreInterstitialClosed() then --User has closed the ad
		if love_admob.interstitialClosed then
			love_admob.interstitialClosed()
		end
	end

	if love_admob.coreRewardedAdError() then --Rewarded ad failed to load
		if love_admob.rewardedAdFailedToLoad then
			love_admob.rewardedAdFailedToLoad()
		end
	end

	if love_admob.coreRewardedAdDidFinish() then --Video has finished playing
		local rewardType = "???"
		local rewardQuantity = 1
		rewardType = love_admob.coreGetRewardType() or "???"
		rewardQuantity = love_admob.coreGetRewardQuantity() or 1

		if love_admob.rewardUserWithReward then
			love_admob.rewardUserWithReward(rewardType,rewardQuantity)
		end
	end

	if love_admob.coreRewardedAdDidStop() then --Video has stopped by user
		if love_admob.rewardedAdDidStop then
			love_admob.rewardedAdDidStop()
		end
	end
end

return love_admob
```

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
