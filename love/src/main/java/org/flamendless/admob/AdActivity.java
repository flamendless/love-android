package org.flamendless.admob;

import org.love2d.android.BuildConfig;
import org.love2d.android.GameActivity;

/* Admob Imports */

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;

import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.AdError;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.FullScreenContentCallback;

import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/* Other Imports */

import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.annotation.NonNull;

import java.lang.StringBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.io.Reader;
import java.io.PrintWriter;
import java.io.StringWriter;

public class AdActivity extends GameActivity {

	private String appID = BuildConfig.ADMOB_APP_ID;
	private boolean fullscreen = false; //CONFIG for banner
	private boolean collectConsent = BuildConfig.COLLECT_CONSENT; //CONFIG for GDPR consent
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
	private boolean bannerVisible = false;
	private boolean bannerHasFinishedLoading = false;
	private boolean bannerCreated = false;
	private String bannerPosition;
	private String bannerAdID = ""; //no need to set

	//Interstitial stuff
	private InterstitialAd mInterstitialAd;
	private boolean hasInterstitial = false;
	private boolean interstitialLoaded = false;

	//Rewarded video stuff
	private RewardedAd mRewardedAd;
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

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        RequestConfiguration configuration =
				new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
		MobileAds.setRequestConfiguration(configuration);

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
                    mAdView.setAdSize(AdSize.BANNER);
                    mAdView.setAdUnitId(adID);

                    // Another way to set the size
					//AdSize adSize = getAdSize();
					//mAdView.setAdSize(adSize);

                    adRequest = new AdRequest.Builder().build();
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
							if (bannerVisible)
							{
								mAdView.setVisibility(View.GONE);
								mAdView.setVisibility(View.VISIBLE);
							}
							else
							{
								mAdView.setVisibility(View.GONE);
							}
							Log.d("AdActivity","Banner - onAdLoaded: " + bannerVisible);
							bannerHasFinishedLoading = true;
						}

						@Override
						public void onAdClicked() {
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
				bannerVisible = false;
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
				bannerVisible = true;
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
                adRequest = new AdRequest.Builder().build();

                InterstitialAd.load(mSingleton, adID, adRequest,
                    new InterstitialAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            Log.i("AdActivity", "interstitialDidReceive");

                            mInterstitialAd = interstitialAd;
                            interstitialLoaded = true;

                            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                                @Override
                                public void onAdClicked() {
                                    Log.d("AdActivity","adClicked: Interstitial");
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    Log.d("AdActivity", "Ad dismissed fullscreen content.");
                                    interstitialDidClose = true;
                                    mInterstitialAd = null;
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                    Log.e("AdActivity", "Ad failed to show fullscreen content.");
                                    mInterstitialAd = null;
                                }

                                @Override
                                public void onAdImpression() {
                                    Log.d("AdActivity", "Ad recorded an impression.");
                                    interstitialLoaded = false;
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    Log.d("AdActivity", "Ad showed fullscreen content.");
                                    interstitialLoaded = false;
                                }
                            });

                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Log.i("AdActivity", "onInterstitialFailedToLoad: Error " + error.getCode() + " " + error.getMessage());
                            interstitialDidFailToLoad = true;
                        }

                    }
                );

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
					if (mInterstitialAd != null)
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
					if (mInterstitialAd != null)
					{
						mInterstitialAd.show(mSingleton);
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

                adRequest = new AdRequest.Builder().build();

                RewardedAd.load(mSingleton, adID, adRequest,
                    new RewardedAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd)  {
                            Log.i("AdActivity", "rewardedAdDidReceive");

                            mRewardedAd = rewardedAd;
                            rewardedAdLoaded = true;

                            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                                @Override
                                public void onAdClicked() {
                                    Log.d("AdActivity","adClicked: RewardedAd");
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    Log.d("AdActivity", "Ad dismissed fullscreen content.");
                                    rewardedAdDidStop = true;
                                    mRewardedAd = null;
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                    Log.e("AdActivity", "Ad failed to show fullscreen content.");
                                    mRewardedAd = null;
                                }

                                @Override
                                public void onAdImpression() {
                                    Log.d("AdActivity", "Ad recorded an impression.");
                                    rewardedAdLoaded = false;
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    Log.d("AdActivity", "Ad showed fullscreen content.");
                                    rewardedAdLoaded = false;
                                }
                            });

                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Log.i("AdActivity", "onRewardedAdFailedToLoad: Error " + error.getCode() + " " + error.getMessage());
                            rewardedAdDidFailToLoad = true;
                        }

                    }
                );

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
					if (mRewardedAd != null)
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
					if (mRewardedAd != null)
					{
						mRewardedAd.show(mSingleton, new OnUserEarnedRewardListener() {
                            @Override
                            public void onUserEarnedReward(RewardItem reward) {
                                rewardedAdDidFinish = true;
                                rewardQty = reward.getAmount();
                                rewardType = reward.getType();
                            }
                        });
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
