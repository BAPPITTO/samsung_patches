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
