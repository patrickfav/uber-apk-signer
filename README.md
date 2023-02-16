# Uber Apk Signer
A tool that helps to sign, [zip aligning](https://developer.android.com/studio/command-line/zipalign.html) and verifying
multiple Android application packages (APKs) with either debug or provided release certificates (or multiple). It
supports [v1, v2](https://developer.android.com/about/versions/nougat/android-7.0.html#apk_signature_v2), [v3 Android signing scheme](https://source.android.com/security/apksigning/v3)
and  [v4 Android signing scheme](https://source.android.com/security/apksigning/v4). Easy and convenient debug signing
with embedded debug keystore. Automatically verifies signature and zipalign after every signing.

[![GitHub release](https://img.shields.io/github/release/patrickfav/uber-apk-signer.svg)](https://github.com/patrickfav/uber-apk-signer/releases/latest)
[![Github Actions](https://github.com/patrickfav/uber-apk-signer/actions/workflows/build_deploy.yml/badge.svg)](https://github.com/patrickfav/uber-apk-signer/actions)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=patrickfav_uber-apk-signer&metric=coverage)](https://sonarcloud.io/summary/new_code?id=patrickfav_uber-apk-signer)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=patrickfav_uber-apk-signer&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=patrickfav_uber-apk-signer)

Main features:

* zipalign, (re)signing and verifying of multiple APKs in one step
* verify signature (with hash check) and zipalign of multiple APKs in one step
* built-in zipalign & debug keystore for convenient usage
* supports v1, v2, v3 and v4 android apk singing scheme
* support for multiple signatures for one APK
* crypto/signing code relied upon official implementation

Basic usage:

    java -jar uber-apk-signer.jar --apks /path/to/apks

This should run on any Windows, Mac or Linux machine where JDK8 is installed. 

### Requirements

* JDK 8
* Currently on Linux 32bit: zipalign must be set in `PATH`

## Download

[Grab jar from the latest Release](https://github.com/patrickfav/uber-apk-signer/releases/latest)

## Demo

[![asciicast](https://asciinema.org/a/91092.png)](https://asciinema.org/a/91092)

## Command Line Interface

    -a,--apks <file/folder>           Can be a single apk or a folder containing multiple apks. These are used
                                      as source for zipalining/signing/verifying. It is also possible to
                                      provide multiple locations space seperated (can be mixed file folder):
                                      '/apk /apks2 my.apk'. Folder will be checked non-recursively.
       --allowResign                  If this flag is set, the tool will not show error on signed apks, but
                                      will sign them with the new certificate (therefore removing the old
                                      one).
       --debug                        Prints additional info for debugging.
       --dryRun                       Check what apks would be processed without actually doing anything.
    -h,--help                         Prints help docs.
       --ks <keystore>                The keystore file. If this isn't provided, will tryto sign with a debug
                                      keystore. The debug keystore will be searched in the same dir as
                                      execution and 'user_home/.android' folder. If it is not found there a
                                      built-in keystore will be used for convenience. It is possible to pass
                                      one or multiple keystores. The syntax for multiple params is
                                      '<index>=<keystore>' for example: '1=keystore.jks'. Must match the
                                      parameters of --ksAlias.
       --ksAlias <alias>              The alias of the used key in the keystore. Must be provided if --ks is
                                      provided. It is possible to pass one or multiple aliases for multiple
                                      keystore configs. The syntax for multiple params is '<index>=<alias>'
                                      for example: '1=my-alias'. Must match the parameters of --ks.
       --ksDebug <keystore>           Same as --ks parameter but with a debug keystore. With this option the
                                      default keystore alias and passwords are used and any arguments relating
                                      to these parameter are ignored.
       --ksKeyPass <password>         The password for the key. If this is not provided, caller will get a
                                      user prompt to enter it. It is possible to pass one or multiple
                                      passwords for multiple keystore configs. The syntax for multiple params
                                      is '<index>=<password>'. Must match the parameters of --ks.
       --ksPass <password>            The password for the keystore. If this is not provided, caller will get
                                      a user prompt to enter it. It is possible to pass one or multiple
                                      passwords for multiple keystore configs. The syntax for multiple params
                                      is '<index>=<password>'. Must match the parameters of --ks.
    -l,--lineage <path>               The lineage file for apk signer schema v3 if more then 1 signature is
                                      used. See here https://bit.ly/2mh6iAC for more info.
    -o,--out <path>                   Where the aligned/signed apks will be copied to. Must be a folder. Will
                                      create, if it does not exist.
       --overwrite                    Will overwrite/delete the apks in-place
       --skipZipAlign                 Skips zipAlign process. Also affects verify.
    -v,--version                      Prints current version.
       --verbose                      Prints more output, especially useful for sign verify.
       --verifySha256 <cert-sha256>   Provide one or multiple sha256 in string hex representation (ignoring
                                      case) to let the tool check it against hashes of the APK's certificate
                                      and use it in the verify process. All given hashes must be present in
                                      the signature to verify e.g. if 2 hashes are given the apk must have 2
                                      signatures with exact these hashes (providing only one hash, even if it
                                      matches one cert, will fail).
    -y,--onlyVerify                   If this is passed, the signature and alignment is only verified.
       --zipAlignPath <path>          Pass your own zipalign executable. If this is omitted the built-in
                                      version is used (available for win, mac and linux)

### Examples

Provide your own out directory for signed apks

    java -jar uber-apk-signer.jar -a /path/to/apks --out /path/to/apks/out

Only verify the signed apks

    java -jar uber-apk-signer.jar -a /path/to/apks --onlyVerify

Sign with your own release keystore

    java -jar uber-apk-signer.jar -a /path/to/apks --ks /path/release.jks --ksAlias my_alias

Provide your own zipalign executable

    java -jar uber-apk-signer.jar -a /path/to/apks --zipAlignPath /sdk/build-tools/24.0.3/zipalign

Provide your own location of your debug keystore

    java -jar uber-apk-signer.jar -a /path/to/apks --ksDebug /path/debug.jks

Sign with your multiple release keystores (see below on how to create a lineage file)

    java -jar uber-apk-signer.jar -a /path/to/apks --lineage /path/sig.lineage --ks 1=/path/release.jks 2=/path/release2.jks --ksAlias 1=my_alias1 2=my_alias2

Use multiple locations or files (will ignore duplicate files)

    java -jar uber-apk-signer.jar -a /path/to/apks /path2 /path3/select1.apk /path3/select2.apk

Provide your sha256 hash to check against the signature:

    java -jar uber-apk-signer.jar -a /path/to/apks --onlyVerify --verifySha256 ab318df27


### Process Return Value

This application will return `0` if every signing/verifying was successful, `1` if an error happens (e.g. wrong arguments) and `2` if at least 1 sign/verify process was not successful.

### Debug Signing Mode

If no keystore is provided the tool will try to automatically sign with a debug keystore. It will try to find on in the following locations (descending order):

* Keystore location provided with `--ksDebug`
* `debug.keystore` in the same directory as the jar executable
* `debug.keystore` found in the `/user_home/.android` folder
* Embedded `debug.keystore` packaged with the jar executable

A log message will indicate which one was chosen.

### Zipalign Executable

[`Zipalign`](https://developer.android.com/studio/command-line/zipalign.html) is a tool developed by Google to optimize zips (apks). It is needed if you want to upload it to the Playstore otherwise it is optional. By default, this tool will try to zipalign the apk, therefore it will need the location of the executable. If the path isn't passed in the command line interface, the tool checks if it is in `PATH` environment variable, otherwise it will try to use an embedded version of zipalign. 

If `--skipZipAlign` is passed no executable is needed.

### v1, v2 and v3 Signing Scheme

[Android 7.0 introduces APK Signature Scheme v2](https://developer.android.com/about/versions/nougat/android-7.0.html#apk_signature_v2), a new app-signing scheme that offers faster app install times and more protection against unauthorized alterations to APK files. By default, Android Studio 2.2 and the Android Plugin for Gradle 2.2 sign your app using both APK Signature Scheme v2 and the traditional signing scheme, which uses JAR signing.

[APK Signature Scheme v2 is a whole-file signature scheme](https://source.android.com/security/apksigning/v2.html) that increases verification speed and strengthens integrity guarantees by detecting any changes to the protected parts of the APK. The older jarsigning is called v1 schema.

[APK Signature Scheme v3](https://source.android.com/security/apksigning/v3) is an extension to v2 which allows a new signature lineage feature for key rotation, which basically means it will be possible to change signature keys.

#### Signature Lineage File in Schema v3

This tool does not directly support the creation of lineage files as it is considered a task done very rarely. You can create a lineage file with a sequence of certificates with [Google's `apksigner rotate`](https://developer.android.com/studio/command-line/apksigner.html#options-sign-general) and apply it as `-- lineage` arguments when signing with multiple keystores:

    apksigner rotate --out sig.lineage \
        --old-signer --ks debug1.keystore --ks-key-alias androiddebugkey \
        --new-signer --ks debug2.keystore --ks-key-alias androiddebugkey

    java -jar uber-apk-signer.jar -a /path/to/apks --lineage sig.lineage (...)

## Signed Release Jar

The provided JARs in the GitHub release page are signed with my private key:

    CN=Patrick Favre-Bulle, OU=Private, O=PF Github Open Source, L=Vienna, ST=Vienna, C=AT
    Validity: Thu Sep 07 16:40:57 SGT 2017 to: Fri Feb 10 16:40:57 SGT 2034
    SHA1: 06:DE:F2:C5:F7:BC:0C:11:ED:35:E2:0F:B1:9F:78:99:0F:BE:43:C4
    SHA256: 2B:65:33:B0:1C:0D:2A:69:4E:2D:53:8F:29:D5:6C:D6:87:AF:06:42:1F:1A:EE:B3:3C:E0:6D:0B:65:A1:AA:88

Use the jarsigner tool (found in your `$JAVA_HOME/bin` folder) folder to verify.

### Build with Maven

Use the Maven wrapper to create a jar including all dependencies

    ./mvnw clean install

### Checkstyle Config File

This project uses my [`common-parent`](https://github.com/patrickfav/mvn-common-parent) which centralized a lot of
the plugin versions as well as providing the checkstyle config rules. Specifically they are maintained in [`checkstyle-config`](https://github.com/patrickfav/checkstyle-config). Locally the files will be copied after you `mvnw install` into your `target` folder and is called
`target/checkstyle-checker.xml`. So if you use a plugin for your IDE, use this file as your local configuration.

## Tech-Stack

* Java 8
* Maven

# License

Copyright 2016 Patrick Favre-Bulle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
