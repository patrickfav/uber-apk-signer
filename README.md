# Uber Apk Signer
A tool that helps signing and zip aligning multiple files with either debug or provided release certificatas

Main features:

* zipalign, signing and verifying of multiple APKs in one step
* built-in zipalign & debug keystore for convenient usage
* supports v1 and v2 android apk singing scheme with using the original android `apksigner.jar` from android sdk (see https://source.android.com/security/apksigning/v2.html)

Basic usage:

    java -jar uber-apk-signer.jar -a /path/to/apks


This should run on any Windows, Mac or Linux machine where Java8+ is installed. 

## Download

[Grab jar from latest Release](https://github.com/patrickfav/uber-apk-signer/releases/latest)

## Command Line Interface

    -a,--apks <file/folder>     Can be a single apk or a folder containing multiple apks. These are used as
                                source for zipalining/signing/verifying
       --debug                  Prints additional info for debugging.
       --dryRun                 Check what apks would be processed
    -h,--help                   Prints help docs.
       --ks <keystore>          The keystore file. If this isn't provided, will try to sign with
                                debug.keystore
       --ksAlias <alias>        The alias of the used key in the keystore. Must be provided if --ks is
                                provided.
       --ksKeyPass <password>   The password for the key. If this is not provided, caller will get an user
                                prompt to enter it.
       --ksPass <password>      The password for the keystore. If this is not provided, caller will get an
                                user prompt to enter it.
    -o,--out <path>             Where the aligned/signed apks will be copied to. Must be a folder. Will
                                generate, if not existent.
       --onlyVerify             If this is passed, the signature and alignment is only verified.
       --overwrite              Will overwrite/delete the apks in-place
       --skipZipAlign           Skips zipAlign process. Also affects verify.
    -v,--version                Prints current version.
       --verbose                Prints more output, especially useful for sign verify.
       --zipAlignPath <path>    Pass your own zipalign executable. If this is omitted the built-in version is
                                used (available for win, mac and linux)

Provide your own out directory

    java -jar uber-apk-signer.jar -a /path/to/apks --out /path/to/apks/out

Only verify the signed apks

    java -jar uber-apk-signer.jar -a /path/to/apks --onlyVerify

Sign with your own release keystore

    java -jar uber-apk-signer.jar -a /path/to/apks --ks /path/release.jks --ksAlias my_alias

Provide your own zipalign executable

    java -jar uber-apk-signer.jar -a /path/to/apks --zipAlignPath /sdk/build-tools/24.0.3/zipalign

## Build

Use maven (3.1+) to create a jar including all dependencies

    mvn clean package

## Tech Stack

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
