import { describe, expect, it } from "vitest";
import { ConnectorCode } from "../src/auth/ConnectorCode";
import { systemRandomBytes } from "../src/crypto/RandomBytes";
import { share } from "./Fixtures";
import { PhoneClient, SETUP_SECRET } from "./PhoneClient";

describe("phone setup", () => {
  it("refuses a wrong setup secret and needs a token everywhere else", async () => {
    const stranger = new PhoneClient();
    const wrong = await stranger.request("POST", "/v1/devices", { setupSecret: "not-it", name: "x" });
    expect(wrong.status).toBe(403);
    expect((await stranger.request("GET", "/v1/share-requests")).status).toBe(401);
    expect((await stranger.request("POST", "/v1/connector-codes")).status).toBe(401);
    expect((await stranger.request("GET", "/v1/health")).status).toBe(200);
  });

  it("replaces the earlier phone when set up again, such as after a reinstall", async () => {
    const first = await PhoneClient.setup("Old phone");
    const second = await PhoneClient.setup("New phone");
    expect((await first.request("GET", "/v1/devices")).status).toBe(401);
    const { devices } = (await (await second.request("GET", "/v1/devices")).json()) as { devices: { id: string; revokedAt: string | null }[] };
    expect(devices.find((device) => device.id === first.deviceId)?.revokedAt).not.toBeNull();
    expect(devices.find((device) => device.id === second.deviceId)?.revokedAt).toBeNull();
    expect(second.token).toMatch(/^wt1\.[A-Za-z0-9_-]{43}$/);
  });

  it("keeps the push token, and refuses one that is not text", async () => {
    const phone = await PhoneClient.setup();
    expect((await phone.request("PUT", "/v1/push-token", { pushToken: 42 })).status).toBe(400);
    expect((await phone.request("PUT", "/v1/push-token", { pushToken: "fcm-token" })).status).toBe(204);
    const { devices } = (await (await phone.request("GET", "/v1/devices")).json()) as { devices: { id: string; hasPush: boolean }[] };
    expect(devices.find((device) => device.id === phone.deviceId)?.hasPush).toBe(true);
  });

  it("makes connector codes that read forgivingly", async () => {
    const phone = await PhoneClient.setup();
    const { code } = (await (await phone.request("POST", "/v1/connector-codes")).json()) as { code: string };
    expect(code).toMatch(/^[2-9A-HJ-NP-Z]{3}-[2-9A-HJ-NP-Z]{3}$/);
    expect(ConnectorCode.parse(code.toLowerCase().replace("-", " "))?.toString()).toBe(code);
    expect(ConnectorCode.parse("O0I-111")).toBeNull();
    expect(ConnectorCode.issue(systemRandomBytes).toString()).toMatch(/^[2-9A-HJ-NP-Z]{3}-[2-9A-HJ-NP-Z]{3}$/);
    expect(SETUP_SECRET.length).toBeGreaterThanOrEqual(32);
  });
});

describe("sharing from the phone", () => {
  it("stores a share, says until when it stays, and forgets it on request", async () => {
    const phone = await PhoneClient.setup();
    const shared = await phone.share(share());
    expect(shared.status).toBe(200);
    const body = (await shared.json()) as { sharedAt: string; keepUntil: string; counts: Record<string, number> };
    expect(body.counts).toEqual({ exercises: 5, templates: 1, workouts: 5, runs: 2 });
    expect(Date.parse(body.keepUntil) - Date.parse(body.sharedAt)).toBe(30 * 60 * 1000);
    expect(((await (await phone.request("GET", "/v1/share")).json()) as { share: unknown }).share).toMatchObject({ keepUntil: body.keepUntil });
    expect((await phone.request("DELETE", "/v1/share")).status).toBe(204);
    expect(((await (await phone.request("GET", "/v1/share")).json()) as { share: unknown }).share).toBeNull();
  });

  it("refuses a share that is not Ember's, listing every problem", async () => {
    const phone = await PhoneClient.setup();
    const broken = share() as any;
    broken.bundle.exercises[0].id = 7;
    broken.bundle.sessions[0].startedAtUtc = "yesterday";
    broken.bundle.sessions[0].entries[0].strengthSets[0].tags = [42];
    broken.runs[0].splits = "fast";
    const refused = await phone.share(broken);
    expect(refused.status).toBe(400);
    const { error } = (await refused.json()) as { error: { code: string; details: string[] } };
    expect(error.code).toBe("invalid");
    expect(error.details).toEqual([
      "bundle.exercises[0].id must be an id of 1 to 64 characters.",
      "bundle.sessions[0].startedAtUtc must be a date and time.",
      "bundle.sessions[0].entries[0].strengthSets[0].tags[0] must be one of Warmup, Drop, ToFailure, Failed, Negative, BackOff, Finisher, Explosive, SlowEccentric, SlowConcentric.",
      "runs[0].splits must be a list.",
    ]);
    const unknownFormat = await phone.share({ ...share(), shareFormat: 2 });
    expect(((await unknownFormat.json()) as { error: { details: string[] } }).error.details[0]).toMatch(/^shareFormat must be one of 1/);
  });

  it("answers a request id that does not exist with not_found", async () => {
    const phone = await PhoneClient.setup();
    const missing = await phone.share(share(), "f".repeat(32));
    expect(missing.status).toBe(404);
    expect((await phone.request("POST", `/v1/share-requests/${"f".repeat(32)}/decline`)).status).toBe(404);
  });
});
