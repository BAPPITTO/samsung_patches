package app.samsung.patches.myfiles

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.samsung.patches.shared.Constants.MY_FILES_COMPATIBILITY

private const val EXTENSION_CLASS = "Lapp/samsung/extension/myfiles/DeviceSpoofPatch;"

/**
 * Spoofs the device as a Samsung device so that My Files enables all
 * Samsung-only features on non-Samsung Android devices:
 *  - Removes the "This feature is only supported on Samsung devices" gating.
 *  - Enables Network drive / FTP / SFTP storage sources.
 *  - Keeps encrypted USB drive and Secure Folder entries visible
 *    (functionality still depends on the ROM, but the UI no longer blocks them).
 *  - Re-enables the OneDrive cloud-storage source so the standard Microsoft
 *    sign-in flow works instead of the Galaxy-only pairing check.
 *
 * STATUS: Scaffold. The fingerprint in Fingerprints.kt is a placeholder and
 * MUST be verified against the real dex before this patch works.
 */
@Suppress("unused")
val samsungDeviceSpoof = bytecodePatch(
    name = "Samsung device spoof",
    description = "Spoofs the device brand so Samsung-only features work on any Android device.",
    default = true,
    category = "Compatibility"
) {
    compatibleWith(MY_FILES_COMPATIBILITY)

    extendWith("extensions/extension.mpe")

    execute {
        // Force the device-brand check to return the value provided by the
        // extension instead of the real result.
        DeviceBrandCheckFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $EXTENSION_CLASS;->isSamsungDevice()Z
                move-result v0
                return v0
            """
        )

        // TODO: Add further injection points once verified in Jadx, e.g.:
        //  - KnoxSupportFingerprint.method -> always report Knox available.
        //  - Any PackageManager-based checks for com.samsung.android.* services.
    }
}

/**
 * Secure Folder visibility patch.
 *
 * My Files hides the "Secure Folder" storage entry whenever the Knox/Secure
 * Folder availability check fails (always on non-Samsung devices). This patch
 * forces those checks to report "available" so the entry stays visible; real
 * encryption still depends on the ROM/Knox container service, but the UI no
 * longer blocks it — matching the behavior promised by samsungDeviceSpoof.
 */
@Suppress("unused")
val secureFolderVisibility = bytecodePatch(
    name = "Enable Secure Folder view",
    description = "Keeps the Secure Folder storage entry visible on non-Samsung devices.",
    default = true,
    category = "Compatibility"
) {
    compatibleWith(MY_FILES_COMPATIBILITY)

    execute {
        // Knox availability -> true
        KnoxSupportFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        // Secure Folder availability -> true
        SecureFolderCheckFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}

/**
 * OneDrive cloud-storage patch.
 *
 * In stock My Files the OneDrive source is gated behind a Galaxy-only
 * Samsung/Microsoft account pairing check, so it shows "not available" on
 * every other device. Forcing the check to true restores the OneDrive entry
 * and lets the standard Microsoft sign-in flow run.
 */
@Suppress("unused")
val oneDriveSupport = bytecodePatch(
    name = "Enable OneDrive",
    description = "Re-enables the OneDrive cloud storage source on non-Samsung devices.",
    default = true,
    category = "Compatibility"
) {
    compatibleWith(MY_FILES_COMPATIBILITY)

    execute {
        OneDriveCheckFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
