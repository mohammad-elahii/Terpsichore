# Taste

## Communication & workflow
- User gives brief go-aheads ("good catch go ahead") and terse one-line feature directives ("preview button should only appear on deezer rows", "add debounce with at least 2 characters") without explanation, and expects the agent to infer the full implementation, surface follow-up issues itself, state the plan concisely first, then execute without waiting for detailed instructions. Keep pre-fix summaries short and concrete. Confidence: 0.7
- When the agent proposes fixes and the user approves, apply all of them together in one pass (edit + update tests + add regression tests + rerun affected suites in a single batch) rather than one at a time. Confidence: 0.5
- For UI actions that don't apply to a given item, the user prefers them completely hidden rather than shown-but-disabled (stated for the preview button: "hide it for others" — agent implemented hidden vs. greyed-out). Confidence: 0.6
- When the user asks for a behavioral threshold, treat the stated number as a requirement, not a suggestion (e.g., minimum 2 characters before search fires); the agent chose a 400ms debounce constant to satisfy it. Confidence: 0.6
