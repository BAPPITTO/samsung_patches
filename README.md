# 📱 Samsung Patches

A collection of [Morphe](https://morphe.software) patches to enable Samsung apps to work on other Android devices.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=BAPPITTO/samsung_patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release.

     If you wish for the patches list to be collapsed, then remove the word 'EXPANDED' from the comment tag above.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END
     comment blocks entirely. -->

#### A list of your patches will automatically be shown here after your first patches release is created.

&nbsp;

## 🚀 Supported apps

| App | Package name | Patches | Status |
| --- | --- | --- | --- |
| My Files (Samsung file manager) | `com.android.myfiles` | Samsung device spoof · Remove Samsung-only restrictions · Enable Secure Folder view · Enable OneDrive | 🚧 Scaffold — fingerprints unverified |

## ✅ Things to do (author: BAPPITTO)

The My Files patches in `patches/src/main/kotlin/app/samsung/patches/myfiles/` are a **scaffold**.
Before this repo can ship a working release, here is what still needs doing, in plain language:

### 1. Confirm you're targeting the right app build
- Download the stock **My Files** APK from ApkMirror or UpToDown for the exact version you want to support.
- Double-check the package name of the file you actually downloaded by running
  `aapt dump badging myfiles.apk | head -1`. We expect `com.android.myfiles`, but Samsung has
  shipped variants under different package names, so verify — don't assume.

### 2. Update the compatibility metadata
- Open `patches/src/main/kotlin/app/samsung/patches/shared/Constants.kt` and edit
  `MY_FILES_COMPATIBILITY` so it lists the exact version name(s) you personally tested
  (e.g. `15.1.00.122`) — Morphe only offers patches for versions listed there.
- Set `apkFileType` to match how the APK is really distributed on the store you use
  (plain APK, APKM bundle, or XAPK), otherwise patching will fail for users.

### 3. Replace the placeholder fingerprints with real ones
This is the biggest remaining task. The two fingerprints currently in `Fingerprints.kt`
(`DeviceBrandCheckFingerprint` and `KnoxSupportFingerprint`) are guesses and **will not match**
the real dex until verified:
- Decompile the APK with **Jadx** and hunt for the code that gates Samsung-only behavior:
  search for strings like `"samsung"`, `"galaxy"`, `"ro.product.brand"`, `"Build.BRAND"`,
  `"only supported"`, `"not available"`, and anything Knox/KLMS-related.
- For each real method you find, record its defining class, method name, access flags,
  return type, and parameter list, plus any unique strings/constants inside the method body —
  these become the fingerprint's match criteria and filters.
- Rewrite the entries in `Fingerprints.kt` to describe those actual methods, following the
  [Morphe fingerprinting guide](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_1_fingerprinting.md).
- Keep each fingerprint as its own named `object` — if a fingerprint fails to match at runtime,
  the stack trace will name it, which makes debugging far easier.
- Pay special attention to these feature gates (each has its own fingerprint + patch):
  - **Secure Folder** (`SecureFolderCheckFingerprint` / `KnoxSupportFingerprint`, patched by
    "Enable Secure Folder view" in `SamsungDeviceSpoof.kt`) — Jadx search: `SecureFolder`,
    `secure folder`, `Knox`, `KLMS`, `ContainerService`.
  - **OneDrive** (`OneDriveCheckFingerprint`, patched by "Enable OneDrive") — Jadx search:
    `OneDrive`, `live.com`, `MicrosoftAccount`, `only supported`.
  - If a feature turns out not to need patching (or can't be found), delete that fingerprint and
    its patch rather than shipping dead code.

### 4. Write the patch logic against the verified methods
- With real fingerprints in place, review the corresponding `Patches.kt` mutations so they
  actually flip the device check (e.g. always return `true`) instead of blindly mutating a
  method that may not exist.

### 5. Build, test, and only then release
- Build locally with `./gradlew buildAndroid` (fix any Kotlin compile errors first).
- Patch the APK with Morphe Desktop and install it on a **non-Samsung** test device; confirm
  My Files launches and the previously-blocked features now work, with no crashes.
- Only once everything is verified, merge to `main` so semantic-release cuts a real version
  and the auto-generated patch list below gets populated.

## 🚀 Getting development started

To start using this project, follow these steps:

1. [Setup](https://github.com/MorpheApp/morphe-documentation/blob/main/docs/morphe-development/README.md) your development environment including adding a GitHub PAT as described [here](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md#-prepare-the-environment).
2. Enable "Allow GitHub Actions to create and approve pull requests" in your repo Settings > Actions > General > Workflow permissions.
3. Download the stock **My Files** APK for the version you want to support.
4. Follow the *Things to do* checklist above.

## 🧑‍💻 Dev usage

- **Make all changes to the `dev` branch.**
- Build patches locally: `./gradlew buildAndroid` → output at `patches/build/libs/patches-*.mpp`.
- Test-patch the My Files APK with Morphe Desktop:
  ```bash
  java -jar morphe-desktop*-all.jar patch \
      --patches patches/build/libs/patches-<version>.mpp \
      --out myfiles-patched.apk \
      com.android.myfiles.apk \
      --install
  ```
- Use semantic commit messages (`feat:` / `fix:` / `chore:`); `feat:` and `fix:` trigger
  automatic pre-releases from the `dev` branch via GitHub Actions.
- Merge `dev` into `main` (no squash) for a stable release.
