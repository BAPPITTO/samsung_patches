package app.samsung.patches.myfiles

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.samsung.patches.shared.Constants.MY_FILES_COMPATIBILITY

/**
 * Removes the "only supported on Samsung devices" nag dialogs and
 * feature-blocked toasts shown by My Files on non-Samsung devices.
 *
 * STATUS: Scaffold. Add verified fingerprints for the dialog/toast methods,
 * then inject a single return-void at instruction 0 of each method.
 */
@Suppress("unused")
val removeSamsungOnlyRestrictions = bytecodePatch(
    name = "Remove Samsung-only restrictions",
    description = "Removes nag dialogs that block features on non-Samsung devices.",
    default = true,
    category = "Compatibility"
) {
    compatibleWith(MY_FILES_COMPATIBILITY)

    execute {
        // TODO: Once the restriction-dialog method is located in Jadx (search for
        // strings like "Only supported on Galaxy devices"), define a fingerprint
        // for it in Fingerprints.kt and stub it out like this:
        //
        // RestrictionDialogFingerprint.method.addInstructions(
        //     0,
        //     """return-void"""
        // )

        // Placeholder so the scaffold compiles; replace with real injections above.
        KnoxSupportFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
