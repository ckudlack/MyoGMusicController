# MyoMusicController
Control music playback on your phone with a Myo armband!
https://www.thalmic.com/myo/

In order to use this app, you'll need a Myo armband (obviously)

The app works by running a background service that listens for commands from your Myo and sends them to the device's music playback controller.

Not every music playback application implements a MediaController, but here are some examples:
- Google Play Music, Soundcloud, Spotify, Pandora




In order for Myo to work, you'll need to connect it to the app and sync it:

- Start the app
- Make sure the light on your Myo is on
- Hit the 'Scan' button
- The circle next to 'Connected' should change from red to green
- Perform the Myo Sync gesture until the circle next to 'Synced' changes to green
  - You may have to do this a couple of times, the Myo SDK doesn't always recognize the sync gesture right away

Now, start up your music player of choice, and start a song.

Before you can control audio playback, you need to unlock your Myo. You do this by tapping your pinkie to your thumb twice in quick succession

Music playback commands are:

- Play/Pause - Spread hand
- Skip Back - Wave hand to the left
- Skip Forward - Wave hand to the right
- Change volume - Make a fist, then rotate clockwise to increase or counterclockwise to decrease
