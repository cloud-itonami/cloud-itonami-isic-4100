# cloud-itonami-isic-4100

Open Business Blueprint for **ISIC 4100**: construction of buildings —
the *housing* (住) vertical of the 衣食住 scaffold batch
(ADR-2607122200). Complements the existing civil-engineering and
finishing satellites (4211 / 4321 / 4322) by covering the missing
buildings class itself.

**Maturity: `:blueprint`** — this repository publishes the business
blueprint only. There is **no actor implementation yet**, and none is
claimed. ISIC section F sits in **rollout Wave 3
(production/robotics)** of the reverse-toposort plan (ADR-2607121000):
implementation is gated on the robotics premise (ADR-2607011000).
Publishing the blueprint now is deliberate ammunition loading for when
that gate opens (ADR-2607122100 Track A).

## What the implemented actor will be

**BuildOps-LLM ⊣ Building Construction Governor** — the
fleet-standard pattern: the advisor LLM drafts project intake,
permit-workflow assembly (jurisdiction-specific building codes),
schedule/BOM coordination via the existing procurement shape
(yoro-supply Pregel cells), and inspection planning; the independent
`:building-construction-governor` (a keyword unique fleet-wide) gates
every action; physical-domain work (excavation, framing, lifting,
finishing) is executed by robots under `kotoba-lang/robotics` safety
classes — worker/site safety actions are always `:safety-critical`
and require human sign-off, never dispatched directly by the LLM.

Operating states: `intake → design → permit → build → inspect → audit`.

## Why open

AGPL-3.0-or-later, forkable by any qualified operator, so local
builders never surrender project and inspection data to a closed
SaaS. Part of the [cloud-itonami](https://itonami.cloud) open business
fleet.
