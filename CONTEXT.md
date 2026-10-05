# Ember

Ember logs strength workouts and GPS runs for one person, on their phone. These are the words the
app uses. Use them in code, copy and docs.

## Language

**Exercise**:
A movement in the catalog, like Barbell Bench Press. It is archived, never deleted.
_Avoid_: Movement, lift (as a noun for a catalog item)

**Workout**:
One strength session from start to finish. In code it is `WorkoutSession`.
_Avoid_: Session (in copy), log, training

**Exercise in a workout**:
One exercise as it was done in one workout, with its sets. It keeps a snapshot of the exercise's
name and body part. In code it is `WorkoutEntry`.
_Avoid_: Entry (in copy), block

**Set**:
One round of reps at a weight, inside an exercise in a workout.
_Avoid_: Round

**Set tag**:
A label on a set: Warm-up, Drop, To failure, Failed early, Negative, Back-off. A set can have several.
_Avoid_: Set type (the old single-choice field)

**Superset**:
Two or more consecutive exercises in a workout done as one round.
_Avoid_: Circuit, giant set

**Template**:
A saved list of exercises to start a workout from, with an optional note per exercise.
_Avoid_: Routine, program, plan

**Run**:
One GPS-recorded run with its trace, pace and splits.
_Avoid_: Activity, track

**Route**:
A planned path saved for later runs. A run can follow a route, but the route never forces anything.
_Avoid_: Course, track

**Split**:
The time for one kilometre (or mile) of a run.
_Avoid_: Lap

**Personal record (PR)**:
A set or run that beats every earlier one on some measure, like heaviest set or estimated 1RM.
_Avoid_: Best (as a noun), achievement

**Estimated 1RM (e1RM)**:
The one-rep max a set predicts, from the Epley formula.
_Avoid_: Max, 1RM (when it was not actually lifted)

**Exercise note**:
The owner's lasting note on an exercise, like seat height. It is pinned on the card every time the
exercise is logged.
_Avoid_: Description, instructions

**Steps**:
How to do an exercise, one step per line, shown behind the info button. In code it is
`instructions`.
_Avoid_: Description, cues (in copy)

**Last time**:
The note written on the same exercise in the most recent finished workout that has one.
_Avoid_: Previous note, history

**Plan**:
What a template sets up for one exercise: a number of sets, a rep range, rest and a superset group.
It is a starting point; the workout can always differ. In code it is the template exercise's
targets.
_Avoid_: Program, prescription, target (in copy)

**Body part**:
The main muscle group an exercise trains, like Chest or Legs, shown with its colour dot. An
exercise can also work other body parts.
_Avoid_: Muscle group (in copy), category (that is Strength or Cardio)

**Rest**:
The pause after a set, timed by the rest timer. Each exercise can have its own; otherwise the
default from Settings is used.
_Avoid_: Break, recovery (that is the body map)

**Effort**:
How hard a set felt, as reps in reserve (RIR) or a rate of perceived exertion (RPE).
_Avoid_: Intensity, difficulty

**Library**:
The tab with the exercise catalog and the templates.
_Avoid_: Database, catalogue (in copy)

**What's new**:
The card on Home after an update, with a few lines about the release. It is shown once per
release and put away with Got it.
_Avoid_: Changelog, release notes (in copy), tour

**Ember dev**:
The debug build. It is a separate app from Ember and never touches its data.
_Avoid_: Test app, beta

## Writing copy

- **Empty states** say what is missing, then how to fill it, with an action when one fits:
  "No templates yet" then "A template is a saved list of exercises..." and New template.
- **Gesture hints** are one muted line with an icon, next to the thing they explain, like "Tap a
  set's number for tags, effort, a note, or to remove it."
- **Settings** rows each have a one-line hint, and each section has a description.
- Use the words above. Plain English, short sentences, no em-dashes.
