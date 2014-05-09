RocBrowser
==========

Android stock browser based on chromium engine.

==============================================================================


In order to run the app, please build Chromuium@r197479 (chromium.r197479.tgz) and reuse the engine lib.

You may follow Chromium build guide and run the following command line to generate libchromiumtestshell.so:

    * ninja -C out/Release -j10 chromium_testshell

and put it under "/libs/armeabi-v7a/" according to your target.

==============================================================================

Bookmark, History and Settings were part of "develop" branch.
