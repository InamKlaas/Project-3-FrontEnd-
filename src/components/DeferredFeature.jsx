export default function DeferredFeature({ title, children }) {
  return <section className="panel"><h2>{title}</h2><p className="muted">This workflow is deferred beyond the stage-7 POC.{children ? ` ${children}` : ''}</p></section>
}
