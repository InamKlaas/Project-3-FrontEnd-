import { stat, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const directory = new URL('../docs/backend/', import.meta.url)
if (!(await stat(directory)).isDirectory()) throw new Error('Missing docs/backend directory')
const response = await fetch(process.env.OPENAPI_URL || 'http://localhost:8080/v3/api-docs', {
  signal: AbortSignal.timeout(30000),
})
if (!response.ok) throw new Error(`OpenAPI export failed: HTTP ${response.status}`)
const spec = await response.json()
if (!spec.openapi || !spec.paths?.['/api/admin/users']) throw new Error('The running API does not have the current POC contract')
const target = new URL('openapi.json', directory)
await writeFile(target, `${JSON.stringify(spec, null, 2)}\n`, 'utf8')
console.log(`Exported ${Object.keys(spec.paths).length} paths to ${fileURLToPath(target)}`)
