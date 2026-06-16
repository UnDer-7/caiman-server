# caiman-server

Self-hosted personal billing management backend. See `AGENT.md` and `docs/` for full documentation.

## Security scanning (local dev)

Install [Syft](https://github.com/anchore/syft) and [Grype](https://github.com/anchore/grype):

Then run: `make security/scan/deps` or `make security/scan/base-images`
