package app.samsung.extension.myfiles;

@SuppressWarnings("unused")
public class DeviceSpoofPatch {

    /**
     * Called from patched smali in com.android.myfiles.
     * Return true to make My Files believe it is running on a Samsung device.
     *
     * Complex logic (e.g. reading system properties, faking Build.BRAND for
     * specific call sites) goes here. Simple fixed-value overrides do not need
     * an extension at all, but keeping the call here allows runtime tweaks
     * without re-patching.
     */
    public static boolean isSamsungDevice() {
        return true;
    }
}
