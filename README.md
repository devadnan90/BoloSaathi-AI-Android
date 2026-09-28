<p align="center"><img src="playstore_icon_512.png" width="140" alt="Bolo Saathi icon"></p>

# Bolo Saathi

A phone that listens in your language and does the work for you.

Bolo Saathi is an Android app for people who own a smartphone but struggle with apps built in English. You press one big button and speak in Hindi, Bhojpuri or Maithili. The app understands what you want, does it inside your other apps, and tells you out loud when it is done.

We built it for the AI Build Challenge 2026 (Track 06, AI for Bharat in Indian Languages).

## Why we built this

Think of a 60 year old kirana owner in Bihar, or someone's parents back in the village. They have an Android phone and a UPI app. But every recharge, every payment and every government form means waiting for their son to come home, or paying a cyber cafe to do it. They end up sharing OTPs with strangers, and a fake SMS in English is very easy to fall for.

We wanted a helper that sits inside their phone, speaks their language, and never gets tired of their questions.

## What it can do

**Talk to get things done.** Say "मम्मी को 500 भेज दो" or "बेटा को लिखो मैं पहुँच गया" and the app opens the right app, fills things in and asks you before anything goes out. For money, it opens your UPI app with the amount filled in. You enter the PIN yourself.

**Read things out loud.** Point the camera at a bill, a letter or a medicine strip and hear what it says in your language. Inside any app, tap the floating mic and ask "ये क्या लिखा है?"

**Yojana Saathi.** Ask which government schemes you can get. It asks a few simple questions and tells you the scheme, the benefit, the papers you need and where to apply.

**Scam check.** Paste any SMS or offer and ask if it is real. While you are inside a UPI app, it warns you out loud if the screen looks like a "request money" trap, even without internet.

**Medicine reminders.** "रोज़ सुबह 8 बजे BP की दवा याद दिलाना" sets a daily reminder that speaks at that time.

**Expense diary.** "आज 200 की सब्ज़ी ली" gets noted. Ask "इस महीने कितना खर्च हुआ?" any time.

**Morning update.** Every morning at 8 it tells you the weather, today's medicines and what you spent yesterday.

**Family safety.** An SOS button sends your location to a family member and calls them. The family also gets an SMS when a payment of ₹2000 or more is being made, and the app warns you after a call from an unknown number.

**Messages read aloud.** New WhatsApp and SMS messages can be read out as they arrive. OTP messages are never read.

**Home screen widget.** A big mic on the home screen. Tap it and start speaking.

## Safety rules we never break

1. The app always asks before sending money, sending a message or submitting a form.
2. It never reads, types or stores a PIN, OTP, CVV or password.
3. Your name, number, contacts and diary stay on your phone. We do not run our own server.
4. Text is sent to Gemini only to understand what you said.

## How it works

1. Android's speech recognizer turns your voice into text.
2. Gemini works out what you want and replies in your language.
3. Simple jobs like WhatsApp or UPI use direct app links.
4. Longer jobs like a recharge use an Accessibility Service. It reads the screen, picks one step, taps or types, checks the screen again and repeats. Before any risky step it stops and asks you.
5. Android's text to speech reads the answer out loud.

## Built with

Kotlin, Jetpack Compose, Material 3, Android Accessibility Service, Gemini API, OkHttp, AlarmManager, NotificationListenerService, and Open-Meteo for weather.

## Run it yourself

1. Clone the repo and open it in Android Studio.
2. Get a free Gemini API key from https://aistudio.google.com/apikey
3. Add this line to local.properties in the project root:

        GEMINI_API_KEY=your_key_here

4. Run it on a real Android phone with Android 8 or newer. The mic and the accessibility features do not work well on an emulator.
5. Open the app, tap "अनुमति दें" and turn on Bolo Saathi in Accessibility settings.

You can also paste the key inside the app under Settings.

## Permissions and why we need them

| Permission | Why |
|---|---|
| Microphone | To hear you, only when you press the mic |
| Accessibility | To tap and type in other apps when you ask, and to spot UPI scams |
| Contacts | To find the number when you say a name |
| SMS | To check a message for fraud when you tap "जाँचें", and for SOS |
| Phone and call log | To warn you after a call from an unknown number |
| Location | For SOS and the morning weather |
| Notifications | For medicine reminders and message reading |

Every feature that needs extra permissions can be switched off in Settings.

## Project layout

    app/src/main/java/com/orynex/bolosaathi
      core/    voice, Gemini, the main assistant logic, reminders, diary, background jobs
      agent/   accessibility service, phone control loop, floating mic, scam rules
      ui/      all Compose screens
    app/src/main/assets
      schemes.json   government schemes used by Yojana Saathi
      legal/         privacy policy, terms, data page

## What is next

Better Bhojpuri and Maithili speech with Sarvam or Bhashini, more Indian languages, and voice guided form filling for government schemes.

## Team

Team Orynex. Md Adnan Hassan, IIIT Manipur.

Made with ❤️ in Bihar.
