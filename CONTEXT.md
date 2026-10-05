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

**Ember dev**:
The debug build. It is a separate app from Ember and never touches its data.
_Avoid_: Test app, beta
