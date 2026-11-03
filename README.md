# Saha Kids Test

An Android app for Class 1 Math Olympiad practice (IMO and CREST style). It works offline on an Android tablet or phone.

- Timed tests: 15 minutes (10 questions), 30 minutes (20) or 60 minutes (35, the real exam format: 25 normal + 10 Achievers).
- Random questions every time, with options shuffled. Questions a child saw in their last 3 tests are used only when fresh ones run out.
- Answers and working are shown only after the test is submitted. The test submits itself when time runs out.
- A profile for each child, with score history and the section that needs most practice.
- A **Read aloud** button uses the tablet's own voice.
- Add, edit and delete questions in the app, or add many at once from a CSV file.

Starts with 220 questions: the 120 from Practice Paper 1 and the 100 harder ones from Paper 2 (Challenge).

## Download and install

1. On the tablet, open the latest release: https://github.com/Subhashistech/saha-kids-test/releases/latest
2. Tap **SahaKidsTest.apk** to download it. You need to be signed in to GitHub if the repository is private.
3. Open the downloaded file. If Android asks, allow your browser (or Files app) to **install unknown apps**, then tap **Install**.

To update, download the newest release and install it the same way. Scores and your questions are kept.

## Adding many questions at once (CSV)

In the app, open **Add or edit questions → ⋮ → Add questions from a CSV file**. The file needs these columns:

```
section,question,option_a,option_b,option_c,option_d,answer,working
C,"A pen costs ₹12. How much do 3 pens cost?",₹15,₹36,₹24,₹30,B,"12 + 12 + 12 = 36"
```

- `section`: A Logical Reasoning, B Mathematical Reasoning, C Everyday Mathematics, D Shapes and Patterns, E Achievers (2 marks). The letter or the name works.
- `answer`: the letter of the correct option, A to D.
- `working`: optional; shown in the answers after the test.

**⋮ → Save all questions to a CSV file** gives you a file in this format to edit.

## How the APK is built

Every push to `main` builds the app on GitHub Actions and publishes it as a new release. The app is signed with `app/saha.keystore` so new versions install over old ones without losing data.
