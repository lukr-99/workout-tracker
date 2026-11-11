/**
 * What a Claude connector may do (docs/claude-connector.md): read the training data the phone shares, and
 * propose exercises and templates, which wait in Ember until the owner accepts them.
 */
export const CONNECTOR_SCOPES = ["read", "plan"] as const;

export type ConnectorScope = (typeof CONNECTOR_SCOPES)[number];

/** The valid scopes in [values], in the canonical order, without repeats. */
export function connectorScopes(values: readonly string[]): ConnectorScope[] {
  return CONNECTOR_SCOPES.filter((scope) => values.includes(scope));
}
