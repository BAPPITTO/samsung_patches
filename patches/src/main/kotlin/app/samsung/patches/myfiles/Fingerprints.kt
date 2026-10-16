package app.samsung.patches.myfiles

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprints for Samsung "My Files" (com.android.myfiles).
 *
 * See the fingerprinting guide:
 * https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_1_fingerprinting.md
 *
 * IMPORTANT: These fingerprints are PLACEHOLDERS. They must be verified against
 * the actual dex of the My Files version you are targeting before the patch will
 * compile-time-match at runtime. Use a decompiler (e.g. Jadx) on
 * com.android.myfiles.apk to find the real methods, then update:
 *  - definingClass / name / accessFlags / returnType / parameters
 *  - filters (strings, literals, method calls unique to the method body)
 *
 * Declaring fingerprints as named objects means that if a fingerprint fails to
 * match, the exception stack trace includes the fingerprint name, which makes
 * debugging failed patches much easier.
 */

/**
 * Placeholder: device brand check used throughout My Files to gate
 * Samsung-only features (network drive, Samsung Cloud/Knox storage, etc.).
 *
 * Typical smali shape in Samsung apps:
 *   const-string v0, "samsung"           // or Build.BRAND / ro.product.brand compare
 *   invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
 *   move-result v0
 *   if-eqz v0, :cond_device_supported
 */
object DeviceBrandCheckFingerprint : Fingerprint(
    // Package-scoped match: any class inside the app's util package.
    // TODO: Narrow to the exact obfuscated/unobfuscated class once verified in Jadx.
    definingClass = ":com/samsung/android/filemanager/util/",
    name = "isSamsungDevice",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        string("samsung"),
    )
)

/**
 * Placeholder: Knox / Secure Folder availability check.
 * On non-Samsung devices this returns false and hides encrypted folders.
 * TODO: Verify class/method in the target APK.
 */
object KnoxSupportFingerprint : Fingerprint(
    definingClass = "/KnoxUtils;",
    name = "isKnoxAvailable",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList()
)
