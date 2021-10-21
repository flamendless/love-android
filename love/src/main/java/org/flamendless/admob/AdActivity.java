package org.flamendless.admob;

import org.love2d.android.BuildConfig;
import org.love2d.android.GameActivity;

import com.google.android.gms.ads.reward.*;
import com.google.android.gms.ads.*; // import library
import com.google.android.gms.ads.MobileAds;
import com.google.ads.mediation.admob.AdMobAdapter;

import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.Method;
import java.util.Arrays;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.Vibrator;
import android.util.Log;
import android.util.DisplayMetrics;
import android.widget.Toast;
import android.view.*;
import android.content.pm.PackageManager;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.graphics.Point;

import java.lang.StringBuilder;
import android.os.StrictMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.io.Reader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread;

public class AdActivity extends GameActivity {

	private String appID = BuildConfig.APPLICATION_ID;
	private boolean fullscreen = false; //CONFIG for banner
	private boolean collectConsent = BuildConfig.COLLECT_CONSENT; //CONFIG for GPDR consent
	private String publisherID = BuildConfig.PUBLISHER_ID; //For consent (Like "pub-3940256099942544")
	private String privacyURL = BuildConfig.PRIVACY_URL; // For consent
	private List<String> testDeviceIds = Arrays.asList(BuildConfig.TEST_DEVICE_ID); //no dash and all uppercase format

	//REMEMBER TO SET UP YOUR TEST DEVICE ID!
	//
	//TO UNHIDE THE STATUS BAR, OPEN SDLACTIVITY.JAVA AND UNCOMMENT THE LINES 423 AND 425 (setWindowStyle(false); AND getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);)

	private AdRequest adRequest;
	private Bundle adExtras = new Bundle();

	//Banner stuff
	private AdView mAdView;
	private RelativeLayout adContainer;
	private boolean hasBanner = false;
	private boolean bannerVisibile = false;
	private boolean bannerHasFinishedLoading = false;
	private boolean bannerCreated = false;
	private String bannerPosition;
	private String bannerAdID = ""; //no need to set

	//Interstitial stuff
	private InterstitialAd mInterstitialAd;
	private boolean hasInterstitial = false;
	private boolean interstitialLoaded = false;

	//Rewarded video stuff
	private RewardedVideoAd mRewardedAd;
	private boolean hasRewardedVideo = false;
	private boolean rewardedAdLoaded = false;

	//For callbacks
	private boolean interstitialDidClose = false;
	private boolean interstitialDidFailToLoad = false;

	private boolean rewardedAdDidFinish = false;
	private boolean rewardedAdDidStop = false;
	private boolean rewardedAdDidFailToLoad = false;
	private double rewardQty;
	private String rewardType;

	//Consents
	private ConsentInformation consentInformation;
	private ConsentForm consentForm;

	private URL getAppsPrivacyPolicy() {
        URL mUrl = null;
        try
		{
            mUrl = new URL(privacyURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return mUrl;
    }

	public void createRewardedVideo()
	{
		mRewardedAd = MobileAds.getRewardedVideoAdInstance(this);
		mRewardedAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
			@Override
			public void onRewarded(RewardItem reward) {
				Log.d("AdActivity","onRewarded");
				rewardedAdDidFinish = true;
				rewardQty = reward.getAmount();
				rewardType = reward.getType();
			}

			@Override
			public void onRewardedVideoAdClosed()
			{
				Log.d("AdActivity","onRewardedVideoAdClosed");
				rewardedAdDidStop = true;
			}

			@Override
			public void onRewardedVideoAdFailedToLoad(int errorCode)
			{
				Log.d("AdActivity","onRewardedVideoAdFailedToLoad: Error " + errorCode);
				rewardedAdDidFailToLoad = true;
			}

			@Override
			public void onRewardedVideoAdLeftApplication()
			{
				Log.d("AdActivity","adClicked: Rewarded Video");

			}

			@Override
			public void onRewardedVideoStarted()
			{
			}

			@Override
			public void onRewardedVideoAdLoaded()
			{
				rewardedAdLoaded = true;
				Log.d("AdActivity","rewardedVideoAdLoaded");
			}

			@Override
			public void onRewardedVideoAdOpened()
			{
				rewardedAdLoaded = false;
			}

			@Override
			public void onRewardedVideoCompleted()
			{
			}
		});
	}

	private void displayConsentForm() {
		Log.i("AdActivity","displayConsentForm()");
		loadForm();
	}

	public void loadForm() {
		UserMessagingPlatform.loadConsentForm(
			this,
			new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
				@Override
				public void onConsentFormLoadSuccess(ConsentForm cf) {
					consentForm = cf;
					Log.i("Consent","form load success: " + consentInformation.getConsentStatus());
					if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
						consentForm.show(
							AdActivity.this,
							new ConsentForm.OnConsentFormDismissedListener() {
								@Override
								public void onConsentFormDismissed(FormError formError) {
									adExtras.putString("npa", "1");
									// loadForm();
								}
							}
						);
					}
				}
			},

			new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
				@Override
				public void onConsentFormLoadFailure(FormError formError) {
					// Handle the error
					Log.i("Consent","form load failure: " + formError.getMessage());
				}
			}
		);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i("Consent", "collecting consent: " + String.valueOf(collectConsent));
		if (collectConsent) {
			//for location
			ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
				.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
				.addTestDeviceHashedId(BuildConfig.TEST_DEVICE_ID)
				.build();

			// Set tag for underage of consent. false means users are not underage.
			ConsentRequestParameters params = new ConsentRequestParameters
				.Builder()
				.setConsentDebugSettings(debugSettings)
				.setTagForUnderAgeOfConsent(false)
				.build();

			consentInformation = UserMessagingPlatform.getConsentInformation(this);
			consentInformation.reset();

			consentInformation.requestConsentInfoUpdate(
				this, params,
				new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
					@Override
					public void onConsentInfoUpdateSuccess() {
						Log.i("Consent", "info update success: " + String.valueOf(consentInformation.isConsentFormAvailable()));
						if (consentInformation.isConsentFormAvailable()) {
							loadForm();
						}
					}
				},
				new ConsentInformation.OnConsentInfoUpdateFailureListener() {
					@Override
					public void onConsentInfoUpdateFailure(FormError formError) {
						Log.i("Consent", "info update failure: "  + formError.getMessage());
						Log.i("Consent", "error code: "  + formError.getErrorCode());
					}
				});
		}

		if (appID.equals("INSERT-YOUR-APP-ID-HERE"))
		{
			Log.d("AdActivity","Initializing SDK without appID");
			MobileAds.initialize(this);
		}
		else
		{
			Log.d("AdActivity","Initializing SDK with appID");
			MobileAds.initialize(this, appID);
		}

		//RequestConfiguration configuration =
		//		new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
		//MobileAds.setRequestConfiguration(configuration);

		createRewardedVideo();
	}


	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
    protected void onDestroy() {
      super.onDestroy();
	  if (hasBanner) {
			mAdView.destroy();
			adContainer.setVisibility(View.GONE);
			bannerCreated = false;
			Log.d("AdActivity","OnDestroy");
		}
    }

    @Override
    protected void onPause() {
      super.onPause();
	  if (hasBanner) {
			mAdView.destroy();
			adContainer.setVisibility(View.GONE);
			bannerCreated = false;
			Log.d("AdActivity","OnPause");
		}
    }

    @Override
    public void onResume() {
      super.onResume();
	  if (hasBanner) {
			createBanner(bannerAdID,bannerPosition);
			Log.d("AdActivity","OnResume");
		}
    }

	public void createBanner(final String adID,final String position)
	{
		bannerPosition = position;
		bannerAdID = adID;
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if (!bannerCreated){
					Log.d("AdActivity","CreateBanner: \"" + position + "\"");
					mAdView = new AdView(mSingleton);
					mAdView.setAdUnitId(adID);

					//CODE FOR ADAPTIVE BANNER:
					/*
					AdSize adSize = getAdSize();

					mAdView.setAdSize(adSize);
					*/
					mAdView.setAdSize(AdSize.SMART_BANNER);

					AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
					for (String s : testDeviceIds)
					{
						adRequestBuilder.addTestDevice(s);
					}
					adRequest = adRequestBuilder
						.addNetworkExtrasBundle(AdMobAdapter.class, adExtras)
						.build();
					mAdView.loadAd(adRequest);

					adContainer = new RelativeLayout(mSingleton);

					// Place the ad view.

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					params.addRule(RelativeLayout.CENTER_HORIZONTAL);
					if (position.equals("bottom"))
					{
						Log.d("AdActivity","Bottom");
						params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					}
					else
					{
						params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					}

					adContainer.addView(mAdView, params);

					RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					params2.addRule(RelativeLayout.CENTER_HORIZONTAL);

					if (position.equals("bottom"))
					{
						Log.d("AdActivity","Bottom");
						params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						adContainer.setGravity(Gravity.BOTTOM);
					}
					else
					{
						params2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						adContainer.setGravity(Gravity.TOP);
					}


					mLayout.addView(adContainer,params2);

					//if showBanner() has been called display the banner, else prevent it from appearing.
					mAdView.setAdListener(new AdListener(){
						@Override
						public void onAdLoaded() {
							if (bannerVisibile)
							{
								mAdView.setVisibility(View.GONE);
								mAdView.setVisibility(View.VISIBLE);
							}
							else
							{
								mAdView.setVisibility(View.GONE);
							}
							Log.d("AdActivity","Banner - onAdLoaded: " + bannerVisibile);
							bannerHasFinishedLoading = true;
						}

						@Override
						public void onAdLeftApplication() {
							Log.d("AdActivity","adClicked: AdMob Banner");

						}
					});
					hasBanner = true;
					bannerCreated = true;
					Log.d("AdActivity", "Banner Created.");
				}
			}
		});
	}

	private AdSize getAdSize()
	{
		// Step 2 - Determine the screen width (less decorations) to use for the ad width.
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		float widthPixels = outMetrics.widthPixels;
		float density = outMetrics.density;

		int adWidth = (int) (widthPixels / density);

		// Step 3 - Get adaptive ad size and return for setting on the ad view.
		return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
	}

	public void hideBanner()
	{
		Log.d("AdActivity", "hideBanner");

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{

				if (hasBanner && bannerHasFinishedLoading)
				{
					mAdView.setVisibility(View.GONE);
					Log.d("AdActivity", "Banner Hidden");
				}
				bannerVisibile = false;
			}
		});
	}

	public void showBanner()
	{
		Log.d("AdActivity", "showBanner");

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{

				if (hasBanner && bannerHasFinishedLoading)
				{
					mAdView.loadAd(adRequest);
					mAdView.setVisibility(View.VISIBLE);
					Log.d("AdActivity", "Banner Showing");
				}
				bannerVisibile = true;
			}
		});
	}

	public void requestInterstitial(final String adID)
	{
		Log.d("AdActivity", "requestInterstitial");

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				mInterstitialAd = new InterstitialAd(mSingleton);
				mInterstitialAd.setAdUnitId(adID);

				AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
				for (String s : testDeviceIds)
				{
					adRequestBuilder.addTestDevice(s);
				}

				AdRequest adRequest = adRequestBuilder
					.addNetworkExtrasBundle(AdMobAdapter.class, adExtras)
					.build();
				mInterstitialAd.loadAd(adRequest);

				mInterstitialAd.setAdListener(new AdListener()
				{
					@Override
					public void onAdClosed()
					{
						interstitialDidClose = true;
						Log.d("AdActivity", "onInterstitialClosed");
					}

					@Override
					public void onAdLoaded()
					{
						Log.d("AdActivity", "interstitialDidReceive");
						interstitialLoaded = true;
					}

					@Override
					public void onAdFailedToLoad(int errorCode)
					{
						Log.d("AdActivity", "onInterstitialFailedToLoad: Error " + errorCode);
						interstitialDidFailToLoad = true;
					}

					@Override
					public void onAdOpened()
					{
						interstitialLoaded = false;
					}

					@Override
					public void onAdLeftApplication()
					{
						Log.d("AdActivity","adClicked: Interstitial");

					}
				});

				hasInterstitial = true;
			}
		});
	}

	//Called in isInterstitialLoaded
	private void updateInterstitialState()
	{
		runOnUiThread(new Runnable(){
			@Override
			public void run()
			{

				if (hasInterstitial)
				{
					if (mInterstitialAd.isLoaded())
					{
						interstitialLoaded = true;
						Log.d("AdActivity", "Interstitial is loaded: " + interstitialLoaded);
					}
					else
					{
						interstitialLoaded = false;
						Log.d("AdActivity", "Interstitial has not loaded yet. " + interstitialLoaded);
					}
				}
			}
		});
	}

	public boolean isInterstitialLoaded()
	{
		Log.d("AdActivity", "isInterstitialLoaded");

		//WORKAROUND: runOnUiThread finishes after the return of this function, then interstitialLoaded could be wrong!
		if (interstitialLoaded)
		{
			updateInterstitialState();
			return true;
		}
		updateInterstitialState();
		return false;
	}


	public void showInterstitial()
	{
		Log.d("AdActivity", "showInterstitial");

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{

				if (hasInterstitial)
				{
					if (mInterstitialAd.isLoaded())
					{
						mInterstitialAd.show();
						Log.d("AdActivity", "Ad loaded!, showing...");
					}
					else
					{
						Log.d("AdActivity", "Ad is NOT loaded!, skipping.");
					}
				}
			}
		});
	}

	public void requestRewardedAd(final String adID)
	{
		Log.d("AdActivity", "requestRewardedAd");
		if (!hasRewardedVideo)
		{
			hasRewardedVideo = true;
		}

		runOnUiThread(new Runnable(){
			@Override
			public void run()
			{
				AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
				for (String s : testDeviceIds)
				{
					adRequestBuilder.addTestDevice(s);
				}

				mRewardedAd.loadAd(adID, adRequestBuilder
					.addNetworkExtrasBundle(AdMobAdapter.class, adExtras)
					.build());
			}
		});


	}

	//Called in rewardedAdLoaded
	private void updateRewardedAdState()
	{
		runOnUiThread(new Runnable(){
			@Override
			public void run()
			{

				if (hasRewardedVideo)
				{
					if (mRewardedAd.isLoaded())
					{
						Log.d("AdActivity", "Rewarded ad is loaded");
						rewardedAdLoaded = true;
					}
					else
					{
						Log.d("AdActivity", "Rewarded ad has not loaded yet.");
						rewardedAdLoaded = false;
					}
				}
			}
		});
	}

	public boolean isRewardedAdLoaded()
	{
		Log.d("AdActivity", "isRewardedAdLoaded");
		if (rewardedAdLoaded)
		{
			updateRewardedAdState();
			return true;
		}
		updateRewardedAdState();
		return false;
	}


	public void showRewardedAd()
	{
		Log.d("AdActivity", "showRewardedAd");

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{

				if (hasRewardedVideo)
				{
					if (mRewardedAd.isLoaded())
					{
						mRewardedAd.show();
						Log.d("AdActivity", "RewardedAd loaded!, showing...");
					}
					else
					{
						Log.d("AdActivity", "RewardedAd is NOT loaded!, skipping.");
					}
				}
			}
		});
	}

	public void changeEUConsent()
	{
		Log.d("AdActivity", "changeEUConsent()");
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				displayConsentForm();
			}
		});
	}

	//For callbacks
	public boolean coreInterstitialClosed()
	{
		if (interstitialDidClose)
		{
			interstitialDidClose = false;
			return true;
		}
		return false;
	}

	public boolean coreInterstitialError()
	{
		if (interstitialDidFailToLoad)
		{
			interstitialDidFailToLoad = false;
			return true;
		}
		return false;
	}

	public boolean coreRewardedAdDidStop()
	{
		if (rewardedAdDidStop)
		{
			rewardedAdDidStop = false;
			return true;
		}
		return false;
	}

	public boolean coreRewardedAdError()
	{
		if (rewardedAdDidFailToLoad)
		{
			rewardedAdDidFailToLoad = false;
			return true;
		}
		return false;
	}

	public boolean coreRewardedAdDidFinish()
	{
		if (rewardedAdDidFinish)
		{
			rewardedAdDidFinish = false;
			return true;
		}
		return false;
	}

	public String coreGetRewardType()
	{
		return rewardType;
	}

	public double coreGetRewardQuantity()
	{
		return rewardQty;
	}

	public String getDeviceLanguage()
	{
		String locale;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			locale = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
		} else {
			locale = context.getResources().getConfiguration().locale.getLanguage();
		}
		return locale.toUpperCase();
	}

	public static void httpPostRequest(String urlStr, String user, String pass,String txt) {
		try {
			URL url = new URL(urlStr);
			Map<String,Object> params = new LinkedHashMap<>();
			params.put("user", user);
			params.put("pass", pass);
			params.put("txt", txt);

			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String,Object> param : params.entrySet()) {
				if (postData.length() != 0) postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");

			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			conn.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
			conn.setRequestProperty("Accept","*/*");

			conn.setDoOutput(true);
			conn.getOutputStream().write(postDataBytes);

			Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			for ( int c = in.read(); c != -1; c = in.read() ) {
				Log.d("GameActivity",String.valueOf((char)c));
			}
		} catch (Exception e) {
			Log.e("GameActivity",getStackTrace(e));
		}
	}

	public static String getStackTrace(final Throwable throwable) {
		 final StringWriter sw = new StringWriter();
		 final PrintWriter pw = new PrintWriter(sw, true);
		 throwable.printStackTrace(pw);
		 return sw.getBuffer().toString();
	}
}
