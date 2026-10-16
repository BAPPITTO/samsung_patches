package app.samsung.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

/**
 * App targets for Samsung apps included in this patch bundle.
 *
 * IMPORTANT: The declaration below must match the file type on APKMirror/UpToDown.
 * If declared as APK but the store file is XAPK/APKM, Morphe Manager will not
 * redirect users to the correct download page.
 */
object Constants {
    /**
     * Samsung "My Files" file manager.
     * Package name: com.android.myfiles
     *
     * TODO: Before releasing, verify against the actual APK you downloaded:
     *  - Confirm the exact version name(s) available on apkmirror.com / uptodown.com.
     *  - Set apkFileType to match the store file type (APK / APKM / XAPK).
     *  - Optionally restrict signatures = setOf("<sha256-signature>") to the
     *    official Samsung signing key so users cannot patch tampered APKs.
     */
    val MY_FILES_COMPATIBILITY = Compatibility(
        // App name as it appears in the Android launcher.
        name = "My Files",
        packageName = "com.android.myfiles",
        apkFileType = ApkFileType.APK,
        // Icon color in Morphe Manager (Samsung My Files icon is a blue folder).
        appIconColor = 0x1A73E8,
        targets = listOf(
            // Version confirmed while developing the patch.
            AppTarget(
                version = "15.1.00.122"
            ),
            // Latest versions supported experimentally until verified.
            AppTarget(
                version = null,
                isExperimental = true
            )
        )
    )
}
