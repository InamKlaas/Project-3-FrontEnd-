import { useEffect, useState } from 'react'

/* A cancelled or superseded request must not repaint a different route/user. */
export function useAsyncResource(load, dependencies = []) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    let live = true
    setLoading(true); setError('')
    Promise.resolve().then(() => load(controller.signal))
      .then(value => { if (live) setData(value) })
      .catch(failure => { if (live) setError(failure.message) })
      .finally(() => { if (live) setLoading(false) })
    return () => { live = false; controller.abort() }
    // The caller declares every value used by its loader in dependencies.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencies, attempt])
  return { data, loading, error, reload: () => setAttempt(value => value + 1) }
}
