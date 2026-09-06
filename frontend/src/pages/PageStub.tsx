/**
 * Generic placeholder for a not-yet-implemented operator console page
 * (master spec §5). Each real page replaces its route's <PageStub/> with
 * an actual component as its backing API lands (see routes.ts for the
 * phase each page is scheduled in) — no need for 13 near-identical empty
 * files before there's real content to put in them.
 */
export default function PageStub({ title, phase }: { title: string; phase: string }) {
  return (
    <section>
      <h1>{title}</h1>
      <p>Not implemented yet — planned for {phase}. See ../../docs/02-functional-requirements.md.</p>
    </section>
  )
}
