# Observatory Privacy Model

The Observatory uses application-level encryption for user-written content. Titles, descriptions, notes, subjects, and other semantic planning text are encrypted before they are stored, which protects them from casual database inspection or readable database dumps.

Operational metadata remains plaintext so the app can support scheduling, reminders, filtering, calendar views, free-time calculations, and overload warnings. This includes ownership IDs, parent foreign keys, statuses, priorities, categories, dates, times, recurrence metadata, estimates, completion timestamps, archived/active flags, sort fields, and encryption key versions.

This is not full zero-knowledge encryption. A server or application operator with access to the runtime decryption key could still decrypt Observatory data. Full zero-knowledge encryption would require a different design where keys are derived from the user's password and are never stored server-side.

Set `OBSERVATORY_ENCRYPTION_KEY` in the runtime environment. The key is not stored in the database, hard-coded in application code, or committed to the repository.

The Observatory intentionally does not include iCalendar export or Google Calendar synchronization.

Pulsars are stored as encrypted Observatory Orbits with `kind = PULSAR`. They are not a
separate application entity. On startup, legacy rows from an existing `pulsars` table are
copied into `orbits` without decrypting their semantic fields. Keep the old table until the
migrated rows have been verified, then it can be removed manually. The legacy optional
Pulsar-to-exam relation is not copied because the unified Orbit model links scheduling goals
to Star Systems instead.
