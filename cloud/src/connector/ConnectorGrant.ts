/** What the OAuth grant carries to every MCP request: the connector device it was issued for. */
export interface ConnectorGrant {
  deviceId: string;
}
