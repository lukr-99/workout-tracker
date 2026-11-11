import { beforeEach, describe, expect, it } from "vitest";
import { fold } from "../src/proposals/Proposals";
import { ClaudeClient } from "./ClaudeClient";
import { IDS, share } from "./Fixtures";
import { PhoneClient } from "./PhoneClient";

let phone: PhoneClient;
let claude: ClaudeClient;
const ref = () => crypto.randomUUID();

beforeEach(async () => {
  phone = await PhoneClient.setup();
  claude = await ClaudeClient.connect(phone);
  expect((await phone.share(share())).status).toBe(200);
});

async function pending(): Promise<{ id: string; kind: string; title: string; payload: any }[]> {
  return ((await (await phone.request("GET", "/v1/proposals?status=pending")).json()) as { proposals: never[] }).proposals;
}

describe("proposals", () => {
  it("proposes a template from catalog exercises and a new one, and the owner accepts both at once", async () => {
    const exercise = await claude.call("propose_exercise", {
      name: "Incline Dumbbell Press",
      category: "strength",
      primaryBodyPart: "Chest",
      secondaryBodyParts: ["Shoulders"],
      equipment: "Dumbbells",
      instructions: "Set the bench to 30 degrees\nPress up over the upper chest",
      defaultRestSeconds: 90,
      reason: "Upper chest gets no direct work.",
      clientRef: ref(),
    });
    expect(exercise.isError).toBe(false);
    const exerciseId = exercise.structured.proposal.payload.exerciseId;
    expect(exerciseId).toMatch(/^[0-9a-f]{32}$/);

    const name = `Push B ${ref().slice(0, 6)}`;
    const template = await claude.call("propose_template", {
      name,
      notes: "Heavy-light week pairing with Push A.",
      exercises: [
        { exerciseId: IDS.bench, targetSets: 4, repsMin: 6, repsMax: 8, restSeconds: 180, notes: "RIR 2" },
        { exerciseId, targetSets: 3, repsMin: 10, repsMax: 12, restSeconds: 90, supersetGroup: 1 },
      ],
      reason: "Bench went up 6% in two weeks; add volume for the chest.",
      clientRef: ref(),
    });
    expect(template.isError).toBe(false);
    expect(template.text).toContain(`Proposed the template "${name}"; it waits in Ember.`);
    const payload = template.structured.proposal.payload;
    expect(payload.exercises.map((item: { name: string }) => item.name)).toEqual(["Bench Press", "Incline Dumbbell Press"]);
    expect(payload.exercises[0]).toMatchObject({ targetSets: 4, repsMin: 6, repsMax: 8, restSeconds: 180, supersetGroup: null, notes: "RIR 2", newExercise: null });
    expect(payload.exercises[1]).toMatchObject({ targetSets: 3, repsMin: 10, repsMax: 12, restSeconds: 90, supersetGroup: 1 });
    expect(payload.exercises[1].newExercise).toMatchObject({ exerciseId, equipment: "Dumbbells", defaultRestSeconds: 90, instructions: "Set the bench to 30 degrees\nPress up over the upper chest" });

    const inbox = await pending();
    expect(inbox.map((proposal) => proposal.kind).sort()).toEqual(["exercise", "template"]);
    const templateId = template.structured.proposal.id;
    const accepted = await phone.request("POST", `/v1/proposals/${templateId}/decision`, { decision: "accepted" });
    expect(accepted.status).toBe(200);
    expect(await pending()).toEqual([]);
    const mine = await claude.call("list_proposals", {});
    expect(mine.structured.proposals.map((proposal: { status: string }) => proposal.status)).toEqual(["accepted", "accepted"]);
  });

  it("refuses a duplicate exercise, and repeats a proposal with the same clientRef", async () => {
    const duplicate = await claude.call("propose_exercise", { name: "  bench   PRESS ", category: "strength", primaryBodyPart: "Chest", reason: "x", clientRef: ref() });
    expect(duplicate.text).toMatch(/^duplicate: "Bench Press" is already in the catalog/);
    const archived = await claude.call("propose_exercise", { name: "Cable Curl", category: "strength", primaryBodyPart: "Arms", reason: "x", clientRef: ref() });
    expect(archived.text).toMatch(/^duplicate: The owner has "Cable Curl" archived/);

    const clientRef = ref();
    const args = { name: "Nordic Curl", category: "strength", primaryBodyPart: "Legs", reason: "Hamstrings.", clientRef };
    const first = await claude.call("propose_exercise", args);
    const again = await claude.call("propose_exercise", args);
    expect(again.structured).toMatchObject({ repeated: true, proposal: { id: first.structured.proposal.id } });
    const sameName = await claude.call("propose_exercise", { ...args, clientRef: ref() });
    expect(sameName.text).toMatch(/already proposed and waiting for the owner/);
  });

  it("refuses unknown and archived exercises, an unknown template, and a taken name", async () => {
    const refused = await claude.call("propose_template", {
      name: "Push A",
      exercises: [{ exerciseId: "f".repeat(32) }, { exerciseId: IDS.oldCurl }, { exerciseId: IDS.bench, repsMin: 12, repsMax: 8 }],
      reason: "x",
      clientRef: ref(),
    });
    expect(refused.isError).toBe(true);
    expect(refused.text).toMatch(/^invalid: 4 problem\(s\)/);
    expect(refused.text).toContain("repsMin 12 is more than repsMax 8");
    expect(refused.text).toContain("is no exercise in the catalog");
    expect(refused.text).toContain('\\"Cable Curl\\" is archived');
    expect(refused.text).toContain('already has a template \\"Push A\\"');

    const unknownTemplate = await claude.call("propose_template", {
      name: "Push A",
      exercises: [{ exerciseId: IDS.bench }],
      replacesTemplateId: "e".repeat(32),
      reason: "x",
      clientRef: ref(),
    });
    expect(unknownTemplate.text).toContain("is no template of the owner's");

    const newVersion = await claude.call("propose_template", {
      name: "Push A",
      exercises: [{ exerciseId: IDS.bench, notes: "5x5" }],
      replacesTemplateId: IDS.pushTemplate,
      reason: "Move to 5x5.",
      clientRef: ref(),
    });
    expect(newVersion.structured.proposal.payload).toMatchObject({ templateId: IDS.pushTemplate, replacesTemplateId: IDS.pushTemplate });
  });

  it("lets Claude withdraw a waiting proposal, but not one the owner decided", async () => {
    const one = await claude.call("propose_exercise", { name: `Sled Push ${ref().slice(0, 4)}`, category: "strength", primaryBodyPart: "Legs", reason: "x", clientRef: ref() });
    const two = await claude.call("propose_exercise", { name: `Farmer Walk ${ref().slice(0, 4)}`, category: "strength", primaryBodyPart: "Grip", reason: "x", clientRef: ref() });
    expect((await claude.call("withdraw_proposal", { id: one.structured.proposal.id })).text).toMatch(/^Withdrew/);
    expect((await phone.request("POST", `/v1/proposals/${one.structured.proposal.id}/decision`, { decision: "accepted" })).status).toBe(409);
    expect((await phone.request("POST", `/v1/proposals/${two.structured.proposal.id}/decision`, { decision: "rejected" })).status).toBe(200);
    expect((await phone.request("POST", `/v1/proposals/${two.structured.proposal.id}/decision`, { decision: "rejected" })).status).toBe(200);
    expect((await claude.call("withdraw_proposal", { id: two.structured.proposal.id })).text).toMatch(/^conflict: The owner already rejected it/);
  });

  it("compares names without case, accents, or extra spaces", () => {
    expect(fold("  Rumunský  MRTVÝ tah ")).toBe("rumunsky mrtvy tah");
  });
});
