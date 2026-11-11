/** The fields the Worker needs from a Firebase service-account key file (the FCM_SERVICE_ACCOUNT secret). */
export class FcmServiceAccount {
  private constructor(
    readonly projectId: string,
    readonly clientEmail: string,
    readonly privateKeyPem: string,
  ) {}

  /** Null when the secret is missing or is not a service-account key, so pushing is simply off. */
  static parse(json: string | undefined): FcmServiceAccount | null {
    if (!json) return null;
    try {
      const value = JSON.parse(json) as Record<string, unknown>;
      const { project_id: projectId, client_email: clientEmail, private_key: privateKey } = value;
      if (typeof projectId !== "string" || typeof clientEmail !== "string" || typeof privateKey !== "string") return null;
      if (!privateKey.includes("BEGIN PRIVATE KEY")) return null;
      return new FcmServiceAccount(projectId, clientEmail, privateKey);
    } catch {
      return null;
    }
  }
}
