# Firebase service account key goes here

This app reads `firebase.credentials.path=firebase/serviceAccountKey.json`
(see `src/main/resources/application.properties`) **relative to the
working directory the app is run from** — not from the classpath, and
not from inside the packaged jar. That's why this folder is empty.

## How to get the file
1. Firebase Console → your project → ⚙️ Project settings → Service accounts
2. "Generate new private key" → downloads a `.json` file
3. Rename it to `serviceAccountKey.json` and place it directly in this
   `firebase/` folder (same level as `src`, `pom.xml`)

## If you run from IntelliJ
Make sure the run configuration's "Working directory" is the project
root (`backend/`) — it is by default — so `firebase/serviceAccountKey.json`
resolves correctly.

**Never commit the real key file to git.** It's already excluded via
`.gitignore`.

## Firestore Security Rules (Community Rooms, Phase 6 — chat)

`firestore.rules` in this folder is the actual security boundary for
chat — see its header comment for how it authorizes off the
`communities/{id}/members/{uid}` mirror docs `CommunityService` writes.
Deploy it with the Firebase CLI from the `backend/` root (where
`firebase.json` points at it):

```
firebase deploy --only firestore:rules
```

`storage.rules` (community icon and blog-cover image uploads) deploys
the same way:

```
firebase deploy --only storage:rules
```

There's no CI step wired up for this yet — deploying rules is a manual
step alongside a backend deploy whenever `firestore.rules` changes.
That's a real gap (rules could drift from what's deployed) worth
closing with a CI step once this app has a real deploy pipeline.

## Cloud Functions — NOT required, kept only as a reference

`../functions/` holds an *alternative*, unused-by-default
implementation of the chat-send logic as a Firebase Cloud Function.
Chat sends do **not** go through it — they go through `ChatService` in
the Spring backend instead (`community/service/ChatService.java`),
specifically so this app never needs Firebase's paid Blaze plan.
Cloud Functions require Blaze to deploy at all, even at zero usage;
the Spring-hosted path does the exact same job (membership check +
atomic rate-limit transaction) for free, using the Admin SDK
credentials this backend already has.

`firebase.json` does not list a `functions` config, so a plain
`firebase deploy` will never try to touch this folder. It's left in
place only in case you're already on Blaze for other reasons and
would rather use a Cloud Function than add an endpoint to Spring — see
`ChatService`'s javadoc for the full reasoning either way. If you never
plan to use it, it's safe to delete the whole `functions/` folder.

