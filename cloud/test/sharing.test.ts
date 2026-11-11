import { runInDurableObject } from "cloudflare:test";
import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import type { OwnerObject } from "../src/account/OwnerObject";
import { ClaudeClient } from "./ClaudeClient";
import { share } from "./Fixtures";
import { PhoneClient } from "./PhoneClient";

const owner = () => env.OWNER.get(env.OWNER.idFromName("owner"));

describe("asking the phone", () => {
  it("waits for the phone and answers as soon as it shares", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    const call = claude.call("get_overview");
    const requestId = await phone.nextRequest();
    expect((await phone.share(share(), requestId)).status).toBe(200);
    const answered = await call;
    expect(answered.isError).toBe(false);
    expect(answered.structured.training.completedWorkouts).toBe(5);
    // The request is answered, and the next call reads the copy without asking again.
    expect(((await (await phone.request("GET", "/v1/share-requests")).json()) as { requests: unknown[] }).requests).toEqual([]);
    expect((await claude.call("list_templates")).structured.templates).toHaveLength(1);
    expect(((await (await phone.request("GET", "/v1/share-requests")).json()) as { requests: unknown[] }).requests).toEqual([]);
  });

  it("says declined when the owner taps Deny", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    const call = claude.call("list_workouts");
    const requestId = await phone.nextRequest();
    expect((await phone.request("POST", `/v1/share-requests/${requestId}/decline`)).status).toBe(204);
    const answered = await call;
    expect(answered.isError).toBe(true);
    expect(answered.text).toMatch(/^declined: /);
    // A late answer to the same request is refused.
    expect((await phone.share(share(), requestId)).status).toBe(409);
  });

  it("says waiting_for_phone when the phone does not answer in time, then uses the late answer", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    const answered = await claude.call("get_overview");
    expect(answered.isError).toBe(true);
    // No Firebase key in tests, so the phone could not be woken: Claude is told to have the app opened.
    expect(answered.text).toMatch(/^waiting_for_phone: .*open Ember/);
    const requestId = await phone.nextRequest();
    expect((await phone.share(share(), requestId)).status).toBe(200);
    expect((await claude.call("get_overview")).structured.training.completedWorkouts).toBe(5);
  });

  it("asks once for calls that come together", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    const calls = Promise.all([claude.call("get_overview"), claude.call("list_exercises")]);
    const requestId = await phone.nextRequest();
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(((await (await phone.request("GET", "/v1/share-requests")).json()) as { requests: unknown[] }).requests).toHaveLength(1);
    await phone.share(share(), requestId);
    const [overview, exercises] = await calls;
    expect(overview.isError).toBe(false);
    expect(exercises.structured.exercises).toHaveLength(4);
  });

  it("says no_phone when no phone is set up", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    expect((await phone.request("DELETE", `/v1/devices/${phone.deviceId}`)).status).toBe(204);
    const answered = await claude.call("get_overview");
    expect(answered.text).toMatch(/^no_phone: /);
  });

  it("deletes the copy when it runs out", async () => {
    const phone = await PhoneClient.setup();
    await phone.share(share());
    await runInDurableObject(owner(), async (instance: OwnerObject, state) => {
      state.storage.sql.exec("UPDATE share_state SET keep_until = ?", new Date(Date.now() - 1000).toISOString());
      await instance.alarm();
      expect(state.storage.sql.exec("SELECT COUNT(*) AS n FROM shared_sets").one().n).toBe(0);
      expect(state.storage.sql.exec("SELECT COUNT(*) AS n FROM share_state").one().n).toBe(0);
    });
    expect(((await (await phone.request("GET", "/v1/share")).json()) as { share: unknown }).share).toBeNull();
  });
});
