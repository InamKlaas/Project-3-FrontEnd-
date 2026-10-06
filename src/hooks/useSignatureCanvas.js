import { useRef } from 'react'

export function useSignatureCanvas() {
  const canvases = useRef({})
  const activePointers = useRef(new Set())
  const drawnCanvases = useRef(new Set())

  const canvasProps = id => ({
    ref: canvas => { if (canvas) canvases.current[id] = canvas; else delete canvases.current[id] },
    onPointerDown: event => {
      const canvas = event.currentTarget
      const bounds = canvas.getBoundingClientRect()
      const context = canvas.getContext('2d')
      activePointers.current.add(id)
      canvas.setPointerCapture(event.pointerId)
      context.beginPath()
      context.moveTo((event.clientX - bounds.left) * canvas.width / bounds.width, (event.clientY - bounds.top) * canvas.height / bounds.height)
    },
    onPointerMove: event => {
      if (!activePointers.current.has(id)) return
      const canvas = event.currentTarget
      const bounds = canvas.getBoundingClientRect()
      const context = canvas.getContext('2d')
      drawnCanvases.current.add(id)
      context.lineWidth = 2
      context.lineCap = 'round'
      context.strokeStyle = '#163d38'
      context.lineTo((event.clientX - bounds.left) * canvas.width / bounds.width, (event.clientY - bounds.top) * canvas.height / bounds.height)
      context.stroke()
    },
    onPointerUp: () => activePointers.current.delete(id),
    onPointerCancel: () => activePointers.current.delete(id)
  })

  const toDataURL = id => canvases.current[id]?.toDataURL('image/png') || ''
  const hasInk = id => drawnCanvases.current.has(id)
  return { canvasProps, toDataURL, hasInk }
}