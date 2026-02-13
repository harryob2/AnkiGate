# AnkiGate ProGuard Rules

# Google Play Billing Library
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }

# Keep AIDL interfaces for billing
-keep class com.android.vending.billing.IInAppBillingService { *; }
-keep class com.android.vending.billing.IInAppBillingService$Stub { *; }
