# Ponder Example Addon Smoke Pack

Install the generated addon jar together with Ponder 1.2.0, CraftTweaker and
MixinBooter. Copy the `scripts` directory into the Minecraft instance.

The pack registers a server-synchronizable ZenScript scene using the
`ponder_example:pulse` custom instruction codec. The addon also registers one
Java scene through ServiceLoader and one through Forge IMC.

Use `/ponder inspect ponder_example:codec_sync effective` to inspect codec,
tag and structure metadata. A client without the addon codec is rejected
before the server sends snapshot chunks; local Ponder scenes remain usable.
