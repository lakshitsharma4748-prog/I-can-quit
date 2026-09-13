# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# SafeShield does not ship with debugging symbols stripped by default while
# minification is disabled. When minification is enabled in a later phase,
# rules for Room, VpnService, and DevicePolicyManager-related reflection
# should be added here.
