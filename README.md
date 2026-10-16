# 📱 Samsung Patches

A collection of [Morphe](https://morphe.software) patches to enable Samsung apps to work on other Android devices.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=rambo3721/samsung_patches

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
| My Files (Samsung file manager) | `com.android.myfiles` | Samsung device spoof · Remove Samsung-only restrictions | 🚧 Scaffold — fingerprints unverified |

## ⚠️ My Files patch status (read before releasing)

The My Files patches in `patches/src/main/kotlin/app/samsung/patches/myfiles/` are a **scaffold**:

1. **Verify the package name** against the APK you downloaded (`aapt dump badging myfiles.apk | head -1`).
   It is expected to be `com.android.myfiles`, but Samsung has shipped variants under different names.
2. **Update `Constants.MY_FILES_COMPATIBILITY`** with the exact version name(s) you tested, and set
   `apkFileType` to match how the APK is distributed on ApkMirror/UpToDown (APK / APKM / XAPK).
3. **Replace the placeholder fingerprints** in `Fingerprints.kt`. Open the APK in Jadx, search for
   strings like `"samsung"`, `"galaxy"`, `"only supported"`, and locate the real device-check /
   restriction-dialog methods, then encode them as fingerprints per the
   [fingerprinting guide](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_1_fingerprinting.md).
4. Build and test-patch locally (see *Dev usage* below) before tagging a release.

## 🚀 Getting development started

To start using this project, follow these steps:

1. [Setup](https://github.com/MorpheApp/morphe-documentation/blob/main/docs/morphe-development/README.md) your development environment including adding a GitHub PAT as described [here](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md#-prepare-the-environment).
2. Enable "Allow GitHub Actions to create and approve pull requests" in your repo Settings > Actions > General > Workflow permissions.
3. Download the stock **My Files** APK for the version you want to support.
4. Follow the *My Files patch status* checklist above.

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
