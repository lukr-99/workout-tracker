# Muscle recovery and weekly volume

This note is the basis for Ember's muscle map on the Progress tab: the recovery model in
`domain/recovery/RecoveryEngine.kt`, its numbers in `RecoveryConfig.kt`, and the info sheet that
explains it (`ui/components/RecoveryInfoSheet.kt`). It was written in October 2026 (GoalMaker
Ember #41).

Each citation was checked against its PubMed or Europe PMC record. Numbers come from the abstract
unless the text says "full text". Where the evidence is thin, the note says so, and the model uses
a reasoned choice.

## 1. What the research says

### 1.1 Muscle protein synthesis after a session

- After heavy lifting, muscle protein synthesis (MPS) rises about 50% at 4 h and 109% at 24 h. At
  about 36 h it is almost back to baseline (MacDougall 1995, quoting Chesley 1992).
- In untrained people, MPS is up 112% at 3 h, 65% at 24 h and 34% at 48 h (Phillips 1997).
- Trained muscle has a bigger but shorter rise. After 8 weeks of training, the trained leg was back
  to baseline at 28 h while the untrained leg was still up 70% (Tang 2008).
- Early in a new program, much of that MPS goes to repairing damage, not growth. MPS only tracks
  growth from about week 3, when damage is lower (Damas 2016).

So a muscle is busy rebuilding for roughly 1 to 2 days after a hard session. That matches the common
advice to train a muscle every 2 to 3 days.

### 1.2 How long performance takes to come back

- Sets that stop short of failure recover in about 24 to 48 h. Sets to failure are still impaired at
  48 h and need about 48 to 72 h, even with the same total reps (Morán-Navarro 2017; Pareja-Blanco
  2019, 2020).
- In well-trained men, 4 sets to failure at 80% of 1RM left squat bar speed 8.6% down at 72 h. The
  three big lifts had a similar time course (Belcher 2019).
- A review of 24 studies puts lower body recovery at 48 to 72 h and upper body at 24 h or less. Failure,
  more volume, big multi-joint lifts, eccentric work and training at long muscle lengths all make it
  longer (Sousa 2024).

### 1.3 Differences between muscles

- After the same eccentric work in 9 muscles, the arms (elbow flexors and extensors) and the chest lost
  the most strength. Hamstrings lost more than quads. Strength was still 13 to 49% down at day 4 in
  these untrained men (Chen 2019).
- In normal training, the lower body still takes longer, because leg lifts move more load and more
  muscle (Sousa 2024).
- There is almost no direct data for calves, abs, forearms, glutes or shoulders. Their faster recovery
  in Ember is a reasoned guess (small muscles, used daily, little damage in Chen 2019).

### 1.4 Eccentric work and soreness

- Soreness starts 12 to 24 h after unaccustomed eccentric work, peaks at 24 to 72 h and is gone in 5 to
  7 days. Soreness is a poor guide to actual damage (Hody 2019).
- Eccentric work causes more micro-injury than other contraction types (Cheung 2003).

### 1.5 Training status

- After one damaging session, the next similar one causes less damage and recovers faster. This
  "repeated bout effect" lasts weeks to months (Hyldahl 2017, Hody 2019, Chen 2019).
- No study compares hours to recover in trained and untrained people head to head. Ember does not
  model training status. It calibrates to a lifter who knows their exercises.

### 1.6 Weekly volume

- Each extra weekly set adds about 0.37% muscle growth, with a graded dose response (Schoenfeld,
  Ogborn and Krieger 2017).
- In trained people more volume grew more muscle, while strength gains were the same (Schoenfeld 2019).
- 12 to 20 weekly sets per muscle "may be an optimum standard recommendation" for trained young men
  (Baz-Valle 2022). The ACSM 2026 position stand says hypertrophy improves with 10 or more sets a week
  (Currier 2026).
- A large 2026 meta-regression (67 studies) found growth keeps rising with volume, with shrinking
  returns. Counting sets for helper muscles as half a set fit the data best (Pelland 2026).

### 1.7 Frequency

- Twice a week beat once a week (Schoenfeld 2016), but with equal volume, frequency made no clear
  difference (Schoenfeld 2019, J Sports Sci; Pelland 2026). Ember does not score frequency.

### 1.8 How close to failure

- Failure is not clearly better for growth than stopping a few reps short (Refalo 2023). Growth does
  rise as sets end closer to failure, more so near failure (Robinson 2024).
- There is no research definition of a "hard set". The coaching rule is a set that ends within about 0
  to 4 reps in reserve (RIR). People misjudge RIR by about one rep (Halperin 2022).

## 2. What was off in the old model

The old engine used one 36 h half-life for every muscle and counted fatigue as
`sets + tonnage / 1000 kg`, with readiness `100 * exp(-load / 3)`.

1. **Tonnage drove fatigue.** Heavy lifts like squats piled on extra load units just for weight moved.
   Research ties recovery to sets, effort and damage, not kilograms.
2. **Same speed for all muscles.** Calves and quads recovered at the same rate.
3. **Far too slow overall.** A normal 5 set bench session took over 5 days to show as ready. Research
   says 1 to 3 days.
4. **Effort ignored.** RIR, RPE and failure tags did not matter. Neither did eccentric sets.
5. **Weekly sets were every non-warm-up set**, including easy ones and empty rows, with no target.
6. **Names were not matched.** "Legs", "Quadriceps" and "Quads" were three rows. Wger names like
   "Biceps brachii" did not light up the map.

## 3. The new model

### 3.1 Muscles

`MuscleGroup` has 11 muscles: Chest, Back, Shoulders, Biceps, Triceps, Forearms, Abs, Glutes, Quads,
Hamstrings and Calves. Each has aliases (Core and Obliques are Abs, Lats and Traps are Back,
Biceps brachii is Biceps, and so on). Broad names spread out (`MuscleGroups`):

- Legs and Lower body count fully for Quads, Hamstrings and Glutes.
- Arms counts fully for Biceps and Triceps.
- Full Body counts half for Chest, Back, Shoulders, Quads, Hamstrings and Glutes.

Any other name, like Neck or Cardio, stays its own row. It recovers with a 24 h half-life and has no
weekly target.

### 3.2 Fatigue per set

A working set is a non-warm-up set with reps or a duration. For each one:

```
effort  = clamp(1 + (2 - RIR) * 0.15, 0.5, 1.3)      // RIR from the set, or 10 - RPE,
                                                     // or 0 for a failure tag. Unknown: RIR 2.
effort *= 1.2 if the set is a negative or slow eccentric
fatigue added to a muscle = effort * share           // share: 1 main, 0.5 secondary
```

| RIR | 0 | 1 | 2 (or unknown) | 3 | 4 | 5+ |
|---|---|---|---|---|---|---|
| Effort | 1.3 | 1.15 | 1.0 | 0.85 | 0.7 | 0.55 to 0.5 |

A failure set is about 1.5 times an RIR 3 set. That matches failure sessions taking about 1.5 times as
long to recover (section 1.2). When the same muscle is both a main and a secondary part, the larger
share wins. Cardio counts as one set per 30 minutes for the exercise's parts.

### 3.3 Recovery

```
fatigue(now) = sum over sets of fatigue * 0.5 ^ (hours since the set / half-life of the muscle)
readiness    = 100 * exp(-fatigue / 5)
state        = Ready at 80 or more, Recovering at 50 to 79, Fatigued below 50
```

Sets older than 14 days are ignored. Ready means fatigue is at or below 1.116 sets. The half-lives are
set so a session of 6 hard sets at RIR 2 turns Ready at the time the research gives for "about 90%
recovered":

| Muscle | About 90% recovered | Half-life |
|---|---|---|
| Quads, Hamstrings | 72 h | 30 h |
| Glutes | 60 h | 25 h |
| Chest, Back, Biceps, Triceps | 48 h | 20 h |
| Calves | 42 h | 17 h |
| Shoulders | 36 h | 15 h |
| Abs, Forearms | 30 h | 12 h |

Half-life = recovery time / log2(6 / 1.116), about 2.43. Quads and hamstrings rest on Belcher 2019,
Sousa 2024 and Chen 2019. Chest, back and arms on Sousa 2024 and Chen 2019. The rest are reasoned
choices.

### 3.4 Weekly sets

Hard sets in the last 7 days (rolling), using the same shares (main 1, secondary 0.5). A set with RIR
5 or more is too easy and does not count. Effort does not scale the count, because failure does not
clearly add growth (Refalo 2023).

| Status | Hard sets in 7 days |
|---|---|
| None | 0 |
| Below | under 10 |
| In range | 10 to 20 |
| Above | over 20 |

10 is the ACSM and Schoenfeld 2017 floor, 20 the top of the Baz-Valle 2022 range. More than 20 is not
wrong, so it shows in violet, not red.

### 3.5 Before and after

| Session | Old model | New model |
|---|---|---|
| Bench 5 x 8 at 80 kg, RIR 1 (Chest, secondary Triceps) | Chest 7% right after, ready in 130 h. Triceps ready in 94 h | Chest 32% right after, ready in 47 h. Triceps 56%, ready in 27 h |
| Squat 5 x 5 at 140 kg (Legs, secondary Glutes) | One "Legs" row, ready in 132 h | Quads, Hamstrings, Glutes 37% each, ready in 65 h (glutes 54 h) |
| Calf raise 3 x 15 at 60 kg | Ready in 111 h | 55% right after, ready in 24 h |
| Weekly sets | Every non-warm-up set, whole numbers, no target | Hard sets with half sets for helpers, against 10 to 20 |

## 4. Limits

- No study gives recovery hours per muscle after normal gym training. The table in 3.3 combines
  eccentric studies, compound lift studies and a review.
- The RIR weights are fitted by hand to the qualitative findings.
- Training status, sleep, food, stress, age and sex are not modelled.
- Most subjects were young men. "Recovered" in these studies means strength or speed is back, not
  that the muscle is ready to grow again.

## 5. Sources

1. MacDougall JD, et al. The time course for elevated muscle protein synthesis following heavy resistance exercise. Can J Appl Physiol. 1995;20(4):480-6. doi:10.1139/h95-038. https://pubmed.ncbi.nlm.nih.gov/8563679/
2. Chesley A, et al. Changes in human muscle protein synthesis after resistance exercise. J Appl Physiol. 1992;73(4):1383-8. doi:10.1152/jappl.1992.73.4.1383. https://pubmed.ncbi.nlm.nih.gov/1280254/
3. Phillips SM, et al. Mixed muscle protein synthesis and breakdown after resistance exercise in humans. Am J Physiol. 1997;273(1):E99-107. doi:10.1152/ajpendo.1997.273.1.E99. https://pubmed.ncbi.nlm.nih.gov/9252485/
4. Tang JE, et al. Resistance training alters the response of fed state mixed muscle protein synthesis in young men. Am J Physiol Regul Integr Comp Physiol. 2008;294(1):R172-8. doi:10.1152/ajpregu.00636.2007. https://pubmed.ncbi.nlm.nih.gov/18032468/
5. Damas F, et al. Resistance training-induced changes in integrated myofibrillar protein synthesis are related to hypertrophy only after attenuation of muscle damage. J Physiol. 2016;594(18):5209-22. doi:10.1113/JP272472. https://pubmed.ncbi.nlm.nih.gov/27219125/
6. Morán-Navarro R, et al. Time course of recovery following resistance training leading or not to failure. Eur J Appl Physiol. 2017;117(12):2387-99. doi:10.1007/s00421-017-3725-7. https://pubmed.ncbi.nlm.nih.gov/28965198/
7. Pareja-Blanco F, et al. Time course of recovery following resistance exercise with different loading magnitudes and velocity loss in the set. Sports (Basel). 2019;7(3):59. doi:10.3390/sports7030059. https://pubmed.ncbi.nlm.nih.gov/30836680/
8. Pareja-Blanco F, et al. Time course of recovery from resistance exercise with different set configurations. J Strength Cond Res. 2020;34(10):2867-76. doi:10.1519/JSC.0000000000002756. https://pubmed.ncbi.nlm.nih.gov/30036284/
9. Belcher DJ, et al. Time course of recovery is similar for the back squat, bench press, and deadlift in well-trained males. Appl Physiol Nutr Metab. 2019;44(10):1033-42. doi:10.1139/apnm-2019-0004. https://pubmed.ncbi.nlm.nih.gov/30779596/
10. Sousa CA, Zourdos MC, Storey AG, Helms ER. The importance of recovery in resistance training microcycle construction. J Hum Kinet. 2024;91:205-23. doi:10.5114/jhk/186659. https://pubmed.ncbi.nlm.nih.gov/38689583/
11. Chen TC, et al. Damage and the repeated bout effect of arm, leg, and trunk muscles induced by eccentric resistance exercises. Scand J Med Sci Sports. 2019;29(5):725-35. doi:10.1111/sms.13388. https://pubmed.ncbi.nlm.nih.gov/30663816/
12. Hody S, et al. Eccentric muscle contractions: risks and benefits. Front Physiol. 2019;10:536. doi:10.3389/fphys.2019.00536. https://pubmed.ncbi.nlm.nih.gov/31130877/
13. Cheung K, Hume P, Maxwell L. Delayed onset muscle soreness: treatment strategies and performance factors. Sports Med. 2003;33(2):145-64. doi:10.2165/00007256-200333020-00005. https://pubmed.ncbi.nlm.nih.gov/12617692/
14. Hyldahl RD, Chen TC, Nosaka K. Mechanisms and mediators of the skeletal muscle repeated bout effect. Exerc Sport Sci Rev. 2017;45(1):24-33. doi:10.1249/JES.0000000000000095. https://pubmed.ncbi.nlm.nih.gov/27782911/
15. Schoenfeld BJ, Ogborn D, Krieger JW. Dose-response relationship between weekly resistance training volume and increases in muscle mass. J Sports Sci. 2017;35(11):1073-82. doi:10.1080/02640414.2016.1210197. https://pubmed.ncbi.nlm.nih.gov/27433992/
16. Schoenfeld BJ, et al. Resistance training volume enhances muscle hypertrophy but not strength in trained men. Med Sci Sports Exerc. 2019;51(1):94-103. doi:10.1249/MSS.0000000000001764. https://pubmed.ncbi.nlm.nih.gov/30153194/
17. Baz-Valle E, et al. A systematic review of the effects of different resistance training volumes on muscle hypertrophy. J Hum Kinet. 2022;81:199-210. doi:10.2478/hukin-2022-0017. https://pubmed.ncbi.nlm.nih.gov/35291645/
18. Pelland JC, et al. The resistance training dose response: meta-regressions exploring the effects of weekly volume and frequency on muscle hypertrophy and strength gains. Sports Med. 2026;56(2):481-505. doi:10.1007/s40279-025-02344-w. https://pubmed.ncbi.nlm.nih.gov/41343037/
19. Currier BS, et al. ACSM position stand. Resistance training prescription for muscle function, hypertrophy, and physical performance in healthy adults: an overview of reviews. Med Sci Sports Exerc. 2026;58(4):851-72. doi:10.1249/MSS.0000000000003897. https://pubmed.ncbi.nlm.nih.gov/41843416/
20. Schoenfeld BJ, Ogborn D, Krieger JW. Effects of resistance training frequency on measures of muscle hypertrophy. Sports Med. 2016;46(11):1689-97. doi:10.1007/s40279-016-0543-8. https://pubmed.ncbi.nlm.nih.gov/27102172/
21. Schoenfeld BJ, Grgic J, Krieger J. How many times per week should a muscle be trained to maximize muscle hypertrophy? J Sports Sci. 2019;37(11):1286-95. doi:10.1080/02640414.2018.1555906. https://pubmed.ncbi.nlm.nih.gov/30558493/
22. Refalo MC, et al. Influence of resistance training proximity-to-failure on skeletal muscle hypertrophy. Sports Med. 2023;53(3):649-65. doi:10.1007/s40279-022-01784-y. https://pubmed.ncbi.nlm.nih.gov/36334240/
23. Robinson ZP, et al. Exploring the dose-response relationship between estimated resistance training proximity to failure, strength gain, and muscle hypertrophy. Sports Med. 2024;54(9):2209-31. doi:10.1007/s40279-024-02069-2. https://pubmed.ncbi.nlm.nih.gov/38970765/
24. Halperin I, et al. Accuracy in predicting repetitions to task failure in resistance exercise. Sports Med. 2022. doi:10.1007/s40279-021-01559-x. https://pubmed.ncbi.nlm.nih.gov/34542869/
