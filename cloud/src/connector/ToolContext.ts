import type { DeviceView, OwnerObject } from "../account/OwnerObject";
import type { ConnectorGrant } from "./ConnectorGrant";

/** Everything a tool needs for one MCP request: who calls, as which device, and the owner's object. */
export interface ToolContext {
  grant: ConnectorGrant;
  /** The connector device; proposals made with it are Claude's. */
  device: DeviceView;
  owner: DurableObjectStub<OwnerObject>;
}
